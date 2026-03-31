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

import com.mipyme.stock.model.MeasurementUnit;
import com.mipyme.stock.repository.MeasurementUnitRepository;

@RestController
@RequestMapping("/api/stock/units")
public class MeasurementUnitController {

    private final MeasurementUnitRepository repository;

    public MeasurementUnitController(MeasurementUnitRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<MeasurementUnit> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeasurementUnit> getById(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MeasurementUnit> create(@RequestBody MeasurementUnit entity) {
        if (repository.existsById(entity.getUnitCode())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(entity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MeasurementUnit> update(@PathVariable String id, @RequestBody MeasurementUnit entity) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setUnitName(entity.getUnitName());
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
