package com.zxyy.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxyy.product.domain.entity.VirtualGoodItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface VirtualGoodItemMapper extends BaseMapper<VirtualGoodItem> {


    @Select("""
        SELECT *
        FROM virtual_goods_item
        WHERE product_id = #{productId}
          AND status = 0
        ORDER BY id
        LIMIT #{quantity}
        FOR UPDATE SKIP LOCKED
        """)
    List<VirtualGoodItem> selectAvailableForUpdate(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity);

    @Update("UPDATE virtual_goods_item SET status = 0, order_item_id = NULL " +
            "WHERE order_item_id = #{orderItemId} AND status = 1")
    int releaseReserved(@Param("orderItemId") Long orderItemId);

    @Update("UPDATE virtual_goods_item SET status = 2, delivered_time = NOW() " +
            "WHERE order_item_id = #{orderItemId} AND status = 1")
    int completeReserved(@Param("orderItemId") Long orderItemId);
}
