package com.zxyy.api.vo.order;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatConversationVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long conversationId;
    private String orderNo;
    private Long lastSeq;
    private Long buyerReadSeq;
    private Long adminReadSeq;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    // 仅统计买家发送、且客服尚未读到的消息
    private Long unreadCount;
}
