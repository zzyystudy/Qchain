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
@TableName("chat_message")
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    // 消息主键
    @TableId(type = IdType.AUTO)
    private Long id;

    // 所属会话ID
    private Long conversationId;

    // 会话内递增消息序号
    private Long seq;

    // 客户端消息标识，用于重试去重
    private String clientMsgId;

    // 1买家，2管理员
    private Integer senderType;

    // 买家填0，管理员填真实ID
    private Long senderId;

    // 消息正文
    private String content;

    private LocalDateTime createTime;
}
