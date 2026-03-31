package com.mipyme.stock.controller;

import java.math.BigDecimal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mipyme.stock.model.ProductVariant;
import com.mipyme.stock.model.Product;
import com.mipyme.stock.repository.ProductVariantRepository;
import com.mipyme.stock.repository.ProductRepository;
import com.mipyme.sales.repository.SaleItemRepository;

@RestController
@RequestMapping("/api/stock/variants")
public class ProductVariantController {

    private final ProductVariantRepository repository;
    private final ProductRepository productRepository;
    private final SaleItemRepository saleItemRepository;

    public ProductVariantController(ProductVariantRepository repository, ProductRepository productRepository, SaleItemRepository saleItemRepository) {
        this.repository = repository;
        this.productRepository = productRepository;
        this.saleItemRepository = saleItemRepository;
    }

    @GetMapping
    public java.util.List<ProductVariant> getAll() {
        return repository.findAllEager();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductVariant> getById(@PathVariable Long id) {
        return repository.findWithAttributesById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductVariant> update(@PathVariable Long id, @RequestBody ProductVariant entity) {
        return repository.findWithAttributesById(id)
                .map(existing -> {
                    existing.setVariantSku(entity.getVariantSku());
                    existing.setVariantGtin(entity.getVariantGtin());
                    existing.setStockQuantity(entity.getStockQuantity());
                    existing.setMinStock(entity.getMinStock());


                    if (existing.getAttributes() != null) {
                        existing.getAttributes().clear();
                    }
                    if (entity.getAttributes() != null) {
                        entity.getAttributes().forEach(attr -> {
                            attr.setProductVariant(existing);
                        });
                        if (existing.getAttributes() == null) {
                            existing.setAttributes(entity.getAttributes());
                        } else {
                            existing.getAttributes().addAll(entity.getAttributes());
                        }
                    }

                    existing.setNetWeightGrams(entity.getNetWeightGrams());
                    existing.setSizeDimensionsCm(entity.getSizeDimensionsCm());
                    existing.setPrice(entity.getPrice());
                    existing.setCost(entity.getCost());
                    existing.setIsActive(entity.getIsActive());
                    ProductVariant saved = repository.save(existing);
                    Product product = saved.getProduct();
                    if (product != null && product.getProductId() != null) {
                        BigDecimal sum = repository.sumStockByProductId(product.getProductId());
                        product.setStockQuantity(sum == null ? BigDecimal.ZERO : sum);
                        productRepository.save(product);
                    }
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductVariant> updateStock(@PathVariable Long id, @RequestBody java.util.Map<String, BigDecimal> payload) {
        BigDecimal quantity = payload.get("stockQuantity");
        if (quantity == null) {
            return ResponseEntity.badRequest().build();
        }

        return repository.findById(id)
                .map(existing -> {
                    existing.setStockQuantity(quantity);
                    ProductVariant saved = repository.save(existing);


                    Product product = saved.getProduct();
                    if (product != null && product.getProductId() != null) {
                        BigDecimal sum = repository.sumStockByProductId(product.getProductId());
                        product.setStockQuantity(sum == null ? BigDecimal.ZERO : sum);
                        productRepository.save(product);
                    }

                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return repository.findById(id).map(existing -> {
            Long productId = existing.getProduct() != null ? existing.getProduct().getProductId() : null;

            if (saleItemRepository.existsByProductVariantId(id)) {
                existing.setIsActive(false);
                existing.setStockQuantity(BigDecimal.ZERO);
                repository.save(existing);
            } else {
                repository.delete(existing);
            }

            if (productId != null) {
                Product product = productRepository.findById(productId).orElse(null);
                if (product != null) {
                    BigDecimal sum = repository.sumStockByProductId(productId);
                    product.setStockQuantity(sum == null ? BigDecimal.ZERO : sum);
                    productRepository.save(product);
                }
            }
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
