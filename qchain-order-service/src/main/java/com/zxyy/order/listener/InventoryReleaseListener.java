package com.zxyy.order.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxyy.api.dto.product.OrderInventoryDTO;
import com.zxyy.order.constant.RabbitMqConstant;
import com.zxyy.order.service.OrderProductGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryReleaseListener {
    private final ObjectMapper objectMapper;
    private final OrderProductGateway productGateway;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitMqConstant.INVENTORY_RELEASE_QUEUE, durable = "true"),
            exchange = @Exchange(value = RabbitMqConstant.DELIVERY_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = RabbitMqConstant.INVENTORY_RELEASE_ROUTING_KEY))
    public void release(String payload) throws JsonProcessingException {
        productGateway.release(objectMapper.readValue(payload, OrderInventoryDTO.class));
    }
}
