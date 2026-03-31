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

import com.mipyme.stock.model.ProductCategory;
import com.mipyme.stock.repository.ProductCategoryRepository;

@RestController
@RequestMapping("/api/stock/categories")
public class ProductCategoryController {

    private final ProductCategoryRepository repository;

    public ProductCategoryController(ProductCategoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ProductCategory> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductCategory> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProductCategory entity) {
        try {
            validateAndEnrich(entity);
            return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(entity));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ProductCategory entity) {
        return repository.findById(id)
                .map(existing -> {
                    try {

                        boolean nameChanged = !existing.getCategoryName().equals(entity.getCategoryName());


                        ProductCategory newParent = entity.getParentCategory();
                        ProductCategory oldParent = existing.getParentCategory();

                        boolean parentChanged = false;
                        if (newParent == null && oldParent != null) parentChanged = true;
                        if (newParent != null && oldParent == null) parentChanged = true;
                        if (newParent != null && oldParent != null && !newParent.getCategoryId().equals(oldParent.getCategoryId())) parentChanged = true;

                        if (nameChanged || parentChanged) {


                             validateDuplicate(entity.getCategoryName(), newParent);
                        }


                        if (newParent != null && newParent.getCategoryId() != null) {
                             ProductCategory fetchedParent = repository.findById(newParent.getCategoryId())
                                .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
                             existing.setParentCategory(fetchedParent);
                        } else {
                            existing.setParentCategory(null);
                        }

                        existing.setCategoryName(entity.getCategoryName());

                        return ResponseEntity.ok(repository.save(existing));
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(e.getMessage());
                    }
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

    private void validateAndEnrich(ProductCategory entity) {

        if (entity.getParentCategory() != null && entity.getParentCategory().getCategoryId() != null) {
            ProductCategory parent = repository.findById(entity.getParentCategory().getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
            entity.setParentCategory(parent);
        } else {
            entity.setParentCategory(null);
        }


        validateDuplicate(entity.getCategoryName(), entity.getParentCategory());
    }

    private void validateDuplicate(String name, ProductCategory parent) {
        boolean exists;
        if (parent == null) {
            exists = repository.existsByCategoryNameAndParentCategoryIsNull(name);
        } else {
            exists = repository.existsByCategoryNameAndParentCategory(name, parent);
        }
        if (exists) {
            throw new IllegalArgumentException("A category with this name already exists under the same parent.");
        }
    }
}
