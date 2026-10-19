package com.zxyy.order.controller.notify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxyy.common.result.Result;
import com.zxyy.order.service.ShopOrderService;
import com.zxyy.order.util.PayUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/guest/notify")
@Tag(name = "回调接口")
public class PayNotifyController {
    private final PayUtil payUtil;
    private final ShopOrderService orderService;
    private final ObjectMapper objectMapper;
    @Value("${shop.payment.success-redirect:http://localhost:5173/user/cart}")
    private String successRedirect;

    /** 沿用原流程：主动查询支付宝，以服务端查询结果处理支付成功。 */
    @PostMapping
    @Operation(summary = "支付宝回调接口")
    public Result<String> returns(@RequestParam("out_trade_no") String orderNo) throws JsonProcessingException {
        JsonNode response = objectMapper.readTree(payUtil.query(orderNo)).path("alipay_trade_query_response");
        if ("TRADE_SUCCESS".equals(response.path("trade_status").asText())) {
            orderService.paySuccess(orderNo, response.path("total_amount").asText());
            return Result.success("redirect:" + successRedirect);
        }
        return Result.success("index");
    }
}
