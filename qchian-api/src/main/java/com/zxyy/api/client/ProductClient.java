package com.zxyy.api.client;

import com.zxyy.api.config.InternalFeignConfiguration;
import com.zxyy.api.dto.product.OrderInventoryDTO;
import com.zxyy.api.dto.product.ProductBatchQueryDTO;
import com.zxyy.api.vo.product.OrderProductVO;
import com.zxyy.api.vo.product.OrderVirtualGoodVO;
import com.zxyy.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

@FeignClient(name = "product-service", url = "${qchain.clients.product-url:}", contextId = "productClient", configuration = InternalFeignConfiguration.class)
public interface ProductClient {
    @PostMapping("/internal/products/order-snapshots")
    Result<List<OrderProductVO>> orderSnapshots(@RequestBody ProductBatchQueryDTO request);

    @PostMapping("/internal/inventory/reserve")
    Result<Void> reserve(@RequestBody OrderInventoryDTO request);

    @PostMapping("/internal/inventory/release")
    Result<Void> release(@RequestBody OrderInventoryDTO request);

    @PostMapping("/internal/inventory/complete")
    Result<Void> complete(@RequestBody OrderInventoryDTO request);

    @PostMapping("/internal/inventory/content")
    Result<List<OrderVirtualGoodVO>> content(@RequestBody OrderInventoryDTO request);
}
