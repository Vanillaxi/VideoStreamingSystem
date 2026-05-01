package com.video.messageQueue.rocketmq;

import com.video.pojo.dto.CouponSeckillEvent;
import com.video.pojo.dto.CouponSeckillResultMessage;
import com.video.utils.JSONUtil;
import com.video.utils.RedisUtil;
import com.video.websocket.NotificationWebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class CouponSeckillTxConsumer {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final String SUCCESS_NOTIFY_KEY_PREFIX = "coupon:seckill:success-notify:";
    private static volatile DefaultMQPushConsumer consumer;

    private CouponSeckillTxConsumer() {
    }

    public static void start() {
        if (!STARTED.compareAndSet(false, true)) {
            log.info("优惠券事务秒杀 RocketMQ Consumer 已启动，跳过重复启动");
            return;
        }
        try {
            String namesrvAddr = RocketMQConfig.nameSrvAddr();
            String consumerGroup = RocketMQConfig.couponSeckillTxConsumerGroup();
            String topic = RocketMQConfig.couponSeckillTxTopic();
            DefaultMQPushConsumer mqConsumer = new DefaultMQPushConsumer(consumerGroup);
            mqConsumer.setNamesrvAddr(namesrvAddr);
            mqConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
            mqConsumer.setMaxReconsumeTimes(RocketMQConfig.couponSeckillTxMaxReconsumeTimes());
            mqConsumer.subscribe(topic, "*");
            mqConsumer.registerMessageListener((MessageListenerConcurrently) (messages, context) -> {
                for (MessageExt message : messages) {
                    ConsumeConcurrentlyStatus status = processMessage(message);
                    if (status != ConsumeConcurrentlyStatus.CONSUME_SUCCESS) {
                        return status;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            log.info("优惠券事务秒杀 RocketMQ Consumer 初始化配置，namesrvAddr={}, consumerGroup={}, topic={}, maxReconsumeTimes={}",
                    namesrvAddr, consumerGroup, topic, RocketMQConfig.couponSeckillTxMaxReconsumeTimes());
            mqConsumer.start();
            consumer = mqConsumer;
            log.info("优惠券事务秒杀 RocketMQ Consumer 启动成功，namesrvAddr={}, consumerGroup={}, topic={}, maxReconsumeTimes={}",
                    namesrvAddr, consumerGroup, topic, RocketMQConfig.couponSeckillTxMaxReconsumeTimes());
        } catch (Exception e) {
            STARTED.set(false);
            log.error("优惠券事务秒杀 RocketMQ Consumer 启动失败", e);
        }
    }

    public static void shutdown() {
        if (!STARTED.compareAndSet(true, false)) {
            return;
        }
        DefaultMQPushConsumer current = consumer;
        if (current != null) {
            current.shutdown();
            consumer = null;
        }
        log.info("优惠券事务秒杀 RocketMQ Consumer 已关闭");
    }

    private static ConsumeConcurrentlyStatus processMessage(MessageExt message) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        CouponSeckillEvent event = JSONUtil.toBean(payload, CouponSeckillEvent.class);
        if (event == null || event.getOrderId() == null || event.getCouponId() == null
                || event.getUserId() == null || event.getCouponCode() == null) {
            log.error("事务秒杀消息格式错误，msgId={}, topic={}, group={}, reconsumeTimes={}, payload={}",
                    message.getMsgId(), message.getTopic(), RocketMQConfig.couponSeckillTxConsumerGroup(),
                    message.getReconsumeTimes(), payload);
            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
        }
        try {
            String notifyKey = SUCCESS_NOTIFY_KEY_PREFIX + event.getOrderId();
            if (!RedisUtil.setIfAbsent(notifyKey, "1", 24 * 60 * 60)) {
                log.info("事务秒杀消息重复消费，跳过重复 SUCCESS 推送，msgId={}, topic={}, group={}, reconsumeTimes={}, orderId={}, couponId={}, userId={}",
                        message.getMsgId(), message.getTopic(), RocketMQConfig.couponSeckillTxConsumerGroup(),
                        message.getReconsumeTimes(), event.getOrderId(), event.getCouponId(), event.getUserId());
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
            CouponSeckillResultMessage resultMessage = new CouponSeckillResultMessage();
            resultMessage.setType("COUPON_SECKILL_RESULT");
            resultMessage.setStatus("SUCCESS");
            resultMessage.setCouponId(event.getCouponId());
            resultMessage.setCouponCode(event.getCouponCode());
            resultMessage.setMessage("抢券成功");
            boolean sent = NotificationWebSocketServer.sendToUser(event.getUserId(), JSONUtil.toJson(resultMessage));
            if (sent) {
                log.info("事务秒杀 SUCCESS WebSocket 推送成功，msgId={}, topic={}, group={}, reconsumeTimes={}, orderId={}, couponId={}, userId={}",
                        message.getMsgId(), message.getTopic(), RocketMQConfig.couponSeckillTxConsumerGroup(),
                        message.getReconsumeTimes(), event.getOrderId(), event.getCouponId(), event.getUserId());
            } else {
                log.info("事务秒杀 SUCCESS 用户离线，消费仍视为成功，msgId={}, topic={}, group={}, reconsumeTimes={}, orderId={}, couponId={}, userId={}",
                        message.getMsgId(), message.getTopic(), RocketMQConfig.couponSeckillTxConsumerGroup(),
                        message.getReconsumeTimes(), event.getOrderId(), event.getCouponId(), event.getUserId());
            }
            if (message.getReconsumeTimes() > 0) {
                log.info("事务秒杀消息重复消费完成，msgId={}, orderId={}, reconsumeTimes={}",
                        message.getMsgId(), event.getOrderId(), message.getReconsumeTimes());
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        } catch (Exception e) {
            log.error("事务秒杀 Consumer 系统异常，等待 RocketMQ 重试，msgId={}, topic={}, group={}, reconsumeTimes={}, orderId={}, couponId={}, userId={}",
                    message.getMsgId(), message.getTopic(), RocketMQConfig.couponSeckillTxConsumerGroup(),
                    message.getReconsumeTimes(), event.getOrderId(), event.getCouponId(), event.getUserId(), e);
            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
        }
    }
}
