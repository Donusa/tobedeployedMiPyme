package com.mipyme.stock.controller;

import com.mipyme.stock.dto.GateCheckResult;
import com.mipyme.stock.dto.OfferActionResult;
import com.mipyme.stock.model.Channel;
import com.mipyme.stock.model.Offer;
import com.mipyme.stock.service.OfferOrchestratorService;
import com.mipyme.stock.service.OfferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/stock/offers")
public class OfferController {

    private final OfferService offerService;
    private final OfferOrchestratorService orchestratorService;

    public OfferController(OfferService offerService, OfferOrchestratorService orchestratorService) {
        this.offerService = offerService;
        this.orchestratorService = orchestratorService;
    }

    @GetMapping
    public ResponseEntity<List<Offer>> getAll() {
        return ResponseEntity.ok(offerService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Offer> getById(@PathVariable Long id) {
        Offer offer = offerService.getById(id);
        return offer != null ? ResponseEntity.ok(offer) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Offer> create(@RequestBody Offer offer) {
        return ResponseEntity.status(HttpStatus.CREATED).body(offerService.create(offer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Offer> update(@PathVariable Long id, @RequestBody Offer offer) {
        Offer updated = offerService.update(id, offer);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orchestratorService.deleteWithCleanup(id);
        return ResponseEntity.noContent().build();
    }



    @PostMapping("/{id}/publish/tiendanube")
    public ResponseEntity<OfferActionResult> publishTiendaNube(@PathVariable Long id) {
        OfferActionResult result = orchestratorService.applyToChannel(id, Channel.TN);
        return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/{id}/unpublish/tiendanube")
    public ResponseEntity<OfferActionResult> unpublishTiendaNube(@PathVariable Long id) {
        OfferActionResult result = orchestratorService.removeFromChannel(id, Channel.TN);
        return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/{id}/publish/mercadolibre")
    public ResponseEntity<OfferActionResult> publishMercadoLibre(@PathVariable Long id) {
        OfferActionResult result = orchestratorService.applyToChannel(id, Channel.ML);
        return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/{id}/unpublish/mercadolibre")
    public ResponseEntity<OfferActionResult> unpublishMercadoLibre(@PathVariable Long id) {
        OfferActionResult result = orchestratorService.removeFromChannel(id, Channel.ML);
        return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/{id}/sync/tiendanube")
    public ResponseEntity<OfferActionResult> syncTiendaNube(@PathVariable Long id) {
        OfferActionResult result = orchestratorService.syncChannel(id, Channel.TN);
        return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/{id}/sync/mercadolibre")
    public ResponseEntity<OfferActionResult> syncMercadoLibre(@PathVariable Long id) {
        OfferActionResult result = orchestratorService.syncChannel(id, Channel.ML);
        return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/{id}/gate/{channel}")
    public ResponseEntity<GateCheckResult> checkGate(@PathVariable Long id, @PathVariable String channel) {
        Channel ch;
        try {
            ch = Channel.valueOf(channel.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(GateCheckResult.block("Canal inválido: " + channel));
        }
        GateCheckResult result = orchestratorService.checkGate(id, ch);
        return ResponseEntity.ok(result);
    }
}
