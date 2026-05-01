package com.video.mapper.impl;

import com.video.annotation.MyComponent;
import com.video.mapper.CouponOrderMapper;
import com.video.pojo.entity.CouponOrder;
import com.video.utils.JdbcUtils;
import com.video.utils.XmlSqlReaderUtil;

import java.util.List;

@MyComponent
public class CouponOrderMapperImpl implements CouponOrderMapper {
    @Override
    public CouponOrder getByCouponIdAndUserId(Long couponId, Long userId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CouponOrderMapper.getByCouponIdAndUserId");
        List<CouponOrder> orders = JdbcUtils.executeQuery(CouponOrder.class, sql, couponId, userId);
        return orders == null || orders.isEmpty() ? null : orders.get(0);
    }

    @Override
    public int insert(CouponOrder order) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CouponOrderMapper.insert");
        return JdbcUtils.executeUpdate(sql,
                order.getCouponId(),
                order.getUserId(),
                order.getCouponCode(),
                order.getStatus());
    }
}
