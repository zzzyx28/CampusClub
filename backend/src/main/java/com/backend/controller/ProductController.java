package com.backend.controller;

import com.backend.common.Result;
import com.backend.common.enumeration.ResultCode;
import com.backend.entity.Product;
import com.backend.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品：查询与后台维护
 */
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/list")
    public Result listProducts() {
        List<Product> products = productService.listWithSkus();
        return Result.success(products);
    }

    @GetMapping("/{id}")
    public Result getProductById(@PathVariable Long id) {
        Product product = productService.getByIdWithSkus(id);
        if (product == null) {
            return Result.error(ResultCode.NOT_FOUND, "商品不存在");
        }
        return Result.success(product);
    }

    @GetMapping("/sku/{sku}")
    public Result getProductBySku(@PathVariable String sku) {
        Product product = productService.getBySku(sku);
        if (product == null) {
            return Result.error(ResultCode.NOT_FOUND, "商品不存在");
        }
        return Result.success(product);
    }

    @GetMapping("/search")
    public Result searchProducts(
            @RequestParam String category,
            @RequestParam String shipFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        List<Product> products = productService.searchProducts(category, shipFrom, startTime, endTime);
        return Result.success(products);
    }

    @GetMapping("/brand/{brand}")
    public Result listByBrand(@PathVariable String brand) {
        List<Product> products = productService.listByBrand(brand);
        return Result.success(products);
    }

    @PostMapping
    public Result addProduct(@RequestBody Product product) {
        if (product.getSpu() != null && !product.getSpu().isBlank()) {
            Product existing = productService.getBySpu(product.getSpu());
            if (existing != null) {
                return Result.error(ResultCode.PARAM_ERROR, "SPU 已存在");
            }
        }

        boolean success = productService.saveWithSkus(product);
        if (success) {
            return Result.success("添加商品成功", product);
        }
        return Result.error("添加商品失败");
    }

    @PutMapping("/{id}")
    public Result updateProduct(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        Product existingProduct = productService.getById(id);
        if (existingProduct == null) {
            return Result.error(ResultCode.NOT_FOUND, "商品不存在");
        }

        if (product.getSpu() != null && !product.getSpu().isBlank() && !product.getSpu().equals(existingProduct.getSpu())) {
            Product sameSpu = productService.getBySpu(product.getSpu());
            if (sameSpu != null) {
                return Result.error(ResultCode.PARAM_ERROR, "SPU 已被其他商品使用");
            }
        }

        boolean success = productService.updateWithSkus(product);
        if (success) {
            return Result.success("更新商品成功", product);
        }
        return Result.error("更新商品失败");
    }

    @DeleteMapping("/{id}")
    public Result deleteProduct(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            return Result.error(ResultCode.NOT_FOUND, "商品不存在");
        }

        boolean success = productService.removeById(id);
        if (success) {
            return Result.success("删除商品成功", null);
        }
        return Result.error("删除商品失败");
    }
}
