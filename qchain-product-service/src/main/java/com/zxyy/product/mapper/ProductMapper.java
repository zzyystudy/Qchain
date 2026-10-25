package com.zxyy.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxyy.product.domain.entity.Product;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;

public interface ProductMapper extends BaseMapper<Product> {
    @Select("SELECT * FROM product WHERE id = #{id} FOR UPDATE")
    Product lockById(@Param("id") Long id);

    @Update("UPDATE product SET available_stock = available_stock - #{num} " +
            "WHERE id = #{id} AND available_stock >= #{num}")
    int deductStock(@Param("id") Long productId,@Param("num") int num);

    @Update("UPDATE product SET available_stock = available_stock + #{num} " +
            "WHERE id = #{id}")
    int addStock(@Param("id") Long productId, @Param("num") int size);

    @Update("UPDATE product SET available_stock = available_stock - #{num} " +
            "WHERE product_no = #{no} AND available_stock >= #{num}")
    int deductStockByNo(@Param("no") String productNo, @Param("num")Integer quantity);

    @Update("UPDATE product SET sold_count = sold_count + #{quantity} " +
            "WHERE id = #{productId}")
    int addSoldCount(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
