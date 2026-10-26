package com.zxyy.api.dto.product;

import lombok.Data;

@Data
public class VirtualGoodItemPageQueryDTO {
    private int page;

    private int pageSize;

    private String productNo;
    //状态 0表示可用 1表示占用 2标识交付 3标识作废
    private Integer status;

}
