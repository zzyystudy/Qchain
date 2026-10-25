package com.zxyy.product.controller.internal;

import com.zxyy.api.dto.product.OrderInventoryDTO;
import com.zxyy.api.dto.product.ProductBatchQueryDTO;
import com.zxyy.api.vo.product.OrderProductVO;
import com.zxyy.api.vo.product.OrderVirtualGoodVO;
import com.zxyy.common.result.Result;
import com.zxyy.product.service.OrderInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class OrderInventoryController {
    private final OrderInventoryService inventoryService;

    @PostMapping("/products/order-snapshots")
    public Result<List<OrderProductVO>> snapshots(@Valid @RequestBody ProductBatchQueryDTO request) {
        return Result.success(inventoryService.snapshots(request.getProductNos()));
    }

    @PostMapping("/inventory/reserve")
    public Result<Void> reserve(@Valid @RequestBody OrderInventoryDTO request) {
        inventoryService.reserve(request);
        return Result.success();
    }

    @PostMapping("/inventory/release")
    public Result<Void> release(@Valid @RequestBody OrderInventoryDTO request) {
        inventoryService.release(request);
        return Result.success();
    }

    @PostMapping("/inventory/complete")
    public Result<Void> complete(@Valid @RequestBody OrderInventoryDTO request) {
        inventoryService.complete(request);
        return Result.success();
    }

    @PostMapping("/inventory/content")
    public Result<List<OrderVirtualGoodVO>> content(@Valid @RequestBody OrderInventoryDTO request) {
        return Result.success(inventoryService.content(request));
    }
}
