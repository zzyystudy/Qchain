package com.zxyy.order.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxyy.order.mapper.OrderItemMapper;
import com.zxyy.order.domain.entity.OrderItem;
import org.springframework.stereotype.Service;

@Service
public class OrderItemService extends ServiceImpl<OrderItemMapper, OrderItem> {
}
