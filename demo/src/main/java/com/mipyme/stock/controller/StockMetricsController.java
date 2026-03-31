package com.mipyme.stock.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mipyme.stock.model.Product;
import com.mipyme.stock.model.ProductVariant;
import com.mipyme.stock.repository.ProductRepository;
import com.mipyme.stock.repository.ProductVariantRepository;

@RestController
@RequestMapping("/api/stock/metrics")
public class StockMetricsController {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    public StockMetricsController(ProductRepository productRepository, ProductVariantRepository productVariantRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @GetMapping("/conflicts")
    public ResponseEntity<Map<String, Object>> getConflicts() {
        Map<String, Object> response = new HashMap<>();

        List<Product> duplicateProducts = productRepository.findDuplicateInternalCodes();
        List<ProductVariant> duplicateSkus = productVariantRepository.findDuplicateSkus();
        List<ProductVariant> duplicateGtins = productVariantRepository.findDuplicateGtins();

        response.put("duplicateProducts", duplicateProducts);
        response.put("duplicateSkus", duplicateSkus);
        response.put("duplicateGtins", duplicateGtins);

        return ResponseEntity.ok(response);
    }
}
