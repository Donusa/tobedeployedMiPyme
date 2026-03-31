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

import com.mipyme.stock.model.StorageLocation;
import com.mipyme.stock.model.Warehouse;
import com.mipyme.stock.repository.StorageLocationRepository;
import com.mipyme.stock.repository.WarehouseRepository;

@RestController
@RequestMapping("/api/stock/warehouses")
public class WarehouseController {

    private final WarehouseRepository warehouseRepository;
    private final StorageLocationRepository storageLocationRepository;

    public WarehouseController(WarehouseRepository warehouseRepository, StorageLocationRepository storageLocationRepository) {
        this.warehouseRepository = warehouseRepository;
        this.storageLocationRepository = storageLocationRepository;
    }



    @GetMapping
    public List<Warehouse> getAll() {
        return warehouseRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Warehouse> getById(@PathVariable Long id) {
        return warehouseRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Warehouse entity) {
        if (warehouseRepository.existsByWarehouseName(entity.getWarehouseName())) {
            return ResponseEntity.badRequest().body("Warehouse name already exists");
        }
        if (entity.getWarehouseCode() != null && warehouseRepository.existsByWarehouseCode(entity.getWarehouseCode())) {
            return ResponseEntity.badRequest().body("Warehouse code already exists");
        }
        entity.setWarehouseId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseRepository.save(entity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Warehouse entity) {
        return warehouseRepository.findById(id)
                .map(existing -> {
                    if (!existing.getWarehouseName().equals(entity.getWarehouseName()) && warehouseRepository.existsByWarehouseName(entity.getWarehouseName())) {
                        return ResponseEntity.badRequest().body("Warehouse name already exists");
                    }
                    if (entity.getWarehouseCode() != null && !entity.getWarehouseCode().equals(existing.getWarehouseCode()) && warehouseRepository.existsByWarehouseCode(entity.getWarehouseCode())) {
                        return ResponseEntity.badRequest().body("Warehouse code already exists");
                    }

                    existing.setWarehouseName(entity.getWarehouseName());
                    existing.setWarehouseCode(entity.getWarehouseCode());
                    existing.setAddress(entity.getAddress());
                    return ResponseEntity.ok(warehouseRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (warehouseRepository.existsById(id)) {
            warehouseRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }



    @PostMapping("/{warehouseId}/locations")
    public ResponseEntity<StorageLocation> createLocation(@PathVariable Long warehouseId, @RequestBody StorageLocation entity) {
        return warehouseRepository.findById(warehouseId)
                .map(warehouse -> {
                    entity.setStorageLocationId(null);
                    entity.setWarehouse(warehouse);
                    return ResponseEntity.status(HttpStatus.CREATED).body(storageLocationRepository.save(entity));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{warehouseId}/locations")
    public ResponseEntity<List<StorageLocation>> getLocations(@PathVariable Long warehouseId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            return ResponseEntity.notFound().build();
        }
        List<StorageLocation> locations = storageLocationRepository.findAll().stream()
                .filter(l -> l.getWarehouse().getWarehouseId().equals(warehouseId))
                .toList();
        return ResponseEntity.ok(locations);
    }
}
