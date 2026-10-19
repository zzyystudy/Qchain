package com.zxyy.product.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxyy.common.constant.MessageConstant;
import com.zxyy.common.context.BaseContext;
import com.zxyy.api.dto.product.CategoryDTO;
import com.zxyy.product.domain.entity.Category;
import com.zxyy.product.exception.ProductLogicStillAlive;
import com.zxyy.product.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

@Service
public class CategoryService extends ServiceImpl<CategoryMapper, Category>{
    public void saveCategory(CategoryDTO categoryDTO) {
        Category category = BeanUtil.copyProperties(categoryDTO, Category.class);
        category.setCreateUser(BaseContext.getSecurityContextHolderUserId());
        category.setUpdateUser(BaseContext.getSecurityContextHolderUserId());
        save(category);
    }

    public void delete(Long id) {
        Category category= query().eq("id", id).one();
        //判断是否已经逻辑删除
        if(category.getIsDeleted().equals(1)){
            throw new ProductLogicStillAlive(MessageConstant.CATEGORY_LOGIC_ALIVE);
        }
        removeById(category.getId());

    }
}
