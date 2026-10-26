package com.zxyy.api.client;

import com.zxyy.api.dto.order.OrderSubmitDTO;
import com.zxyy.api.dto.order.QueryOrderDTO;
import com.zxyy.api.vo.order.*;
import com.zxyy.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "order-service", url = "${qchain.clients.order-url:}", contextId = "orderClient")
public interface OrderClient {
    @PostMapping("/guest/shopOrder/submit")
    Result<OrderSubmitVO> submit(@RequestBody OrderSubmitDTO request);

    @GetMapping("/guest/shopOrder")
    Result<String> pay(@RequestParam("orderNo") String orderNo);

    @GetMapping("/guest/shopOrder/simple")
    Result<List<SimpleOrderVO>> listByEmail(@RequestParam("email") String email);

    @PostMapping("/guest/shopOrder/detail")
    Result<ShopVirtualGoodVO> detail(@RequestBody QueryOrderDTO request);

}
