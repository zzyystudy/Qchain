package com.zxyy.api.vo.product;

import lombok.Data;
import java.math.BigDecimal;

/** 下单需要的商品快照，不暴露商品数据库实体。 */
@Data
public class OrderProductVO {
    private Long id;
    private String productNo;
    private String name;
    private String image;
    private Integer deliveryType;
    private BigDecimal price;
    private Integer purchaseLimit;
}
