package com.zxyy.api.dto.product;

import lombok.Data;

@Data
public class CategoryDTO {
    //主键id
    private Long id;
    //分类名称
    private String name;

    //顺序
    private Integer sort;
}
