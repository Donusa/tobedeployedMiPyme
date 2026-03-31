package com.mipyme.stock.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mipyme.stock.model.StorageLocation;
import com.mipyme.stock.repository.StorageLocationRepository;

@RestController
@RequestMapping("/api/stock/locations")
public class StorageLocationController {

    private final StorageLocationRepository repository;

    public StorageLocationController(StorageLocationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<StorageLocation> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StorageLocation> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<StorageLocation> create(@RequestBody StorageLocation entity) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(repository.save(entity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StorageLocation> update(@PathVariable Long id, @RequestBody StorageLocation entity) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setLocationCode(entity.getLocationCode());
                    existing.setLocationDescription(entity.getLocationDescription());
                    existing.setLocationType(entity.getLocationType());


                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
