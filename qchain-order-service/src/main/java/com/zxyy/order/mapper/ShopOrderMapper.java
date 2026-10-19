package com.zxyy.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxyy.order.domain.entity.ShopOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ShopOrderMapper extends BaseMapper<ShopOrder> {

    @Update("UPDATE shop_order SET order_status = 40, closed_time = NOW(),close_reason = '超时未支付' " +
            "WHERE id = #{id} AND payment_status = 0 AND order_status = 10")
    int closeById(@Param("id") Long orderId);

    @Update("UPDATE shop_order SET payment_status = 1, paid_time = NOW(),order_status = 20,paid_amount = #{totalAmount} " +
            "WHERE order_no = #{orderNo} AND order_status = 10 AND payment_status = 0")
    int markPaid(@Param("orderNo") String orderNo, @Param("totalAmount") String totalAmount);
}
