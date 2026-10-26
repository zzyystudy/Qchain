package com.zxyy.api.dto.order;

import lombok.Data;

@Data
public class BuyProductDTO {
    //商品编号
    @jakarta.validation.constraints.NotBlank
    private String productNo;
    //商品数量
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Integer quantity;
}
