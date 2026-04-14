package com.backend.dao.mapper;

import com.backend.entity.ProductSku;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    @Select("SELECT * FROM product_sku WHERE sku = #{sku} LIMIT 1")
    ProductSku getBySku(@Param("sku") String sku);

    @Select("SELECT * FROM product_sku WHERE product_id = #{productId} AND spec_option = #{specOption} LIMIT 1")
    ProductSku getByProductIdAndSpec(@Param("productId") Long productId, @Param("specOption") String specOption);

    @Select("SELECT * FROM product_sku WHERE product_id = #{productId} AND is_default = 1 LIMIT 1")
    ProductSku getDefaultByProductId(@Param("productId") Long productId);

    @Select("SELECT * FROM product_sku WHERE product_id = #{productId} ORDER BY is_default DESC, id ASC")
    List<ProductSku> listByProductId(@Param("productId") Long productId);

    @Delete("DELETE FROM product_sku WHERE product_id = #{productId}")
    int deleteByProductId(@Param("productId") Long productId);

    @Select("SELECT COUNT(*) FROM product_order WHERE product_sku_id = #{skuId}")
    int countOrderRefsBySkuId(@Param("skuId") Long skuId);
}
