package com.video.service.impl;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.video.annotation.MyAutowired;
import com.video.annotation.MyComponent;
import com.video.config.SentinelRuleManager;
import com.video.exception.BusinessException;
import com.video.mapper.CouponMapper;
import com.video.mapper.CouponOrderMapper;
import com.video.pojo.dto.AdminCouponCreateRequest;
import com.video.pojo.dto.AdminCouponCreateResponse;
import com.video.pojo.dto.CouponSeckillEvent;
import com.video.pojo.dto.CouponSeckillResultMessage;
import com.video.pojo.dto.CouponOrderStatusDTO;
import com.video.pojo.dto.CouponSeckillRequest;
import com.video.pojo.dto.PageResult;
import com.video.pojo.entity.Coupon;
import com.video.pojo.entity.CouponOrder;
import com.video.messageQueue.rocketmq.CouponSeckillRedisCompensator;
import com.video.messageQueue.rocketmq.CouponSeckillTxProducer;
import com.video.service.CouponService;
import com.video.utils.LuaScriptUtil;
import com.video.utils.CouponCodeUtil;
import com.video.utils.IdGenerator;
import com.video.utils.JSONUtil;
import com.video.utils.RedisUtil;
import com.video.utils.UserHolder;
import com.video.websocket.NotificationWebSocketServer;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

@Slf4j
@MyComponent
public class CouponServiceImpl implements CouponService {
    private static final String COUPON_SECKILL_SCRIPT = LuaScriptUtil.load("lua/coupon_seckill.lua");
    private static final String COUPON_STOCK_PREFIX = "coupon:seckill:stock:";
    private static final String COUPON_USERS_PREFIX = "coupon:seckill:users:";

    @MyAutowired
    private CouponMapper couponMapper;

    @MyAutowired
    private CouponOrderMapper couponOrderMapper;

    /**
     * 管理员手动创建优惠券
     * @param request
     * @return
     */
    @Override
    public AdminCouponCreateResponse createByAdmin(AdminCouponCreateRequest request) {
        validateCreateRequest(request);

        Coupon coupon = new Coupon();
        coupon.setTitle(request.getTitle().trim());
        coupon.setStock(request.getStock());
        coupon.setStartTime(request.getStartTime());
        coupon.setEndTime(request.getEndTime());
        coupon.setStatus(1);

        Long couponId = couponMapper.create(coupon);
        initCouponRedis(couponId, request.getStock());
        return new AdminCouponCreateResponse(couponId, "创建成功");
    }

