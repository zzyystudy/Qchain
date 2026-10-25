package com.zxyy.product.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxyy.api.dto.product.OrderInventoryDTO;
import com.zxyy.api.dto.product.OrderInventoryLineDTO;
import com.zxyy.api.vo.product.OrderProductVO;
import com.zxyy.api.vo.product.OrderVirtualGoodVO;
import com.zxyy.common.constant.MessageConstant;
import com.zxyy.common.util.AESGCMUtil;
import com.zxyy.product.domain.entity.Product;
import com.zxyy.product.domain.entity.VirtualGoodItem;
import com.zxyy.product.exception.BizException;
import com.zxyy.product.mapper.ProductMapper;
import com.zxyy.product.mapper.VirtualGoodItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderInventoryService {
    private final ProductMapper productMapper;
    private final VirtualGoodItemMapper virtualGoodItemMapper;
    private final AESGCMUtil aesgcmUtil;

    public List<OrderProductVO> snapshots(List<String> productNos) {
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .in(Product::getProductNo, productNos)
                        .eq(Product::getStatus, 1).eq(Product::getIsDeleted, 0))
                .stream().map(product -> BeanUtil.copyProperties(product, OrderProductVO.class)).toList();
    }

    @Transactional
    public void reserve(OrderInventoryDTO request) {
        lockProducts(request);
        for (OrderInventoryLineDTO line : sortedLines(request)) {
            List<VirtualGoodItem> bound = boundStocks(line);
            if (!bound.isEmpty()) {
                checkQuantity(line, bound);
                if (bound.stream().anyMatch(stock -> stock.getStatus() == 0)) {
                    throw new BizException("订单库存已经释放");
                }
                if (bound.stream().allMatch(stock -> stock.getStatus() == 1 || stock.getStatus() == 2)) continue;
                throw new BizException("订单库存状态不允许预占");
            }
            if (!request.getExpireTime().isAfter(LocalDateTime.now())) {
                throw new BizException(MessageConstant.ORDER_OUTOFTIME);
            }
            List<VirtualGoodItem> stocks = virtualGoodItemMapper.selectAvailableForUpdate(
                    line.getProductId(), line.getQuantity());
            if (stocks.size() != line.getQuantity()) {
                throw new BizException(MessageConstant.OUT_OF_STOCK);
            }
            for (VirtualGoodItem stock : stocks) {
                stock.setStatus(1);
                stock.setOrderItemId(line.getOrderItemId());
                virtualGoodItemMapper.updateById(stock);
            }
            if (productMapper.deductStock(line.getProductId(), line.getQuantity()) != 1) {
                throw new BizException(MessageConstant.OUT_OF_STOCK);
            }
        }
    }

    @Transactional
    public void release(OrderInventoryDTO request) {
        lockProducts(request);
        for (OrderInventoryLineDTO line : sortedLines(request)) {
            List<VirtualGoodItem> bound = boundStocks(line);
            if (bound.isEmpty() || bound.stream().allMatch(stock -> stock.getStatus() == 0)) continue;
            checkQuantity(line, bound);
            if (bound.stream().anyMatch(stock -> stock.getStatus() == 2)) {
                throw new BizException("已交付的库存不能作为超时订单释放");
            }
            if (bound.stream().anyMatch(stock -> stock.getStatus() != 0 && stock.getStatus() != 1)) {
                throw new BizException("订单库存状态不允许释放");
            }
            int released = virtualGoodItemMapper.releaseReserved(line.getOrderItemId(), line.getProductId());
            if (released > 0 && productMapper.addStock(line.getProductId(), released) != 1) {
                throw new BizException("恢复商品库存失败");
            }
        }
    }

    @Transactional
    public void complete(OrderInventoryDTO request) {
        lockProducts(request);
        for (OrderInventoryLineDTO line : sortedLines(request)) {
            List<VirtualGoodItem> bound = boundStocks(line);
            checkQuantity(line, bound);
            if (bound.stream().anyMatch(stock -> stock.getStatus() != 1 && stock.getStatus() != 2)) {
                throw new BizException("订单库存未预占或已经释放");
            }
            int completed = virtualGoodItemMapper.completeReserved(line.getOrderItemId(), line.getProductId());
            // 只累计本次从预占变成交付的数量，重复调用时 completed=0。
            if (completed > 0 && productMapper.addSoldCount(line.getProductId(), completed) != 1) {
                throw new BizException("更新商品销量失败");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<OrderVirtualGoodVO> content(OrderInventoryDTO request) {
        List<OrderVirtualGoodVO> result = new ArrayList<>();
        for (OrderInventoryLineDTO line : sortedLines(request)) {
            List<VirtualGoodItem> bound = boundStocks(line);
            checkQuantity(line, bound);
            if (bound.stream().anyMatch(stock -> stock.getStatus() != 1 && stock.getStatus() != 2)) {
                throw new BizException("订单库存不存在或已经释放");
            }
            for (VirtualGoodItem stock : bound) {
                result.add(new OrderVirtualGoodVO(stock.getOrderItemId(), stock.getProductId(),
                        aesgcmUtil.decrypt(stock.getContentCiphertext(), stock.getEncryptionNonce(), stock.getInventoryNo())));
            }
        }
        return result;
    }

    private List<VirtualGoodItem> boundStocks(OrderInventoryLineDTO line) {
        List<VirtualGoodItem> stocks = virtualGoodItemMapper.selectList(new LambdaQueryWrapper<VirtualGoodItem>()
                .eq(VirtualGoodItem::getOrderItemId, line.getOrderItemId()));
        if (stocks.stream().anyMatch(stock -> !line.getProductId().equals(stock.getProductId()))) {
            throw new BizException("订单明细与商品不匹配");
        }
        return stocks;
    }

    private void checkQuantity(OrderInventoryLineDTO line, List<VirtualGoodItem> stocks) {
        if (stocks.size() != line.getQuantity()) throw new BizException("订单库存数量与明细不一致");
    }

    private void lockProducts(OrderInventoryDTO request) {
        // 并发重试必须串行检查已有库存绑定；多商品按相同顺序加锁以避免互相等待。
        for (Long id : request.getItems().stream().map(OrderInventoryLineDTO::getProductId).distinct().sorted().toList()) {
            if (productMapper.lockById(id) == null) throw new BizException(MessageConstant.PRODUCT_NOT_EXIT);
        }
    }

    private List<OrderInventoryLineDTO> sortedLines(OrderInventoryDTO request) {
        return request.getItems().stream().sorted(Comparator.comparing(OrderInventoryLineDTO::getProductId)
                .thenComparing(OrderInventoryLineDTO::getOrderItemId)).toList();
    }

}
