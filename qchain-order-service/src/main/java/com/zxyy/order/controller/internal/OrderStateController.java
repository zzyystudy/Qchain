package com.zxyy.order.controller.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxyy.api.vo.order.OrderStateVO;
import com.zxyy.common.result.Result;
import com.zxyy.order.domain.entity.ShopOrder;
import com.zxyy.order.mapper.ShopOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/orders")
public class OrderStateController {
    private final ShopOrderMapper orderMapper;

    @GetMapping("/{orderNo}/state")
    public Result<OrderStateVO> state(@PathVariable String orderNo) {
        ShopOrder order = orderMapper.selectOne(new LambdaQueryWrapper<ShopOrder>()
                .eq(ShopOrder::getOrderNo, orderNo));
        return Result.success(order == null ? null : new OrderStateVO(order.getOrderNo(), order.getOrderStatus()));
    }
}
