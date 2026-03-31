package com.mipyme.stock.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mipyme.stock.model.Product;
import com.mipyme.stock.model.ProductVariant;
import com.mipyme.stock.repository.ProductRepository;
import com.mipyme.stock.repository.ProductVariantRepository;
import com.mipyme.sales.repository.SaleItemRepository;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/stock/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final SaleItemRepository saleItemRepository;

    public ProductController(ProductRepository productRepository, ProductVariantRepository productVariantRepository, SaleItemRepository saleItemRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.saleItemRepository = saleItemRepository;
    }

    @GetMapping
    public List<Product> getAll() {
        return productRepository.findAllWithVariants();
    }

    @GetMapping("/scan")
    public ResponseEntity<java.util.Map<String, Object>> scan(@org.springframework.web.bind.annotation.RequestParam String code) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();


        List<Product> products = productRepository.findByInternalCode(code);
        if (products.size() > 1) {
             response.put("message", "Error: Se encontraron múltiples productos con el mismo código interno. Conflicto de métricas.");
             response.put("code", "DUPLICATE_PRODUCT");
             return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        if (!products.isEmpty()) {
            Product p = products.get(0);
            response.put("type", "PRODUCT");
            response.put("product", p);
            response.put("hasVariants", productVariantRepository.countByProductProductId(p.getProductId()) > 0);
            return ResponseEntity.ok(response);
        }


        List<ProductVariant> variants = productVariantRepository.findBySkuOrGtin(code);
        if (variants.size() > 1) {
             response.put("message", "Error: Se encontraron múltiples variantes con el mismo SKU/GTIN. Conflicto de métricas.");
             response.put("code", "DUPLICATE_VARIANT");
             return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        if (!variants.isEmpty()) {
            ProductVariant v = variants.get(0);
            response.put("type", "VARIANT");
            response.put("product", v.getProduct());
            response.put("variant", v);
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return productRepository.findByIdWithVariants(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product entity) {
        entity.setProductId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(productRepository.save(entity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody Product entity) {
        return productRepository.findByIdWithVariants(id)
                .map(existing -> {
                    existing.setInternalCode(entity.getInternalCode());
                    existing.setProductName(entity.getProductName());
                    existing.setMeasurementUnit(entity.getMeasurementUnit());
                    existing.setMeasurementValue(entity.getMeasurementValue());
                    existing.setProductCategory(entity.getProductCategory());
                    existing.setProductBrand(entity.getProductBrand());
                    existing.setWarehouse(entity.getWarehouse());
                    existing.setStorage(entity.getStorage());
                    existing.setPrice(entity.getPrice());
                    existing.setCost(entity.getCost());
                    long variantCount = productVariantRepository.countByProductProductId(existing.getProductId());
                    if (variantCount > 0) {
                        java.math.BigDecimal sum = productVariantRepository.sumStockByProductId(existing.getProductId());
                        existing.setStockQuantity(sum == null ? java.math.BigDecimal.ZERO : sum);
                    } else {
                        existing.setStockQuantity(entity.getStockQuantity());
                    }
                    existing.setMinStock(entity.getMinStock());
                    existing.setIsActive(entity.getIsActive());
                    return ResponseEntity.ok(productRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Product> updateStock(@PathVariable Long id, @RequestBody java.util.Map<String, java.math.BigDecimal> payload) {
        java.math.BigDecimal quantity = payload.get("stockQuantity");
        if (quantity == null) {
            return ResponseEntity.badRequest().build();
        }

        return productRepository.findByIdWithVariants(id)
                .map(existing -> {



                    existing.setStockQuantity(quantity);
                    return ResponseEntity.ok(productRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return productRepository.findByIdWithVariants(id).map(existing -> {
            boolean isUsed = saleItemRepository.existsByProductId(id);

            if (isUsed) {
                existing.setIsActive(false);
                existing.setStockQuantity(BigDecimal.ZERO);
                if (existing.getProductVariants() != null) {
                    existing.getProductVariants().forEach(v -> {
                        v.setIsActive(false);
                        v.setStockQuantity(BigDecimal.ZERO);
                    });
                }
                productRepository.save(existing);
            } else {
                productRepository.delete(existing);
            }
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }



    @PostMapping("/{productId}/variants")
    public ResponseEntity<ProductVariant> createVariant(@PathVariable Long productId, @RequestBody ProductVariant entity) {
        return productRepository.findById(productId)
                .map(product -> {
                    entity.setProductVariantId(null);
                    entity.setProduct(product);

                    if (entity.getAttributes() != null) {
                        entity.getAttributes().forEach(attr -> attr.setProductVariant(entity));
                    }
                    ProductVariant saved = productVariantRepository.save(entity);
                    java.math.BigDecimal sum = productVariantRepository.sumStockByProductId(product.getProductId());
                    product.setStockQuantity(sum == null ? java.math.BigDecimal.ZERO : sum);
                    productRepository.save(product);

                    return productVariantRepository.findWithAttributesById(saved.getProductVariantId())
                            .map(v -> ResponseEntity.status(HttpStatus.CREATED).body(v))
                            .orElse(ResponseEntity.status(HttpStatus.CREATED).body(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{productId}/variants")
    public ResponseEntity<List<ProductVariant>> getVariants(@PathVariable Long productId) {
        if (!productRepository.existsById(productId)) {
            return ResponseEntity.notFound().build();
        }
        List<ProductVariant> variants = productVariantRepository.findAllByProductIdEager(productId);
        return ResponseEntity.ok(variants);
    }
}
