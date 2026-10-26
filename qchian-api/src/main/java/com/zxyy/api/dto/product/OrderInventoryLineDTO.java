package com.zxyy.api.dto.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderInventoryLineDTO {
    @NotNull @Positive
    private Long orderItemId;
    @NotNull @Positive
    private Long productId;
    @NotNull @Positive
    private Integer quantity;
}
