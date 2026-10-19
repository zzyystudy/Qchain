package com.zxyy.order.controller.guest;

import com.alipay.api.AlipayApiException;
import com.zxyy.api.dto.order.OrderSubmitDTO;
import com.zxyy.api.dto.order.QueryOrderDTO;
import com.zxyy.api.vo.order.OrderSubmitVO;
import com.zxyy.api.vo.order.ShopVirtualGoodVO;
import com.zxyy.api.vo.order.SimpleOrderVO;
import com.zxyy.common.result.Result;
import com.zxyy.order.service.ShopOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/guest/shopOrder")
@Tag(name = "订单接口")
@Slf4j
public class ShopOrderController {
    @Autowired
    private ShopOrderService shopOrderService;

    @GetMapping
    @Operation(summary = "用户付款接口")
    public Result<String> alipay(String orderNo) throws AlipayApiException {
        log.info("支付订单:{}",orderNo);
        String content = shopOrderService.pay(orderNo);
        return Result.success(content);
    }



    @PostMapping("/submit")
    @Operation(summary = "用户下单")
    public Result<OrderSubmitVO> submit(@jakarta.validation.Valid @RequestBody OrderSubmitDTO orderSubmitDTO){
        log.info("用户下单，请求号:{}",orderSubmitDTO.getClientRequestNo());
        OrderSubmitVO orderSubmitVO = shopOrderService.submitOrder(orderSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    //查询订单信息的接口
    @GetMapping("/simple")
    @Operation(summary = "根据邮箱地址查询订单")
    public Result<List<SimpleOrderVO>> getSimpleOrderByEmail(String email){
        log.info("根据邮箱查询:{}",email);
        List<SimpleOrderVO> simpleOrderVOList = shopOrderService.getSimpleOrderByEmail(email);
        return Result.success(simpleOrderVOList);
    }

    //查询订单商品的接口
    @PostMapping("/detail")
    @Operation(summary = "查询订单的详细信息")
    public Result<ShopVirtualGoodVO> getOrderDetail(@jakarta.validation.Valid @RequestBody QueryOrderDTO queryOrderDTO){
        log.info("查询订单:{}",queryOrderDTO.getOrderNo());
        ShopVirtualGoodVO shopVirtualGoodVO = shopOrderService.getOrderDetail(queryOrderDTO);
        return Result.success(shopVirtualGoodVO);
    }}
