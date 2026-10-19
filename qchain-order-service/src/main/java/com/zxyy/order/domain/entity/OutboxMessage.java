package com.zxyy.order.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mq_outbox")
public class OutboxMessage {

    @TableId(type = IdType.INPUT)
    private String messageId;

    private String eventKey;
    private String exchangeName;
    private String routingKey;
    private String payload;

    private Integer status;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private String lastError;
    private LocalDateTime createdTime;
}
