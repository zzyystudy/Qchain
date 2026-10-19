package com.zxyy.order.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxyy.order.domain.entity.ShopOrder;
import com.zxyy.order.mapper.ShopOrderMapper;
import com.zxyy.order.service.ShopOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/** 延时消息发送失败或耗尽重试时，仍然可以关闭过期订单并产生库存释放事件。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredOrderJob {
    private final ShopOrderMapper orderMapper;
    private final ShopOrderService orderService;

    @Scheduled(fixedDelayString = "${qchain.order.expiry-scan-delay:60000}")
    public void closeExpiredOrders() {
        for (ShopOrder order : orderMapper.selectList(new LambdaQueryWrapper<ShopOrder>()
                .eq(ShopOrder::getOrderStatus, 10).eq(ShopOrder::getPaymentStatus, 0)
                .le(ShopOrder::getExpireTime, LocalDateTime.now()).last("LIMIT 100"))) {
            try {
                orderService.restoreStock(order.getId());
            } catch (Exception exception) {
                log.warn("关闭过期订单失败，orderNo={}", order.getOrderNo(), exception);
            }
        }
    }
}
