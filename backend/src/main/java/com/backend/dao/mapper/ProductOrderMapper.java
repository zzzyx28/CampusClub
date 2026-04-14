package com.backend.dao.mapper;

import com.backend.entity.ProductOrder;
import com.backend.vo.ProductOrderDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductOrderMapper extends BaseMapper<ProductOrder> {
    ProductOrderDetail getOrderDetailById(Long orderId);
}
