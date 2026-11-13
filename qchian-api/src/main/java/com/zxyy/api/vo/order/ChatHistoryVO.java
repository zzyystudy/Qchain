package com.zxyy.api.vo.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String orderNo;
    private Long lastSeq;
    private Long buyerReadSeq;
    private Long adminReadSeq;
    // 始终按 seq 升序返回，方便页面直接展示
    private List<ChatMessageVO> messages;
    // 当前查询方向上是否还有更多消息
    private boolean hasMore;
}
