package com.backend.service;

import com.backend.entity.Product;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductService extends IService<Product> {

    Product getBySku(String sku);

    Product getBySpu(String spu);

    List<Product> listWithSkus();

    Product getByIdWithSkus(Long id);

    boolean saveWithSkus(Product product);

    boolean updateWithSkus(Product product);

    List<Product> searchProducts(String category, String shipFrom,
            LocalDateTime startTime, LocalDateTime endTime);

    List<Product> listByBrand(String brand);

    /**
     * 开售时间不早于当前时刻的商品，按开售时间升序，最多 limit 条。
     */
    List<Product> listRecentlyAvailable(int limit);
}
