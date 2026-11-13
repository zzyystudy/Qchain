package com.zxyy.order.ws;

import lombok.AllArgsConstructor;

public record WsMessage(String type,
                        String requestId,
                        String orderNo,
                        String content,
                        Long seq) {}
