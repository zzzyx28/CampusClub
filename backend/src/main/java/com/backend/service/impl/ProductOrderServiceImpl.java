package com.backend.service.impl;

import com.backend.dao.mapper.ProductOrderMapper;
import com.backend.dao.mapper.ProductSkuMapper;
import com.backend.entity.Product;
import com.backend.entity.ProductOrder;
import com.backend.entity.ProductSku;
import com.backend.entity.User;
import com.backend.service.ProductOrderService;
import com.backend.service.ProductService;
import com.backend.service.UserService;
import com.backend.vo.ProductOrderDetail;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductOrderServiceImpl extends ServiceImpl<ProductOrderMapper, ProductOrder> implements ProductOrderService {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductOrderMapper productOrderMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Override
    public List<ProductOrder> listByUserId(Long userId) {
        LambdaQueryWrapper<ProductOrder> q = new LambdaQueryWrapper<>();
        q.eq(ProductOrder::getUserId, userId)
                .orderByDesc(ProductOrder::getCreatedAt);
        return list(q);
    }

    @Override
    public List<ProductOrder> listByProductId(Long productId) {
        LambdaQueryWrapper<ProductOrder> q = new LambdaQueryWrapper<>();
        q.eq(ProductOrder::getProductId, productId)
                .orderByDesc(ProductOrder::getCreatedAt);
        return list(q);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductOrder createOrder(Long userId, Long productId, String specOption) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        Product product = productService.getById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        ProductOrder order = new ProductOrder();
        ProductSku selectedSku;
        if (specOption != null && !specOption.isBlank()) {
            selectedSku = productSkuMapper.getByProductIdAndSpec(productId, specOption);
            if (selectedSku == null) {
                throw new RuntimeException("所选规格不存在");
            }
        } else {
            selectedSku = productSkuMapper.getDefaultByProductId(productId);
            if (selectedSku == null) {
                throw new RuntimeException("该商品未配置默认规格");
            }
        }
        if (selectedSku.getStock() == null || selectedSku.getStock() <= 0) {
            throw new RuntimeException("所选规格库存不足");
        }

        order.setUserId(userId);
        order.setProductId(productId);
        order.setProductSkuId(selectedSku.getId());
        order.setQuantity(1);
        order.setUnitPrice(selectedSku.getSalePrice());
        order.setTotalPrice(selectedSku.getSalePrice());
        order.setStatus(1);

        save(order);

        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId) {
        ProductOrder order = getById(orderId);
        if (order == null) {
            return false;
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new RuntimeException("仅进行中的订单可取消");
        }

        order.setStatus(2);

        return updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeOrder(Long orderId) {
        ProductOrder order = getById(orderId);
        if (order == null) {
            return false;
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new RuntimeException("仅进行中的订单可完成");
        }
        order.setStatus(3);
        return updateById(order);
    }

    @Override
    public List<ProductOrder> listOrdersWithDetail(Long userId) {
        List<ProductOrder> orders = listByUserId(userId);

        return orders.stream().map(order -> {
            User user = userService.getById(order.getUserId());
            order.setUser(user);

            Product product = productService.getById(order.getProductId());
            order.setProduct(product);

            return order;
        }).collect(Collectors.toList());
    }

    @Override
    public ProductOrderDetail getOrderDetailById(Long id) {
        return productOrderMapper.getOrderDetailById(id);
    }

    @Override
    public List<ProductOrderDetail> getProductOrderDetailsByName(String realName) {
        User user = userService.getUserByRealName(realName);
        if (user == null) {
            return List.of();
        }

        List<ProductOrder> orders = listByUserId(user.getId());

        orders = orders.stream()
                .filter(order -> order.getStatus() != 2)
                .toList();

        return orders.stream()
                .map(order -> productOrderMapper.getOrderDetailById(order.getId()))
                .collect(Collectors.toList());
    }
}
