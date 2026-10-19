package com.zxyy.order.util;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxyy.order.exception.OrderException;
import com.zxyy.order.properties.AliPayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PayUtil {
    private final AliPayProperties properties;
    private final ObjectMapper objectMapper;
    private volatile AlipayClient alipayClient;

    public String sendRequestToAlipay(String orderNo, BigDecimal amount, String subject) throws AlipayApiException {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setReturnUrl(properties.getReturnUrl());
        request.setNotifyUrl(properties.getNotifyUrl());
        request.setBizContent(json(Map.of("out_trade_no", orderNo, "total_amount", amount,
                "subject", subject, "product_code", "FAST_INSTANT_TRADE_PAY")));
        return client().pageExecute(request).getBody();
    }

    public String query(String orderNo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent(json(Map.of("out_trade_no", orderNo)));
        try {
            AlipayTradeQueryResponse response = client().execute(request);
            if (!response.isSuccess()) {
                throw new OrderException("支付宝查询失败：" + response.getSubMsg());
            }
            return response.getBody();
        } catch (AlipayApiException exception) {
            throw new OrderException("支付宝查询失败");
        }
    }

    private AlipayClient client() {
        if (alipayClient == null) {
            synchronized (this) {
                if (alipayClient == null) {
                    alipayClient = new DefaultAlipayClient(properties.getGatewayUrl(), properties.getAppId(),
                            properties.getAppPrivateKey(), properties.getFormat(), properties.getCharset(),
                            properties.getAlipayPublicKey(), properties.getSignType());
                }
            }
        }
        return alipayClient;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("支付请求序列化失败", exception);
        }
    }
}
