package com.mipyme.caja.service;

import com.mipyme.caja.dto.AnularMovimientoRequest;
import com.mipyme.caja.dto.RegistrarEgresoRequest;
import com.mipyme.caja.dto.RegistrarIngresoManualRequest;
import com.mipyme.caja.model.*;
import com.mipyme.caja.repository.CajaDiariaRepository;
import com.mipyme.caja.repository.CajaEgresoCategoriaRepository;
import com.mipyme.caja.repository.CajaMovimientoRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class CajaMovimientoService {

    private final CajaDiariaRepository cajaRepository;
    private final CajaMovimientoRepository movimientoRepository;
    private final CajaEgresoCategoriaRepository categoriaRepository;
    private final CajaAuditService auditService;

    private static final Set<CajaMovimientoTipo> TIPOS_EGRESO = Set.of(
            CajaMovimientoTipo.GASTO_OPERATIVO,
            CajaMovimientoTipo.PAGO_PROVEEDOR,
            CajaMovimientoTipo.RETIRO_EFECTIVO);

    private static final Set<CajaMovimientoTipo> TIPOS_INGRESO_MANUAL = Set.of(
            CajaMovimientoTipo.INGRESO_MANUAL,
            CajaMovimientoTipo.REPOSICION_CAMBIO);

    public CajaMovimientoService(CajaDiariaRepository cajaRepository,
            CajaMovimientoRepository movimientoRepository,
            CajaEgresoCategoriaRepository categoriaRepository,
            CajaAuditService auditService) {
        this.cajaRepository = cajaRepository;
        this.movimientoRepository = movimientoRepository;
        this.categoriaRepository = categoriaRepository;
        this.auditService = auditService;
    }



    @Transactional
    public CajaMovimiento registrarEgreso(Long cajaId, RegistrarEgresoRequest request) {
        String username = getCurrentUsername();
        CajaDiaria caja = obtenerCajaAbierta(cajaId);


        if (request.getMonto() == null || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        if (request.getMedioPago() == null) {
            throw new IllegalArgumentException("El medio de pago es obligatorio");
        }
        if (request.getJustificacion() == null || request.getJustificacion().isBlank()) {
            throw new IllegalArgumentException("La justificación es obligatoria para egresos");
        }
        if (request.getComprobanteTexto() == null || request.getComprobanteTexto().isBlank()) {
            throw new IllegalArgumentException("La descripción del comprobante es obligatoria para egresos");
        }

        CajaMovimientoTipo tipo = CajaMovimientoTipo.GASTO_OPERATIVO;
        if (request.getTipo() != null) {
            try {
                tipo = CajaMovimientoTipo.valueOf(request.getTipo());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Tipo de movimiento inválido: " + request.getTipo());
            }
            if (!TIPOS_EGRESO.contains(tipo)) {
                throw new IllegalArgumentException("El tipo " + tipo + " no es un tipo de egreso válido");
            }
        }


        if (request.getCategoriaId() != null) {
            if (!categoriaRepository.existsById(request.getCategoriaId())) {
                throw new IllegalArgumentException("Categoría de egreso no encontrada: " + request.getCategoriaId());
            }
        }

        MedioPago medioPago = parseMedioPago(request.getMedioPago());

        CajaMovimiento mov = new CajaMovimiento();
        mov.setTipo(tipo);
        mov.setMedioPago(medioPago);
        mov.setMonto(request.getMonto());
        mov.setDescripcion("Egreso: " + request.getJustificacion());
        mov.setJustificacion(request.getJustificacion());
        mov.setComprobanteTexto(request.getComprobanteTexto());
        mov.setComprobanteNumero(request.getComprobanteNumero());
        mov.setCategoriaEgresoId(request.getCategoriaId());
        mov.setUsuarioCreador(username);
        mov.setFechaHoraCreacion(LocalDateTime.now());
        mov.setObservaciones(request.getObservacion());
        caja.addMovimiento(mov);

        cajaRepository.save(caja);

        auditService.registrar(cajaId, "CajaMovimiento", mov.getMovimientoId(),
                "EGRESO_REGISTRADO", username, null, null,
                "Egreso de $" + request.getMonto() + " via " + request.getMedioPago()
                        + " — " + request.getJustificacion());

        return mov;
    }



    @Transactional
    public CajaMovimiento registrarIngresoManual(Long cajaId, RegistrarIngresoManualRequest request) {
        String username = getCurrentUsername();
        CajaDiaria caja = obtenerCajaAbierta(cajaId);

        if (request.getMonto() == null || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        if (request.getMedioPago() == null) {
            throw new IllegalArgumentException("El medio de pago es obligatorio");
        }
        if (request.getDescripcion() == null || request.getDescripcion().isBlank()) {
            throw new IllegalArgumentException("La descripción es obligatoria");
        }

        CajaMovimientoTipo tipo = CajaMovimientoTipo.INGRESO_MANUAL;
        if (request.getTipo() != null && !request.getTipo().isBlank()) {
            try {
                tipo = CajaMovimientoTipo.valueOf(request.getTipo());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Tipo de movimiento inválido: " + request.getTipo());
            }
            if (!TIPOS_INGRESO_MANUAL.contains(tipo)) {
                throw new IllegalArgumentException("El tipo " + tipo + " no es un tipo de ingreso manual válido");
            }
        }

        MedioPago medioPago = parseMedioPago(request.getMedioPago());

        CajaMovimiento mov = new CajaMovimiento();
        mov.setTipo(tipo);
        mov.setMedioPago(medioPago);
        mov.setMonto(request.getMonto());
        mov.setDescripcion(request.getDescripcion());
        mov.setUsuarioCreador(username);
        mov.setFechaHoraCreacion(LocalDateTime.now());
        caja.addMovimiento(mov);

        cajaRepository.save(caja);

        auditService.registrar(cajaId, "CajaMovimiento", mov.getMovimientoId(),
                "INGRESO_REGISTRADO", username, null, null,
                "Ingreso manual de $" + request.getMonto() + " via " + request.getMedioPago()
                        + " — " + request.getDescripcion());

        return mov;
    }



    @Transactional
    public CajaMovimiento anularMovimiento(Long cajaId, Long movimientoId, AnularMovimientoRequest request) {
        String username = getCurrentUsername();
        CajaDiaria caja = obtenerCajaAbierta(cajaId);

        CajaMovimiento mov = movimientoRepository.findById(movimientoId)
                .orElseThrow(() -> new IllegalArgumentException("Movimiento no encontrado: " + movimientoId));

        if (!mov.getCajaDiaria().getCajaDiariaId().equals(cajaId)) {
            throw new IllegalArgumentException("El movimiento no pertenece a esta caja");
        }
        if (mov.getEstado() != CajaMovimientoEstado.ACTIVO) {
            throw new IllegalStateException("El movimiento ya está " + mov.getEstado());
        }
        if (mov.getTipo() == CajaMovimientoTipo.APERTURA || mov.getTipo() == CajaMovimientoTipo.CIERRE) {
            throw new IllegalStateException("No se pueden anular movimientos de apertura o cierre");
        }
        if (request.getMotivo() == null || request.getMotivo().isBlank()) {
            throw new IllegalArgumentException("El motivo de anulación es obligatorio");
        }


        mov.setEstado(CajaMovimientoEstado.ANULADO);
        mov.setObservaciones((mov.getObservaciones() != null ? mov.getObservaciones() + " | " : "")
                + "ANULADO por " + username + ": " + request.getMotivo());
        movimientoRepository.save(mov);


        CajaMovimiento movAnulacion = new CajaMovimiento();
        movAnulacion.setTipo(CajaMovimientoTipo.ANULACION);
        movAnulacion.setMedioPago(mov.getMedioPago());
        movAnulacion.setMonto(mov.getMonto());
        movAnulacion.setDescripcion("Anulación del movimiento #" + movimientoId + ": " + request.getMotivo());
        movAnulacion.setReferenciaTipo("CajaMovimiento");
        movAnulacion.setReferenciaId(String.valueOf(movimientoId));
        movAnulacion.setUsuarioCreador(username);
        movAnulacion.setFechaHoraCreacion(LocalDateTime.now());
        caja.addMovimiento(movAnulacion);

        cajaRepository.save(caja);

        auditService.registrar(cajaId, "CajaMovimiento", movimientoId,
                "MOVIMIENTO_ANULADO", username,
                mov.getEstado().name(), CajaMovimientoEstado.ANULADO.name(),
                "Anulado: " + request.getMotivo());

        return movAnulacion;
    }



    public List<CajaMovimiento> obtenerMovimientos(Long cajaId) {
        CajaDiaria caja = cajaRepository.findByIdWithMovimientos(cajaId)
                .orElseThrow(() -> new IllegalArgumentException("Caja no encontrada: " + cajaId));
        return caja.getMovimientos();
    }

    public List<CajaMovimiento> obtenerMovimientosActivos(Long cajaId) {
        CajaDiaria caja = cajaRepository.findByIdWithMovimientos(cajaId)
                .orElseThrow(() -> new IllegalArgumentException("Caja no encontrada: " + cajaId));
        return caja.getMovimientos().stream()
                .filter(m -> m.getEstado() == CajaMovimientoEstado.ACTIVO)
                .toList();
    }



    private CajaDiaria obtenerCajaAbierta(Long cajaId) {
        CajaDiaria caja = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new IllegalArgumentException("Caja no encontrada: " + cajaId));
        if (caja.getEstado() != CajaEstado.ABIERTA) {
            throw new IllegalStateException("La caja no está abierta. Estado actual: " + caja.getEstado());
        }
        return caja;
    }

    private MedioPago parseMedioPago(String medioPago) {
        try {
            return MedioPago.valueOf(medioPago);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Medio de pago inválido: " + medioPago);
        }
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
