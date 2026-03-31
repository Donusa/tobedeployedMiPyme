package com.mipyme.stock.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mipyme.stock.model.ProductBrand;
import com.mipyme.stock.repository.ProductBrandRepository;

@RestController
@RequestMapping("/api/stock/brands")
public class ProductBrandController {

    private final ProductBrandRepository repository;

    public ProductBrandController(ProductBrandRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ProductBrand> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductBrand> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProductBrand> create(@RequestBody ProductBrand entity) {
        entity.setBrandId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(entity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductBrand> update(@PathVariable Long id, @RequestBody ProductBrand entity) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setBrandName(entity.getBrandName());
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
