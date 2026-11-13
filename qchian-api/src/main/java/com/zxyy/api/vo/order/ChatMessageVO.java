package com.zxyy.api.vo.order;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatMessageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long messageId;
    private Long seq;
    private String clientMsgId;
    // 0 为买家，正数为管理员ID
    private Long senderId;
    private String content;
    private LocalDateTime createTime;
}
