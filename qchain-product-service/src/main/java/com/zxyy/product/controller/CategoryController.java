package com.zxyy.product.controller;

import cn.hutool.core.bean.BeanUtil;
import com.zxyy.common.result.Result;
import com.zxyy.product.domain.entity.Category;
import com.zxyy.api.vo.product.CategoryVO;
import com.zxyy.product.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("guestCategoryController")
@RequestMapping("/guest/category")
@Tag(name = "客户端分类接口")
@Slf4j
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @GetMapping
    @Operation(summary = "获取分类列表")
    public Result<List<CategoryVO>> getCategoryList(){
        log.info("获取分类列表");
        List<Category> categories = categoryService.query().eq("is_deleted",0)
                .eq("status",1).list();
        List<CategoryVO> categoryVOs = categories.stream().map(
                category -> {
                    CategoryVO categoryVO = new CategoryVO();
                    BeanUtil.copyProperties(category, categoryVO);
                    return categoryVO;
                }
        ).toList();
        return Result.success(categoryVOs);
    }
}
