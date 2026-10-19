package com.zxyy.order.service;

import com.zxyy.api.client.ProductClient;
import com.zxyy.api.dto.product.OrderInventoryDTO;
import com.zxyy.api.dto.product.ProductBatchQueryDTO;
import com.zxyy.api.vo.product.OrderProductVO;
import com.zxyy.api.vo.product.OrderVirtualGoodVO;
import com.zxyy.common.result.Result;
import com.zxyy.order.exception.OrderException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

/** HTTP 200 不等于业务成功，远程失败必须抛异常才能触发本地回滚和 MQ 重试。 */
@Component
@RequiredArgsConstructor
public class OrderProductGateway {
    private final ProductClient productClient;

    public List<OrderProductVO> snapshots(List<String> productNos) {
        return requireData(productClient.orderSnapshots(new ProductBatchQueryDTO(productNos)));
    }

    public void reserve(OrderInventoryDTO request) { requireSuccess(productClient.reserve(request)); }
    public void release(OrderInventoryDTO request) { requireSuccess(productClient.release(request)); }
    public void complete(OrderInventoryDTO request) { requireSuccess(productClient.complete(request)); }
    public List<OrderVirtualGoodVO> content(OrderInventoryDTO request) {
        return requireData(productClient.content(request));
    }

    private <T> T requireData(Result<T> result) {
        requireSuccess(result);
        if (result.getData() == null) {
            throw new OrderException("商品服务返回的数据为空");
        }
        return result.getData();
    }

    private void requireSuccess(Result<?> result) {
        if (result == null || !Integer.valueOf(1).equals(result.getCode())) {
            throw new OrderException(result == null ? "商品服务未返回结果" : result.getMsg());
        }
    }
}
