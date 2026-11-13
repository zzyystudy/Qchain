package com.zxyy.api.dto.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class ChatConversationPageQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull
    @Min(1)
    private Integer page = 1;

    @NotNull
    @Min(1)
    @Max(100)
    private Integer pageSize = 20;

    // 可选：按订单号精确过滤
    @Size(max = 64)
    private String orderNo;
}
