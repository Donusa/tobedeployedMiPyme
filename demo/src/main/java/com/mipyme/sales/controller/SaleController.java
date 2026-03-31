package com.mipyme.sales.controller;

import com.mipyme.sales.dto.CreateSaleRequest;
import com.mipyme.sales.dto.SalesMetricsResponse;
import com.mipyme.sales.model.Sale;
import com.mipyme.sales.service.SaleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    public List<Sale> getAll() {
        return saleService.getAllSales();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateSaleRequest request) {
        try {
            Sale sale = saleService.createSale(request);
            return ResponseEntity.ok(sale);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error processing sale: " + e.getMessage());
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<SalesMetricsResponse> getMetrics(
            @RequestParam(defaultValue = "7D") String range,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return ResponseEntity.ok(saleService.getSalesMetrics(range, startDate, endDate));
    }

    @PatchMapping("/{id}/facturado")
    public ResponseEntity<?> markAsFacturado(@PathVariable Long id) {
        try {
            Sale sale = saleService.markAsFacturado(id);
            return ResponseEntity.ok(sale);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error updating sale: " + e.getMessage());
        }
    }
}
