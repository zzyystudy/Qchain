package com.zxyy.order.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_conversation")
public class ChatConversation implements Serializable {

    private static final long serialVersionUID = 1L;

    // 会话主键
    @TableId(type = IdType.AUTO)
    private Long id;

    // 关联订单号
    private String orderNo;

    // 会话内最新消息序号
    private Long lastSeq;

    // 买家已读到的消息序号
    private Long buyerReadSeq;

    // 客服侧共享的已读消息序号
    private Long adminReadSeq;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
