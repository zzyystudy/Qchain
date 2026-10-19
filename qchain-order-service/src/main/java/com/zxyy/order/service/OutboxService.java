package com.zxyy.order.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxyy.order.constant.RabbitMqConstant;
import com.zxyy.order.mapper.OutboxMapper;
import com.zxyy.order.domain.entity.OutboxMessage;
import com.zxyy.api.dto.product.OrderInventoryDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OutboxService
        extends ServiceImpl<OutboxMapper, OutboxMessage> {

    private final RabbitTemplate outboxTemplate;
    private final ObjectMapper objectMapper;

    public OutboxService(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // 单独的发送模板，避免影响现有延时消息。
        outboxTemplate =
                new RabbitTemplate(rabbitTemplate.getConnectionFactory());

        outboxTemplate.setMessageConverter(
                rabbitTemplate.getMessageConverter());

        outboxTemplate.setMandatory(true);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveInventoryReleaseMessage(OrderInventoryDTO request) {
        saveReleaseMessage(request);
    }

    /** 下单事务已经回滚，补偿消息必须单独开启事务保存。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveRollbackReleaseMessage(OrderInventoryDTO request) {
        saveReleaseMessage(request);
    }

    private void saveReleaseMessage(OrderInventoryDTO request) {
        String eventKey = request.getOrderNo() + ":inventory-release";
        if (lambdaQuery().eq(OutboxMessage::getEventKey, eventKey).exists()) {
            return;
        }
        OutboxMessage message = new OutboxMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setEventKey(eventKey);
        message.setExchangeName(RabbitMqConstant.DELIVERY_EXCHANGE);
        message.setRoutingKey(RabbitMqConstant.INVENTORY_RELEASE_ROUTING_KEY);
        try {
            message.setPayload(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("库存释放请求序列化失败", exception);
        }
        message.setStatus(0);
        message.setRetryCount(0);
        message.setNextRetryTime(LocalDateTime.now());
        message.setCreatedTime(LocalDateTime.now());
        if (!save(message)) {
            throw new IllegalStateException("保存库存释放消息失败");
        }
    }

    /**
     * 加入调用方已有的数据库事务。
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveDeliveryMessage(String orderNo) {
        OutboxMessage message = new OutboxMessage();

        message.setMessageId(UUID.randomUUID().toString());
        message.setEventKey(orderNo + ":delivery");
        message.setExchangeName(RabbitMqConstant.DELIVERY_EXCHANGE);
        message.setRoutingKey(RabbitMqConstant.DELIVERY_ROUTING_KEY);
        message.setPayload(orderNo);

        message.setStatus(0);
        message.setRetryCount(0);
        message.setNextRetryTime(LocalDateTime.now());
        message.setCreatedTime(LocalDateTime.now());

        if (!save(message)) {
            throw new IllegalStateException("保存交付消息失败");
        }
    }

    /**
     * 查询并发送待发送消息。
     * 不给整个方法加数据库事务。
     */
    @Scheduled(fixedDelay = 1000)
    public void sendPendingMessages() {
        List<OutboxMessage> messages = lambdaQuery()
                .eq(OutboxMessage::getStatus, 0)
                .le(OutboxMessage::getNextRetryTime, LocalDateTime.now())
                .orderByAsc(OutboxMessage::getNextRetryTime)
                .orderByAsc(OutboxMessage::getMessageId)
                .last("LIMIT 10")
                .list();

        for (OutboxMessage message : messages) {
            try {
                sendAndConfirm(message);

                boolean updated = lambdaUpdate()
                        .set(OutboxMessage::getStatus, 1)
                        .set(OutboxMessage::getLastError, null)
                        .eq(OutboxMessage::getMessageId,
                                message.getMessageId())
                        .eq(OutboxMessage::getStatus, 0)
                        .update();

                if (!updated) {
                    throw new IllegalStateException("更新消息发送状态失败");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;

            } catch (Exception e) {
                String error = e.toString();
                if (error.length() > 1000) {
                    error = error.substring(0, 1000);
                }

                lambdaUpdate()
                        .setSql("retry_count = retry_count + 1")
                        .set(OutboxMessage::getNextRetryTime,
                                LocalDateTime.now().plusSeconds(10))
                        .set(OutboxMessage::getLastError, error)
                        .eq(OutboxMessage::getMessageId,
                                message.getMessageId())
                        .eq(OutboxMessage::getStatus, 0)
                        .update();

                log.warn("发送失败，messageId={}",
                        message.getMessageId(), e);
            }
        }
    }

    /**
     * 真正发送 MQ，并检查发布确认和退回结果。
     */
    private void sendAndConfirm(OutboxMessage message)
            throws Exception {

        CorrelationData correlation =
                new CorrelationData(UUID.randomUUID().toString());

        outboxTemplate.convertAndSend(
                message.getExchangeName(),
                message.getRoutingKey(),
                message.getPayload(),
                amqpMessage -> {
                    amqpMessage.getMessageProperties()
                            .setMessageId(message.getMessageId());

                    amqpMessage.getMessageProperties()
                            .setDeliveryMode(MessageDeliveryMode.PERSISTENT);

                    return amqpMessage;
                },
                correlation
        );

        CorrelationData.Confirm confirm =
                correlation.getFuture().get(5, TimeUnit.SECONDS);

        if (!confirm.isAck()) {
            throw new IllegalStateException(
                    "Broker未确认：" + confirm.getReason());
        }

        if (correlation.getReturned() != null) {
            throw new IllegalStateException(
                    "消息无法路由："
                            + correlation.getReturned().getReplyText());
        }
    }
}
