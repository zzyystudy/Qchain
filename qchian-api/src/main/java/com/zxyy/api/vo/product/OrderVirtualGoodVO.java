package com.zxyy.api.vo.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVirtualGoodVO {
    private Long orderItemId;
    private Long productId;
    private String context;
}
