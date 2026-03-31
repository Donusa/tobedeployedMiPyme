package com.mipyme.caja.controller;

import com.mipyme.caja.dto.*;
import com.mipyme.caja.model.CajaDiaria;
import com.mipyme.caja.model.CajaEgresoCategoria;
import com.mipyme.caja.model.CajaMovimiento;
import com.mipyme.caja.service.CajaAuditService;
import com.mipyme.caja.service.CajaDiariaService;
import com.mipyme.caja.service.CajaMovimientoService;
import com.mipyme.sales.model.Sale;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cajas")
public class CajaDiariaController {

    private final CajaDiariaService cajaService;
    private final CajaMovimientoService movimientoService;
    private final CajaAuditService auditService;

    public CajaDiariaController(CajaDiariaService cajaService,
            CajaMovimientoService movimientoService,
            CajaAuditService auditService) {
        this.cajaService = cajaService;
        this.movimientoService = movimientoService;
        this.auditService = auditService;
    }



    @PostMapping("/abrir")
    public ResponseEntity<?> abrirCaja(@RequestBody AbrirCajaRequest request) {
        try {
            CajaDiaria caja = cajaService.abrirCaja(request);
            return ResponseEntity.ok(cajaService.toResponse(caja));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al abrir caja: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/cerrar")
    public ResponseEntity<?> cerrarCaja(@PathVariable Long id, @RequestBody CerrarCajaRequest request) {
        try {
            CajaDiaria caja = cajaService.cerrarCaja(id, request);
            return ResponseEntity.ok(cajaService.toResponse(caja));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al cerrar caja: " + e.getMessage());
        }
    }



    @GetMapping("/actual")
    public ResponseEntity<?> obtenerCajaActual(@RequestParam Long sucursalId) {
        try {
            return cajaService.obtenerCajaActual(sucursalId)
                    .map(caja -> ResponseEntity.ok((Object) cajaService.toResponse(caja)))
                    .orElse(ResponseEntity.noContent().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al consultar caja: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            CajaDiaria caja = cajaService.obtenerPorId(id);
            return ResponseEntity.ok(cajaService.toResponse(caja));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al consultar caja: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/resumen")
    public ResponseEntity<?> obtenerResumen(@PathVariable Long id) {
        try {
            CajaResumenResponse resumen = cajaService.obtenerResumen(id);
            return ResponseEntity.ok(resumen);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al obtener resumen: " + e.getMessage());
        }
    }

    @GetMapping("/historial")
    public ResponseEntity<?> obtenerHistorial(
            @RequestParam Long sucursalId,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta) {
        try {
            List<CajaDiaria> cajas = cajaService.obtenerHistorial(sucursalId, desde, hasta);
            List<CajaDiariaResponse> responses = cajas.stream()
                    .map(cajaService::toResponse)
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al obtener historial: " + e.getMessage());
        }
    }



    @GetMapping("/{id}/movimientos")
    public ResponseEntity<?> obtenerMovimientos(@PathVariable Long id) {
        try {
            List<CajaMovimiento> movimientos = movimientoService.obtenerMovimientos(id);
            List<CajaMovimientoResponse> responses = movimientos.stream()
                    .map(cajaService::toMovimientoResponse)
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al obtener movimientos: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/egresos")
    public ResponseEntity<?> registrarEgreso(@PathVariable Long id, @RequestBody RegistrarEgresoRequest request) {
        try {
            CajaMovimiento mov = movimientoService.registrarEgreso(id, request);
            return ResponseEntity.ok(cajaService.toMovimientoResponse(mov));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al registrar egreso: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/ingresos")
    public ResponseEntity<?> registrarIngresoManual(@PathVariable Long id, @RequestBody RegistrarIngresoManualRequest request) {
        try {
            CajaMovimiento mov = movimientoService.registrarIngresoManual(id, request);
            return ResponseEntity.ok(cajaService.toMovimientoResponse(mov));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al registrar ingreso: " + e.getMessage());
        }
    }

    @PostMapping("/{cajaId}/movimientos/{movId}/anular")
    public ResponseEntity<?> anularMovimiento(@PathVariable Long cajaId, @PathVariable Long movId,
            @RequestBody AnularMovimientoRequest request) {
        try {
            CajaMovimiento mov = movimientoService.anularMovimiento(cajaId, movId, request);
            return ResponseEntity.ok(cajaService.toMovimientoResponse(mov));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al anular movimiento: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/ventas")
    public ResponseEntity<?> obtenerVentas(@PathVariable Long id) {
        try {
            List<Sale> ventas = cajaService.obtenerVentasDeCaja(id);
            return ResponseEntity.ok(ventas);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al obtener ventas: " + e.getMessage());
        }
    }



    @GetMapping("/categorias-egreso")
    public ResponseEntity<?> obtenerCategoriasEgreso() {
        try {
            List<CajaEgresoCategoria> categorias = cajaService.obtenerCategoriasEgresoActivas();
            return ResponseEntity.ok(categorias);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al obtener categorías: " + e.getMessage());
        }
    }



    @GetMapping("/{id}/auditoria")
    public ResponseEntity<?> obtenerAuditoria(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(auditService.obtenerPorCaja(id));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al obtener auditoría: " + e.getMessage());
        }
    }
}