    /**
     * 查询优惠券
     * @param status
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public PageResult<Coupon> list(Integer status, Integer page, Integer pageSize) {
        return queryCouponPage(1, page, pageSize);
    }

    @Override
    public PageResult<Coupon> listByAdmin(Integer status, Integer page, Integer pageSize) {
        return queryCouponPage(status, page, pageSize);
    }

    private PageResult<Coupon> queryCouponPage(Integer status, Integer page, Integer pageSize) {
        int currentPage = page == null || page < 1 ? 1 : page;
        int currentPageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int offset = (currentPage - 1) * currentPageSize;
        List<Coupon> coupons = couponMapper.list(status, offset, currentPageSize);
        Long total = couponMapper.count(status);
        return new PageResult<>(total == null ? 0L : total, coupons);
    }

    @Override
    public void disableByAdmin(Long couponId) {
        if (couponId == null) {
            throw new BusinessException(400, "优惠券ID不能为空");
        }
        Coupon coupon = couponMapper.getById(couponId);
        if (coupon == null) {
            throw new BusinessException(404, "优惠券不存在");
        }
        int rows = couponMapper.disable(couponId);
        if (rows <= 0) {
            throw new BusinessException("优惠券停用失败");
        }
        RedisUtil.del(stockKey(couponId));
        RedisUtil.del(usersKey(couponId));
    }

    @Override
    public Coupon detail(Long couponId) {
        if (couponId == null) {
            throw new BusinessException(400, "优惠券ID不能为空");
        }
        Coupon coupon = couponMapper.getById(couponId);
        if (coupon == null) {
            throw new BusinessException(404, "优惠券不存在");
        }
        return coupon;
    }

    @Override
    public Long seckillPreDeduct(CouponSeckillRequest request) {
        Long couponId = request == null ? null : request.getCouponId();
        try (Entry ignored = SphU.entry(SentinelRuleManager.COUPON_SECKILL_PRE_DEDUCT, EntryType.IN, 1, couponId)) {
            applySentinelDegradeTestSwitch(couponId);
            return doSeckillPreDeduct(request);
        } catch (ParamFlowException e) {
            log.warn("触发 Sentinel 热点参数限流, couponId={}", couponId);
            throw new BusinessException("当前优惠券过于火爆，请稍后再试");
        } catch (FlowException e) {
            log.warn("触发 Sentinel 普通限流, resource={}", SentinelRuleManager.COUPON_SECKILL_PRE_DEDUCT);
            throw new BusinessException("系统繁忙，请稍后再试");
        } catch (DegradeException e) {
            log.warn("触发 Sentinel 熔断降级, resource={}", SentinelRuleManager.COUPON_SECKILL_PRE_DEDUCT);
            throw new BusinessException("服务暂时不可用，请稍后再试");
        } catch (BlockException e) {
            log.warn("触发 Sentinel 规则拦截, resource={}", SentinelRuleManager.COUPON_SECKILL_PRE_DEDUCT);
            throw new BusinessException("系统繁忙，请稍后再试");
        }
    }

    private void applySentinelDegradeTestSwitch(Long couponId) {
        if (couponId == null) {
            return;
        }
        if (couponId == 901L) {
            log.warn("couponId=901 模拟慢调用");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                RuntimeException exception = new RuntimeException("模拟慢调用被中断", e);
                Tracer.trace(exception);
                throw exception;
            }
        }
        if (couponId == 902L) {
            log.warn("couponId=902 模拟异常");
            RuntimeException exception = new RuntimeException("模拟秒杀异常");
            Tracer.trace(exception);
            throw exception;
        }
    }

    private Long doSeckillPreDeduct(CouponSeckillRequest request) {
        if (request == null || request.getCouponId() == null) {
            throw new BusinessException(400, "优惠券ID不能为空");
        }
        Coupon coupon = detail(request.getCouponId());
        Long userId = UserHolder.getUser().getId();
        long startTime = toEpochMilli(coupon.getStartTime());
        long endTime = toEpochMilli(coupon.getEndTime());
        Long result = RedisUtil.evalLong(
                COUPON_SECKILL_SCRIPT,
                Arrays.asList(stockKey(coupon.getId()), usersKey(coupon.getId())),
                Arrays.asList(userId.toString(), String.valueOf(System.currentTimeMillis()),
                        String.valueOf(startTime), String.valueOf(endTime))
        );
        if (result != null && result == 0L) {
            CouponSeckillEvent event = new CouponSeckillEvent();
            event.setOrderId(IdGenerator.nextId());
            event.setCouponId(coupon.getId());
            event.setUserId(userId);
            event.setCouponCode(CouponCodeUtil.generate(coupon.getId(), userId));
            event.setCreatedAt(System.currentTimeMillis());
            notifySeckillResult(userId, coupon.getId(), "PROCESSING", null, "抢券请求已受理");
            boolean sent = CouponSeckillTxProducer.send(event);
            if (!sent) {
                CouponSeckillRedisCompensator.compensate(event, "事务消息发送失败");
                notifySeckillResult(userId, coupon.getId(), "FAILED", null, "抢券失败，请稍后重试");
            }
        }
        return result;
    }

    @Override
    public CouponOrderStatusDTO orderStatus(Long couponId) {
        if (couponId == null) {
            throw new BusinessException(400, "优惠券ID不能为空");
        }
        Long userId = UserHolder.getUser().getId();
        CouponOrder order = couponOrderMapper.getByCouponIdAndUserId(couponId, userId);
        if (order == null) {
            return null;
        }
        CouponOrderStatusDTO dto = new CouponOrderStatusDTO();
        dto.setCouponId(order.getCouponId());
        dto.setUserId(order.getUserId());
        dto.setStatus(order.getStatus());
        dto.setCouponCode(order.getCouponCode());
        return dto;
    }

    private long toEpochMilli(java.time.LocalDateTime time) {
        if (time == null) {
            return 0L;
        }
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private String stockKey(Long couponId) {
        return COUPON_STOCK_PREFIX + couponId;
    }

    private String usersKey(Long couponId) {
        return COUPON_USERS_PREFIX + couponId;
    }

    private void validateCreateRequest(AdminCouponCreateRequest request) {
        if (request == null) {
            throw new BusinessException(400, "请求不能为空");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BusinessException(400, "优惠券标题不能为空");
        }
        if (request.getStock() == null || request.getStock() <= 0) {
            throw new BusinessException(400, "库存必须大于0");
        }
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();
        if (startTime == null || endTime == null) {
            throw new BusinessException(400, "开始时间和结束时间不能为空");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(400, "开始时间必须早于结束时间");
        }
    }

    private void initCouponRedis(Long couponId, Integer stock) {
        try {
            RedisUtil.setPersistent(stockKey(couponId), stock.toString());
            RedisUtil.del(usersKey(couponId));
        } catch (Exception e) {
            log.error("初始化优惠券 Redis 秒杀数据失败, couponId={}, stock={}", couponId, stock, e);
        }
    }

    private void notifySeckillResult(Long userId, Long couponId, String status, String couponCode, String message) {
        CouponSeckillResultMessage resultMessage = new CouponSeckillResultMessage();
        resultMessage.setType("COUPON_SECKILL_RESULT");
        resultMessage.setCouponId(couponId);
        resultMessage.setStatus(status);
        resultMessage.setCouponCode(couponCode);
        resultMessage.setMessage(message);
        NotificationWebSocketServer.sendToUser(userId, JSONUtil.toJson(resultMessage));
    }
}
