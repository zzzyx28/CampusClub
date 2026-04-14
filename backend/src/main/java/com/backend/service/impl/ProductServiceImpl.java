package com.backend.service.impl;

import com.backend.dao.mapper.ProductMapper;
import com.backend.dao.mapper.ProductSkuMapper;
import com.backend.entity.Product;
import com.backend.entity.ProductSku;
import com.backend.service.ProductService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Override
    public Product getBySku(String sku) {
        ProductSku skuEntity = productSkuMapper.getBySku(sku);
        if (skuEntity == null) {
            return null;
        }
        return getById(skuEntity.getProductId());
    }

    @Override
    public Product getBySpu(String spu) {
        LambdaQueryWrapper<Product> q = new LambdaQueryWrapper<>();
        q.eq(Product::getSpu, spu);
        return getOne(q);
    }

    @Override
    public List<Product> listWithSkus() {
        List<Product> products = list();
        for (Product product : products) {
            product.setSkuList(productSkuMapper.listByProductId(product.getId()));
        }
        return products;
    }

    @Override
    public Product getByIdWithSkus(Long id) {
        Product product = getById(id);
        if (product == null) {
            return null;
        }
        product.setSkuList(productSkuMapper.listByProductId(id));
        return product;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveWithSkus(Product product) {
        List<ProductSku> skuList = product.getSkuList() == null ? List.of() : product.getSkuList();
        prepareProductAndSkus(product, skuList, true);
        validateSkuList(skuList);

        boolean ok = save(product);
        if (!ok) {
            return false;
        }
        for (ProductSku sku : skuList) {
            sku.setId(null);
            sku.setProductId(product.getId());
            productSkuMapper.insert(sku);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWithSkus(Product product) {
        List<ProductSku> skuList = product.getSkuList() == null ? List.of() : product.getSkuList();
        prepareProductAndSkus(product, skuList, false);
        validateSkuList(skuList);

        boolean ok = updateById(product);
        if (!ok) {
            return false;
        }

        List<ProductSku> existingSkuList = productSkuMapper.listByProductId(product.getId());
        Set<Long> submittedIds = new HashSet<>();

        for (ProductSku sku : skuList) {
            sku.setProductId(product.getId());
            if (sku.getId() != null) {
                submittedIds.add(sku.getId());
                productSkuMapper.updateById(sku);
            } else {
                productSkuMapper.insert(sku);
                if (sku.getId() != null) {
                    submittedIds.add(sku.getId());
                }
            }
        }

        for (ProductSku existingSku : existingSkuList) {
            if (submittedIds.contains(existingSku.getId())) {
                continue;
            }
            int refs = productSkuMapper.countOrderRefsBySkuId(existingSku.getId());
            if (refs == 0) {
                productSkuMapper.deleteById(existingSku.getId());
            }
        }
        return true;
    }

    private void validateSkuList(List<ProductSku> skuList) {
        if (skuList.isEmpty()) {
            throw new RuntimeException("至少需要一个商品规格");
        }
        int defaultCount = 0;
        List<String> skuCodes = new ArrayList<>();
        for (ProductSku sku : skuList) {
            if (sku.getSku() == null || sku.getSku().isBlank()) {
                throw new RuntimeException("规格 SKU 不能为空");
            }
            if (sku.getSpecOption() == null || sku.getSpecOption().isBlank()) {
                throw new RuntimeException("规格说明不能为空");
            }
            if (sku.getSalePrice() == null) {
                throw new RuntimeException("销售价不能为空");
            }
            if (sku.getStock() == null || sku.getStock() < 0) {
                throw new RuntimeException("库存不能小于 0");
            }
            if (skuCodes.contains(sku.getSku())) {
                throw new RuntimeException("同一商品下规格 SKU 不能重复");
            }
            skuCodes.add(sku.getSku());
            if (sku.getIsDefault() != null && sku.getIsDefault() == 1) {
                defaultCount++;
            }
        }
        if (defaultCount != 1) {
            throw new RuntimeException("必须且仅能设置一个默认规格");
        }
    }

    private void prepareProductAndSkus(Product product, List<ProductSku> skuList, boolean isCreate) {
        if (isCreate || product.getSpu() == null || product.getSpu().isBlank()) {
            product.setSpu(generateCode("SPU"));
        }
        for (ProductSku sku : skuList) {
            if (sku.getSku() == null || sku.getSku().isBlank()) {
                sku.setSku(generateCode("SKU"));
            }
        }
    }

    private String generateCode(String prefix) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return prefix + "-" + token;
    }

    @Override
    public List<Product> searchProducts(String category, String shipFrom,
            LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<Product> q = new LambdaQueryWrapper<>();
        q.eq(category != null && !category.isBlank(), Product::getCategory, category)
                .and(shipFrom != null && !shipFrom.isBlank(),
                        wrapper -> wrapper.like(Product::getName, shipFrom).or().like(Product::getBrand, shipFrom))
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getCreatedAt);
        return list(q);
    }

    @Override
    public List<Product> listByBrand(String brand) {
        LambdaQueryWrapper<Product> q = new LambdaQueryWrapper<>();
        q.eq(Product::getBrand, brand)
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getCreatedAt);
        return list(q);
    }

    @Override
    public List<Product> listRecentlyAvailable(int limit) {
        int n = Math.min(Math.max(limit, 1), 30);
        LambdaQueryWrapper<Product> q = new LambdaQueryWrapper<>();
        q.eq(Product::getStatus, 1)
                .orderByDesc(Product::getCreatedAt)
                .last("LIMIT " + n);
        return list(q);
    }
}
