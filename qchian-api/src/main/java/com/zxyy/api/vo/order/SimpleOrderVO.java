package com.zxyy.api.vo.order;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SimpleOrderVO implements Serializable {
    private static final long serialVersionUID = 1L;

    // 业务订单号
    private String orderNo;

    // 订单状态
    private Integer orderStatus;
    private Integer paymentStatus;
    private Integer fulfillmentStatus;

    // 金额
    private BigDecimal goodsAmount;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
    private BigDecimal paidAmount;
    private BigDecimal refundAmount;

    // 数量
    private Integer itemQuantity;

    // 备注与原因
    private String buyerRemark;
    private String closeReason;

    // 时间
    private LocalDateTime expireTime;
    private LocalDateTime paidTime;
    private LocalDateTime completedTime;
    private LocalDateTime cancelledTime;
    private LocalDateTime closedTime;
    private LocalDateTime createTime;

}
