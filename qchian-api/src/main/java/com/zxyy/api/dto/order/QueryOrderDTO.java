package com.zxyy.api.dto.order;

import lombok.Data;

@Data
public class QueryOrderDTO {
    @jakarta.validation.constraints.NotBlank
    private String orderNo;
    @jakarta.validation.constraints.NotBlank
    private String email;
    @jakarta.validation.constraints.NotBlank
    private String password;
}
