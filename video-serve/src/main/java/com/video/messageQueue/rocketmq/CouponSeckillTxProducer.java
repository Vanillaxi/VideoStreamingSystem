package com.video.messageQueue.rocketmq;

import com.video.config.DBPool;
import com.video.pojo.dto.CouponSeckillEvent;
import com.video.utils.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class CouponSeckillTxProducer {
    private static volatile TransactionMQProducer producer;
    private static volatile ExecutorService transactionExecutor;

    private CouponSeckillTxProducer() {
    }

    public static boolean send(CouponSeckillEvent event) {
        String topic = RocketMQConfig.couponSeckillTxTopic();
        String payload = JSONUtil.toJson(event);
        try {
            Message message = new Message(topic, payload.getBytes(StandardCharsets.UTF_8));
            message.setKeys(event.getOrderId().toString());
            SendResult result = getProducer().sendMessageInTransaction(message, event);
            log.info("RocketMQ coupon_seckill_tx 事务半消息发送完成，topic={}, orderId={}, couponId={}, userId={}, msgId={}, status={}",
                    topic, event.getOrderId(), event.getCouponId(), event.getUserId(),
                    result.getMsgId(), result.getSendStatus());
            return true;
        } catch (Exception e) {
            log.error("RocketMQ coupon_seckill_tx 事务消息发送失败，orderId={}, couponId={}, userId={}, payload={}",
                    event.getOrderId(), event.getCouponId(), event.getUserId(), payload, e);
            return false;
        }
    }

    public static void close() {
        TransactionMQProducer current = producer;
        if (current != null) {
            current.shutdown();
            producer = null;
            log.info("RocketMQ 事务 Producer 已关闭");
        }
        ExecutorService executor = transactionExecutor;
        if (executor != null) {
            executor.shutdownNow();
            transactionExecutor = null;
        }
    }

    private static TransactionMQProducer getProducer() throws Exception {
        if (producer == null) {
            synchronized (CouponSeckillTxProducer.class) {
                if (producer == null) {
                    String producerGroup = RocketMQConfig.couponSeckillTxProducerGroup();
                    String namesrvAddr = RocketMQConfig.nameSrvAddr();
                    String topic = RocketMQConfig.couponSeckillTxTopic();
                    TransactionMQProducer mqProducer = new TransactionMQProducer(producerGroup);
                    mqProducer.setNamesrvAddr(namesrvAddr);
                    transactionExecutor = Executors.newFixedThreadPool(4, r -> {
                        Thread thread = new Thread(r, "coupon-seckill-tx-executor");
                        thread.setDaemon(true);
                        return thread;
                    });
                    mqProducer.setExecutorService(transactionExecutor);
                    mqProducer.setTransactionListener(new CouponSeckillTransactionListener());
                    log.info("RocketMQ TransactionProducer 初始化配置，namesrvAddr={}, producerGroup={}, topic={}, sendMsgTimeout={}",
                            namesrvAddr, producerGroup, topic, mqProducer.getSendMsgTimeout());
                    mqProducer.start();
                    producer = mqProducer;
                    log.info("RocketMQ TransactionProducer start 成功，namesrvAddr={}, producerGroup={}, topic={}, sendMsgTimeout={}",
                            namesrvAddr, producerGroup, topic, mqProducer.getSendMsgTimeout());
                }
            }
        }
        return producer;
    }

    private static class CouponSeckillTransactionListener implements TransactionListener {
        @Override
        public LocalTransactionState executeLocalTransaction(Message message, Object arg) {
            CouponSeckillEvent event = arg instanceof CouponSeckillEvent
                    ? (CouponSeckillEvent) arg
                    : JSONUtil.toBean(new String(message.getBody(), StandardCharsets.UTF_8), CouponSeckillEvent.class);
            if (event == null) {
                log.error("RocketMQ 本地事务执行失败，消息体无法解析，msgId={}", message.getTransactionId());
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }
            try {
                insertOrderWithTransaction(event);
                log.info("RocketMQ 本地事务提交成功，orderId={}, couponId={}, userId={}",
                        event.getOrderId(), event.getCouponId(), event.getUserId());
                return LocalTransactionState.COMMIT_MESSAGE;
            } catch (SQLException e) {
                if (isDuplicate(e) && existsCouponUserOrderQuietly(event)) {
                    log.warn("RocketMQ 本地事务唯一索引冲突，按幂等成功处理，orderId={}, couponId={}, userId={}",
                            event.getOrderId(), event.getCouponId(), event.getUserId(), e);
                    return LocalTransactionState.COMMIT_MESSAGE;
                }
                log.error("RocketMQ 本地事务失败，准备回滚半消息并补偿 Redis，orderId={}, couponId={}, userId={}",
                        event.getOrderId(), event.getCouponId(), event.getUserId(), e);
                CouponSeckillRedisCompensator.compensate(event, "本地事务失败");
                notifyFailed(event, "抢券失败，请稍后重试");
                return LocalTransactionState.ROLLBACK_MESSAGE;
            } catch (Exception e) {
                log.error("RocketMQ 本地事务异常，暂返回 UNKNOWN 等待回查，orderId={}, couponId={}, userId={}",
                        event.getOrderId(), event.getCouponId(), event.getUserId(), e);
                return LocalTransactionState.UNKNOW;
            }
        }

        @Override
        public LocalTransactionState checkLocalTransaction(MessageExt message) {
            CouponSeckillEvent event = JSONUtil.toBean(new String(message.getBody(), StandardCharsets.UTF_8), CouponSeckillEvent.class);
            if (event == null) {
                log.error("RocketMQ 事务回查消息体无法解析，msgId={}", message.getMsgId());
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }
            try {
                if (existsOrderId(event.getOrderId()) || existsCouponUserOrder(event)) {
                    log.info("RocketMQ 事务回查确认订单存在，提交消息，msgId={}, orderId={}, couponId={}, userId={}",
                            message.getMsgId(), event.getOrderId(), event.getCouponId(), event.getUserId());
                    return LocalTransactionState.COMMIT_MESSAGE;
                }
                // 如果订单不存在，说明本地事务没有成功提交。这里返回 ROLLBACK，避免无订单消息被消费后误推 SUCCESS。
                log.warn("RocketMQ 事务回查确认订单不存在，回滚消息，msgId={}, orderId={}, couponId={}, userId={}",
                        message.getMsgId(), event.getOrderId(), event.getCouponId(), event.getUserId());
                CouponSeckillRedisCompensator.compensate(event, "事务回查订单不存在");
                notifyFailed(event, "抢券失败，请稍后重试");
                return LocalTransactionState.ROLLBACK_MESSAGE;
            } catch (Exception e) {
                // 查询数据库失败时状态无法确定，返回 UNKNOWN 让 RocketMQ 后续继续回查。
                log.error("RocketMQ 事务回查异常，返回 UNKNOWN 等待后续回查，msgId={}, orderId={}, couponId={}, userId={}",
                        message.getMsgId(), event.getOrderId(), event.getCouponId(), event.getUserId(), e);
                return LocalTransactionState.UNKNOW;
            }
        }
    }

    private static void insertOrderWithTransaction(CouponSeckillEvent event) throws SQLException {
        String decreaseStockSql = "UPDATE coupon SET stock = stock - 1, update_time = NOW() WHERE id = ? AND stock > 0";
        String insertOrderSql = "INSERT INTO coupon_order (id, coupon_id, user_id, coupon_code, status, create_time) "
                + "VALUES (?, ?, ?, ?, 1, FROM_UNIXTIME(? / 1000))";
        Connection conn = DBPool.getConnection();
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(decreaseStockSql)) {
                pstmt.setLong(1, event.getCouponId());
                int stockRows = pstmt.executeUpdate();
                if (stockRows <= 0) {
                    throw new SQLException("优惠券库存不足或优惠券不存在，couponId=" + event.getCouponId());
                }
            }
            try (PreparedStatement pstmt = conn.prepareStatement(insertOrderSql)) {
                pstmt.setLong(1, event.getOrderId());
                pstmt.setLong(2, event.getCouponId());
                pstmt.setLong(3, event.getUserId());
                pstmt.setString(4, event.getCouponCode());
                pstmt.setLong(5, event.getCreatedAt() == null ? System.currentTimeMillis() : event.getCreatedAt());
                pstmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            rollback(conn);
            throw e;
        } finally {
            resetAutoCommit(conn);
            DBPool.releaseConnection(conn);
        }
    }

    private static boolean existsOrderId(Long orderId) throws SQLException {
        String sql = "SELECT 1 FROM coupon_order WHERE id = ? LIMIT 1";
        Connection conn = DBPool.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } finally {
            DBPool.releaseConnection(conn);
        }
    }

    private static boolean existsCouponUserOrder(CouponSeckillEvent event) throws SQLException {
        String sql = "SELECT 1 FROM coupon_order WHERE coupon_id = ? AND user_id = ? LIMIT 1";
        Connection conn = DBPool.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, event.getCouponId());
            pstmt.setLong(2, event.getUserId());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } finally {
            DBPool.releaseConnection(conn);
        }
    }

    private static boolean existsCouponUserOrderQuietly(CouponSeckillEvent event) {
        try {
            return existsCouponUserOrder(event);
        } catch (SQLException e) {
            log.error("查询优惠券订单幂等状态失败，orderId={}, couponId={}, userId={}",
                    event.getOrderId(), event.getCouponId(), event.getUserId(), e);
            return false;
        }
    }

    private static boolean isDuplicate(SQLException e) {
        return "23000".equals(e.getSQLState()) || e.getErrorCode() == 1062;
    }

    private static void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            log.error("RocketMQ 本地事务回滚失败", e);
        }
    }

    private static void resetAutoCommit(Connection conn) {
        try {
            conn.setAutoCommit(true);
        } catch (Exception e) {
            log.error("重置数据库连接自动提交失败", e);
        }
    }

    private static void notifyFailed(CouponSeckillEvent event, String message) {
        com.video.pojo.dto.CouponSeckillResultMessage resultMessage = new com.video.pojo.dto.CouponSeckillResultMessage();
        resultMessage.setType("COUPON_SECKILL_RESULT");
        resultMessage.setCouponId(event.getCouponId());
        resultMessage.setStatus("FAILED");
        resultMessage.setMessage(message);
        com.video.websocket.NotificationWebSocketServer.sendToUser(event.getUserId(), JSONUtil.toJson(resultMessage));
    }
}
