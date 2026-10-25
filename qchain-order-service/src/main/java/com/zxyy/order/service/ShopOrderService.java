package com.zxyy.order.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.api.AlipayApiException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxyy.common.constant.MessageConstant;
import com.zxyy.order.constant.RabbitMqConstant;
import com.zxyy.common.exception.BaseException;
import com.zxyy.order.exception.OrderException;
import com.zxyy.order.exception.PasswordError;
import com.zxyy.order.mapper.OrderItemMapper;
import com.zxyy.order.mapper.ShopOrderMapper;
import com.zxyy.order.messaging.DelayMessageProcessor;
import com.zxyy.order.messaging.MultiDelayMessage;
import com.zxyy.api.dto.order.BuyProductDTO;
import com.zxyy.api.dto.order.OrderSubmitDTO;
import com.zxyy.api.dto.order.QueryOrderDTO;
import com.zxyy.order.domain.entity.OrderItem;
import com.zxyy.order.domain.entity.ShopOrder;
import com.zxyy.api.vo.order.OrderSubmitVO;
import com.zxyy.api.vo.order.ShopVirtualGoodVO;
import com.zxyy.api.vo.order.SimpleOrderVO;
import com.zxyy.api.vo.order.VirtualGoodsVO;
import com.zxyy.common.util.*;
import com.zxyy.order.util.PasswordUtil;
import com.zxyy.order.util.PayUtil;
import com.zxyy.api.dto.product.*;
import com.zxyy.api.vo.product.*;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ShopOrderService extends ServiceImpl<ShopOrderMapper, ShopOrder> {
    @Autowired
    private AESGCMUtil aesgcmUtil;
    @Autowired
    private OrderProductGateway productGateway;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ShopOrderMapper shopOrderMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private PayUtil payUtil;
    @Autowired
    private OutboxService outboxService;
    /**
     * 创建订单
     *
     * @param orderSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrderSubmitDTO orderSubmitDTO) {
        String clientRequestNo = orderSubmitDTO.getClientRequestNo();
        String email = orderSubmitDTO.getEmail();
        String queryPassword = orderSubmitDTO.getQueryPassword();
        String buyerRemark = orderSubmitDTO.getBuyerRemark();
        List<BuyProductDTO> items = orderSubmitDTO.getItems();
        StringBuilder payload = new StringBuilder(email + "|" + queryPassword + "|" +buyerRemark +"|");
        for(BuyProductDTO buyProductDTO : items){
            payload.append(buyProductDTO.getProductNo()).append("|").append(buyProductDTO.getQuantity()).append("|");
        }
        //计算请求指纹
        String requestFingerprint = HmacSha256Util.sign(payload.toString());
        //校验全局唯一的下单幂等号 防止重复下单
        ShopOrder shopOrder = query().eq("client_request_no", clientRequestNo).one();
        if(shopOrder != null){
            //如果重复下单 校验订单指纹
            if(!shopOrder.getRequestFingerprint().equals(requestFingerprint)){
                throw new OrderException(MessageConstant.IDEMPOTENCY_CHECK_FAILED);
            }
            //订单指纹一致 直接返回原订单
            return BeanUtil.copyProperties(shopOrder, OrderSubmitVO.class);
        }
        //规范化邮箱计算邮箱的HMAC指纹并加密 有相同的直接返回已存在的订单
        //1.将邮箱中的字母全部转为小写
        String lowEmail = email.toLowerCase();
        //2.对邮箱进行加密 并计算指纹
        AESGCMUtil.EncryptedPayload encryptedPayload = aesgcmUtil.encrypt(lowEmail);
        String buyerEmailFingerprint = HmacSha256Util.sign(lowEmail);
        //通过商品服务获取上架且未删除的商品快照
        List<OrderProductVO> products = productGateway.snapshots(
                items.stream().map(BuyProductDTO::getProductNo).toList());
        if(products.size() != items.size()){
            throw new OrderException(MessageConstant.PRODUCT_NOT_SOLD);
        }
        Map<String, OrderProductVO> collect = products.stream()
                .collect(Collectors.toMap(OrderProductVO::getProductNo, p -> p));
        //使用数据库中的商品价格计算订单金额
        BigDecimal goodsAmount = new BigDecimal(0);
        int itemQuantity = 0;
        for(BuyProductDTO buyProductDTO : items){
            BigDecimal price = collect.get(buyProductDTO.getProductNo()).getPrice();
            BigDecimal quantity = BigDecimal.valueOf(buyProductDTO.getQuantity());
            goodsAmount = goodsAmount.add(price.multiply(quantity));
            itemQuantity += buyProductDTO.getQuantity();
        }
        //2.创建订单主表
        ShopOrder newShopOrder = new ShopOrder();
        newShopOrder.setOrderNo(ULIDUtil.generateULID());
        newShopOrder.setClientRequestNo(clientRequestNo);
        newShopOrder.setRequestFingerprint(requestFingerprint);
        newShopOrder.setBuyerEmailCiphertext(encryptedPayload.ciphertext());
        newShopOrder.setBuyerEmailNonce(encryptedPayload.nonce());
        newShopOrder.setBuyerEmailKeyVersion("2026"); //TODO 固定密钥
        newShopOrder.setBuyerEmailFingerprint(buyerEmailFingerprint);
        newShopOrder.setQueryPasswordHash(PasswordUtil.encrypt(queryPassword));
        newShopOrder.setGoodsAmount(goodsAmount);
        newShopOrder.setPayableAmount(goodsAmount);
        newShopOrder.setItemQuantity(itemQuantity);
        newShopOrder.setBuyerRemark(buyerRemark);
        newShopOrder.setExpireTime(LocalDateTime.now().plusMinutes(15));
        newShopOrder.setOrderStatus(10);
        newShopOrder.setPaymentStatus(0);
        newShopOrder.setFulfillmentStatus(0);
        save(newShopOrder);

        //创建订单明细表
        int i = 0;
        //创建订单明细表
        for(BuyProductDTO buyProductDTO : items){
            String productNo = buyProductDTO.getProductNo();
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(newShopOrder.getId());
            orderItem.setLineNo(i++);
            orderItem.setProductId(collect.get(productNo).getId());
            orderItem.setProductNoSnapshot(productNo);
            orderItem.setProductName(collect.get(productNo).getName());
            orderItem.setProductImageUrl(collect.get(productNo).getImage());
            orderItem.setDeliveryType(collect.get(productNo).getDeliveryType());
            orderItem.setUnitPrice(collect.get(productNo).getPrice());
            orderItem.setQuantity(buyProductDTO.getQuantity());
            orderItem.setGoodsAmount(collect.get(productNo).getPrice().multiply(BigDecimal.valueOf(buyProductDTO.getQuantity())));
            orderItem.setPayableAmount(orderItem.getGoodsAmount()); //TODO 真正的优惠 设置 优惠引擎？？
            orderItemMapper.insert(orderItem);
        }
        OrderInventoryDTO inventory = inventoryRequest(newShopOrder);
        // HTTP 超时也可能已经预占成功；本地回滚后用 Outbox 持久化释放请求。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    outboxService.saveRollbackReleaseMessage(inventory);
                }
            }
            @Override
            public void afterCommit() {
                try {
                    MultiDelayMessage<Long> msg = new MultiDelayMessage<>(newShopOrder.getId());
                    rabbitTemplate.convertAndSend(RabbitMqConstant.DELAY_EXCHANGE,
                            RabbitMqConstant.DELAY_ORDER_ROUTING_KEY, msg,
                            new DelayMessageProcessor(msg.removeNextDelay()));
                } catch (Exception exception) {
                    log.error("订单延时消息发送失败，orderNo={}",
                            newShopOrder.getOrderNo(), exception);
                }
            }
        });
        productGateway.reserve(inventory);
        return BeanUtil.copyProperties(newShopOrder,OrderSubmitVO.class);
    }

    /**
     * 调用支付宝支付接口
     * @param orderNo
     * @return
     */
    public String pay(String orderNo) throws AlipayApiException {
        //查询金额 发送请求
        ShopOrder shopOrder = lambdaQuery()
                .eq(ShopOrder::getOrderNo, orderNo)
                .one();
        //判断订单状态 待支付才可以进行支付  和未支付才可以处理
        if(shopOrder.getOrderStatus() != 10 || shopOrder.getPaymentStatus() != 0){
            throw new OrderException("订单正在处理");
        }
        //回调时post的参数 就是orderNo 我们传入的第一个值
        return payUtil.sendRequestToAlipay(orderNo, shopOrder.getPayableAmount(), "用户支付");
    }

    /**
     * 订单超时恢复库存
     * @param orderId
     */
    @Transactional
    public void restoreStock(Long orderId) {
        int rows = shopOrderMapper.closeById(orderId);
        if(rows == 0){
            //没有修改成功说明已经支付成功了 直接返回
            return;
        }
        ShopOrder order = shopOrderMapper.selectById(orderId);
        // 订单关闭和释放事件同事务提交，避免远程释放成功后本地回滚重新变成待支付。
        outboxService.saveInventoryReleaseMessage(inventoryRequest(order));
    }

    @Transactional
    public void paySuccess(String orderNo, String totalAmount) {
        //支付成功修改订单 库存状态 修改product已经售出的库存
        //1.先修改这个订单的状态 成功再往下走
        int rows = shopOrderMapper.markPaid(orderNo,totalAmount);
        if(rows == 0){
            //如果支付成功后 这个超时订单刚好关闭了
            //TODO 退款 ???

            return;
        }
        outboxService.saveDeliveryMessage(orderNo);
    }

    /**
     * 根据订单 编号返回 虚拟商品信息
     *
     * @param orderNo 订单编号
     * @return 虚拟商品信息
     */
    public ShopVirtualGoodVO listVirtualItemByOrderNo(String orderNo) {
        ShopOrder shopOrder = lambdaQuery().eq(ShopOrder::getOrderNo, orderNo).one();
        return getShopVirtualGoodVO(orderNo, shopOrder);
    }

    private ShopVirtualGoodVO getShopVirtualGoodVO(String orderNo, ShopOrder shopOrder) {
        //解密邮箱
        String email = aesgcmUtil.decrypt(shopOrder.getBuyerEmailCiphertext(), shopOrder.getBuyerEmailNonce());
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, shopOrder.getId());
        List<OrderItem> orderItemList = orderItemMapper.selectList(wrapper);
        Map<Long, OrderItem> orderItemMap = orderItemList.stream()
                .collect(Collectors.toMap(OrderItem::getId, item -> item));
        List<VirtualGoodsVO> virtualGoodsVOList = productGateway.content(inventoryRequest(shopOrder)).stream()
                .map(stock -> {
                    VirtualGoodsVO vo = new VirtualGoodsVO();
                    vo.setProductName(orderItemMap.get(stock.getOrderItemId()).getProductName());
                    vo.setContext(stock.getContext());
                    return vo;
                }).toList();
        ShopVirtualGoodVO shopVirtualGoodVO = new ShopVirtualGoodVO();
        shopVirtualGoodVO.setEmail(email);
        shopVirtualGoodVO.setShopOrderNo(orderNo);
        shopVirtualGoodVO.setVirtualGoodsVOList(virtualGoodsVOList);
        return shopVirtualGoodVO;
    }

    @Transactional
    public void completeOrderByNo(String orderNo) {
        //
        boolean update = lambdaUpdate().set(ShopOrder::getCompletedTime, LocalDateTime.now())
                .set(ShopOrder::getFulfillmentStatus, 2)
                .set(ShopOrder::getOrderStatus, 30)
                .eq(ShopOrder::getOrderNo, orderNo)
                .eq(ShopOrder::getOrderStatus, 20)
                .update();
        if (!update){
            //更新失败 直接返回
            return;
        }
        ShopOrder shopOrder = lambdaQuery().eq(ShopOrder::getOrderNo, orderNo).one();
        LambdaQueryWrapper<OrderItem> queryWrapper = new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId,shopOrder.getId());
        List<OrderItem> orderItemList = orderItemMapper.selectList(queryWrapper);
        orderItemList.forEach(orderItem -> {
            LambdaUpdateWrapper<OrderItem> updateWrapper = new LambdaUpdateWrapper<OrderItem>()
                    .set(OrderItem::getDeliveredTime,LocalDateTime.now())
                    .set(OrderItem::getDeliveredQuantity,orderItem.getQuantity())
                    .eq(OrderItem::getId,orderItem.getId());
            orderItemMapper.update(updateWrapper);

        });
        productGateway.complete(inventoryRequest(shopOrder));
    }

    private OrderInventoryDTO inventoryRequest(ShopOrder order) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        return new OrderInventoryDTO(order.getOrderNo(), order.getExpireTime(), items.stream()
                .map(item -> new OrderInventoryLineDTO(item.getId(), item.getProductId(), item.getQuantity()))
                .toList());
    }

    public List<SimpleOrderVO> getSimpleOrderByEmail(String email) {
        String lowerCase = email.toLowerCase();
        String buyerEmailSign = HmacSha256Util.sign(lowerCase);
        List<ShopOrder> list = lambdaQuery()
                .eq(ShopOrder::getBuyerEmailFingerprint, buyerEmailSign)
                .list();
        List<SimpleOrderVO> simpleOrderVOList = list.stream().map(shopOrder -> {
            return BeanUtil.copyProperties(shopOrder, SimpleOrderVO.class);
        }).toList();
        return simpleOrderVOList;
    }

    public ShopVirtualGoodVO getOrderDetail(QueryOrderDTO queryOrderDTO) {
        String lowerCaseEmail = queryOrderDTO.getEmail().toLowerCase();
        String emailSign = HmacSha256Util.sign(lowerCaseEmail);
        ShopOrder shopOrder = lambdaQuery()
                .eq(ShopOrder::getOrderNo, queryOrderDTO.getOrderNo())
                .eq(ShopOrder::getPaymentStatus,1)
                .one();
        if (ObjectUtil.isNull(shopOrder)){
            throw new BaseException(MessageConstant.ORDER_NOT_PAID);
        }
        boolean passwordMatch = PasswordUtil.matches(queryOrderDTO.getPassword(), shopOrder.getQueryPasswordHash());
        boolean emailMatch = emailSign.equals(shopOrder.getBuyerEmailFingerprint());
        if(passwordMatch && emailMatch){
            return getShopVirtualGoodVO(queryOrderDTO.getOrderNo(),shopOrder);
        }else {
            throw new PasswordError(MessageConstant.PASSWORD_ERROR);
        }
    }
}
