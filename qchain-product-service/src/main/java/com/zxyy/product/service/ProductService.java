package com.zxyy.product.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxyy.common.constant.MessageConstant;
import com.zxyy.common.context.BaseContext;
import com.zxyy.api.dto.product.ProductDTO;
import com.zxyy.product.domain.entity.Category;
import com.zxyy.product.domain.entity.Product;
import com.zxyy.product.exception.CategoryException;
import com.zxyy.product.exception.ProductLogicStillAlive;
import com.zxyy.product.mapper.CategoryMapper;
import com.zxyy.product.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductService extends ServiceImpl<ProductMapper, Product>{
    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 根据商品编号彻底删除商品
     * @param productNo
     */
    public void delete(String productNo) {
        Product product = query().eq("product_no", productNo).one();
        //判断是否已经逻辑删除
        if(product.getIsDeleted().equals(1)){
            throw new ProductLogicStillAlive(MessageConstant.PRODUCT_LOGIC_ALIVE);
        }
        removeById(product.getId());
    }

    public void saveProduct(ProductDTO productDTO) {
        //判断category是否存在 这个前端校验一下按理说就可以了
        Category category = categoryMapper.selectById(productDTO.getCategoryId());
        if(category == null){
            throw new CategoryException(MessageConstant.CATEGORY_NOT_EXIT);
        }
        Product product = BeanUtil.copyProperties(productDTO, Product.class);
        product.setCreateUser(BaseContext.getSecurityContextHolderUserId());
        product.setUpdateUser(BaseContext.getSecurityContextHolderUserId());
        save(product);
    }
}
