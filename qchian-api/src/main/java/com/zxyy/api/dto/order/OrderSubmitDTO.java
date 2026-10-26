package com.zxyy.api.dto.order;

import lombok.Data;

import java.util.List;

@Data
public class OrderSubmitDTO {
    //客户下单幂等号 防止重复下单
    @jakarta.validation.constraints.NotBlank
    private String clientRequestNo;
    //用户备注
    private String buyerRemark;
    //用户邮箱
    @jakarta.validation.constraints.NotBlank
    private String email;
    //用户订单查询密码
    @jakarta.validation.constraints.NotBlank
    private String queryPassword;
    //选择下单的商品
    @jakarta.validation.constraints.NotEmpty
    @jakarta.validation.Valid
    private List<BuyProductDTO> items;
}
