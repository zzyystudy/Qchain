package com.zxyy.api.dto.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/** 同一订单的预占、释放、交付使用相同的订单号和明细，作为业务幂等依据。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderInventoryDTO {
    @NotBlank
    private String orderNo;
    @NotNull
    private LocalDateTime expireTime;
    @NotEmpty @Valid
    private List<OrderInventoryLineDTO> items;
}
