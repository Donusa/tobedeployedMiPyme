package com.mipyme.caja.service;

import com.mipyme.caja.dto.*;
import com.mipyme.caja.model.*;
import com.mipyme.caja.repository.CajaDiariaRepository;
import com.mipyme.caja.repository.CajaEgresoCategoriaRepository;
import com.mipyme.caja.repository.CajaMovimientoRepository;
import com.mipyme.sales.model.Sale;
import com.mipyme.sales.repository.SaleRepository;
import com.mipyme.stock.repository.WarehouseRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CajaDiariaService {

    private final CajaDiariaRepository cajaRepository;
    private final CajaMovimientoRepository movimientoRepository;
    private final CajaEgresoCategoriaRepository categoriaRepository;
    private final WarehouseRepository warehouseRepository;
    private final CajaAuditService auditService;
    private final SaleRepository saleRepository;


    private static final List<CajaMovimientoTipo> TIPOS_INGRESO = List.of(
            CajaMovimientoTipo.VENTA,
            CajaMovimientoTipo.COBRO_CUENTA_CORRIENTE,
            CajaMovimientoTipo.INGRESO_MANUAL,
            CajaMovimientoTipo.REPOSICION_CAMBIO,
            CajaMovimientoTipo.AJUSTE_POSITIVO);


    private static final List<CajaMovimientoTipo> TIPOS_EGRESO = List.of(
            CajaMovimientoTipo.GASTO_OPERATIVO,
            CajaMovimientoTipo.PAGO_PROVEEDOR,
            CajaMovimientoTipo.RETIRO_EFECTIVO,
            CajaMovimientoTipo.DEVOLUCION_CLIENTE,
            CajaMovimientoTipo.AJUSTE_NEGATIVO);

    public CajaDiariaService(CajaDiariaRepository cajaRepository,
            CajaMovimientoRepository movimientoRepository,
            CajaEgresoCategoriaRepository categoriaRepository,
            WarehouseRepository warehouseRepository,
            CajaAuditService auditService,
            SaleRepository saleRepository) {
        this.cajaRepository = cajaRepository;
        this.movimientoRepository = movimientoRepository;
        this.categoriaRepository = categoriaRepository;
        this.warehouseRepository = warehouseRepository;
        this.auditService = auditService;
        this.saleRepository = saleRepository;
    }



    @Transactional
    public CajaDiaria abrirCaja(AbrirCajaRequest request) {
        String username = getCurrentUsername();

        if (request.getSucursalId() == null) {
            throw new IllegalArgumentException("La sucursal es obligatoria");
        }
        if (request.getMontoAperturaEfectivo() == null || request.getMontoAperturaEfectivo().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto de apertura debe ser mayor o igual a cero");
        }
        if (!warehouseRepository.existsById(request.getSucursalId())) {
            throw new IllegalArgumentException("Sucursal no encontrada: " + request.getSucursalId());
        }


        Optional<CajaDiaria> cajaExistente = cajaRepository.findCajaAbiertaByWarehouse(request.getSucursalId());
        if (cajaExistente.isPresent()) {
            throw new IllegalStateException("Ya existe una caja abierta o en relevo para esta sucursal (ID: "
                    + cajaExistente.get().getCajaDiariaId() + ")");
        }

        CajaDiaria caja = new CajaDiaria();
        caja.setWarehouseId(request.getSucursalId());
        caja.setFechaOperativa(LocalDate.now());
        caja.setEstado(CajaEstado.ABIERTA);
        caja.setUsuarioApertura(username);
        caja.setUsuarioResponsable(username);
        caja.setFechaHoraApertura(LocalDateTime.now());
        caja.setMontoAperturaEfectivo(request.getMontoAperturaEfectivo());
        caja.setObservacionApertura(request.getObservacion());

        caja = cajaRepository.save(caja);


        CajaMovimiento movApertura = new CajaMovimiento();
        movApertura.setTipo(CajaMovimientoTipo.APERTURA);
        movApertura.setMedioPago(MedioPago.EFECTIVO);
        movApertura.setMonto(request.getMontoAperturaEfectivo());
        movApertura.setDescripcion("Apertura de caja");
        movApertura.setUsuarioCreador(username);
        movApertura.setFechaHoraCreacion(LocalDateTime.now());
        movApertura.setIdempotencyKey("APERTURA_" + caja.getCajaDiariaId());
        caja.addMovimiento(movApertura);

        caja = cajaRepository.save(caja);

        auditService.registrar(caja.getCajaDiariaId(), "CajaDiaria", caja.getCajaDiariaId(),
                "APERTURA", username, null, null,
                "Caja abierta con $" + request.getMontoAperturaEfectivo() + " en efectivo");

        return caja;
    }



    @Transactional
    public CajaDiaria cerrarCaja(Long cajaId, CerrarCajaRequest request) {
        String username = getCurrentUsername();

        CajaDiaria caja = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new IllegalArgumentException("Caja no encontrada: " + cajaId));

        if (caja.getEstado() != CajaEstado.ABIERTA) {
            throw new IllegalStateException("Solo se puede cerrar una caja en estado ABIERTA. Estado actual: " + caja.getEstado());
        }
        if (request.getMontoContadoEfectivo() == null || request.getMontoContadoEfectivo().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto contado de efectivo es obligatorio y debe ser >= 0");
        }


        BigDecimal efectivoEsperado = calcularEfectivoEsperado(cajaId, caja.getMontoAperturaEfectivo());
        BigDecimal diferencia = request.getMontoContadoEfectivo().subtract(efectivoEsperado);

        if (diferencia.compareTo(BigDecimal.ZERO) != 0) {
            if (request.getObservacion() == null || request.getObservacion().isBlank()) {
                throw new IllegalArgumentException("Se detectó diferencia de $" + diferencia
                        + ". La observación es obligatoria cuando hay diferencia de efectivo");
            }
            caja.setEstado(CajaEstado.CERRADA_CON_DIFERENCIA);
        } else {
            caja.setEstado(CajaEstado.CERRADA);
        }

        caja.setUsuarioCierre(username);
        caja.setFechaHoraCierre(LocalDateTime.now());
        caja.setMontoEsperadoEfectivo(efectivoEsperado);
        caja.setMontoContadoEfectivo(request.getMontoContadoEfectivo());
        caja.setMontoDiferencia(diferencia);
        caja.setObservacionCierre(request.getObservacion());


        CajaMovimiento movCierre = new CajaMovimiento();
        movCierre.setTipo(CajaMovimientoTipo.CIERRE);
        movCierre.setMedioPago(MedioPago.EFECTIVO);
        movCierre.setMonto(request.getMontoContadoEfectivo());
        movCierre.setDescripcion("Cierre de caja — Esperado: $" + efectivoEsperado
                + " — Contado: $" + request.getMontoContadoEfectivo()
                + " — Diferencia: $" + diferencia);
        movCierre.setUsuarioCreador(username);
        movCierre.setFechaHoraCreacion(LocalDateTime.now());
        movCierre.setIdempotencyKey("CIERRE_" + cajaId);
        caja.addMovimiento(movCierre);

        caja = cajaRepository.save(caja);

        auditService.registrar(caja.getCajaDiariaId(), "CajaDiaria", caja.getCajaDiariaId(),
                "CIERRE", username, null, null,
                "Caja cerrada — Estado: " + caja.getEstado()
                        + " — Esperado: $" + efectivoEsperado
                        + " — Contado: $" + request.getMontoContadoEfectivo()
                        + " — Diferencia: $" + diferencia);

        return caja;
    }



    public Optional<CajaDiaria> obtenerCajaActual(Long sucursalId) {
        return cajaRepository.findCajaAbiertaByWarehouse(sucursalId);
    }

    public CajaDiaria obtenerPorId(Long cajaId) {
        return cajaRepository.findById(cajaId)
                .orElseThrow(() -> new IllegalArgumentException("Caja no encontrada: " + cajaId));
    }

    public CajaDiaria obtenerPorIdConMovimientos(Long cajaId) {
        return cajaRepository.findByIdWithMovimientos(cajaId)
                .orElseThrow(() -> new IllegalArgumentException("Caja no encontrada: " + cajaId));
    }

    public List<CajaDiaria> obtenerHistorial(Long sucursalId, LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null) {
            return cajaRepository.findByWarehouseIdAndFechaOperativaBetweenOrderByFechaOperativaDesc(
                    sucursalId, desde, hasta);
        }
        return cajaRepository.findByWarehouseIdOrderByFechaOperativaDesc(sucursalId);
    }




    public List<Sale> obtenerVentasDeCaja(Long cajaId) {
        CajaDiaria caja = obtenerPorId(cajaId);
        LocalDateTime desde = caja.getFechaHoraApertura();
        LocalDateTime hasta = (caja.getFechaHoraCierre() != null)
                ? caja.getFechaHoraCierre()
                : LocalDateTime.now();
        return saleRepository.findBySaleDateBetween(desde, hasta);
    }




    private static final List<CajaMovimientoTipo> TIPOS_INGRESO_SIN_VENTA = List.of(
            CajaMovimientoTipo.COBRO_CUENTA_CORRIENTE,
            CajaMovimientoTipo.INGRESO_MANUAL,
            CajaMovimientoTipo.REPOSICION_CAMBIO,
            CajaMovimientoTipo.AJUSTE_POSITIVO);

    public CajaResumenResponse obtenerResumen(Long cajaId) {
        CajaDiaria caja = obtenerPorId(cajaId);

        CajaResumenResponse resumen = new CajaResumenResponse();
        resumen.setCajaId(cajaId);
        resumen.setEstado(caja.getEstado().name());


        List<Sale> ventas = obtenerVentasDeCaja(cajaId);

        BigDecimal totalVendido = ventas.stream()
                .map(Sale::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        resumen.setTotalVendido(totalVendido);
        resumen.setCantidadVentas(ventas.size());


        BigDecimal otrosIngresos = movimientoRepository.sumMontoByTipos(cajaId, TIPOS_INGRESO_SIN_VENTA);
        resumen.setTotalCobrado(totalVendido.add(otrosIngresos));


        resumen.setCantidadEgresos(movimientoRepository.countByCajaDiaria_CajaDiariaIdAndEstadoAndTipoIn(
                cajaId, CajaMovimientoEstado.ACTIVO, TIPOS_EGRESO));
        resumen.setCantidadDevoluciones(movimientoRepository.countByCajaDiaria_CajaDiariaIdAndEstadoAndTipoIn(
                cajaId, CajaMovimientoEstado.ACTIVO, List.of(CajaMovimientoTipo.DEVOLUCION_CLIENTE)));
        resumen.setCantidadAnulaciones(movimientoRepository.countByCajaDiaria_CajaDiariaIdAndEstadoAndTipoIn(
                cajaId, CajaMovimientoEstado.ANULADO, TIPOS_INGRESO));


        Map<String, BigDecimal> resumenMedios = new LinkedHashMap<>();


        for (Sale venta : ventas) {
            String medio = (venta.getMedioPago() != null && !venta.getMedioPago().isBlank())
                    ? venta.getMedioPago()
                    : "SIN_DEFINIR";
            resumenMedios.merge(medio, venta.getTotalAmount(), BigDecimal::add);
        }


        List<CajaMovimientoTipo> tiposSinVenta = new ArrayList<>(TIPOS_INGRESO_SIN_VENTA);
        tiposSinVenta.addAll(TIPOS_EGRESO);
        List<Object[]> porMedio = movimientoRepository.sumMontoGroupByMedioPago(cajaId, tiposSinVenta);
        for (Object[] row : porMedio) {
            resumenMedios.merge(row[0].toString(), (BigDecimal) row[1], BigDecimal::add);
        }
        resumen.setResumenPorMedioPago(resumenMedios);


        BigDecimal ventasEfectivo = ventas.stream()
                .filter(v -> "EFECTIVO".equals(v.getMedioPago()))
                .map(Sale::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal otrosIngresosEfectivo = movimientoRepository.sumMontoByMedioPagoAndTipos(
                cajaId, MedioPago.EFECTIVO, CajaMovimientoEstado.ACTIVO, TIPOS_INGRESO_SIN_VENTA);
        BigDecimal ingresosEfectivo = ventasEfectivo.add(otrosIngresosEfectivo);

        BigDecimal egresosEfectivo = movimientoRepository.sumMontoByMedioPagoAndTipos(
                cajaId, MedioPago.EFECTIVO, CajaMovimientoEstado.ACTIVO, TIPOS_EGRESO);

        CajaResumenResponse.CajaFisicaResumen fisico = new CajaResumenResponse.CajaFisicaResumen();
        fisico.setAperturaEfectivo(caja.getMontoAperturaEfectivo());
        fisico.setIngresosEfectivo(ingresosEfectivo);
        fisico.setEgresosEfectivo(egresosEfectivo);
        fisico.setEfectivoEsperado(caja.getMontoAperturaEfectivo().add(ingresosEfectivo).subtract(egresosEfectivo));
        fisico.setEfectivoContado(caja.getMontoContadoEfectivo());
        fisico.setDiferencia(caja.getMontoDiferencia());
        resumen.setCajaFisica(fisico);

        return resumen;
    }



    public BigDecimal calcularEfectivoEsperado(Long cajaId, BigDecimal montoApertura) {

        List<Sale> ventas = obtenerVentasDeCaja(cajaId);
        BigDecimal ventasEfectivo = ventas.stream()
                .filter(v -> "EFECTIVO".equals(v.getMedioPago()))
                .map(Sale::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        BigDecimal otrosIngresosEfectivo = movimientoRepository.sumMontoByMedioPagoAndTipos(
                cajaId, MedioPago.EFECTIVO, CajaMovimientoEstado.ACTIVO, TIPOS_INGRESO_SIN_VENTA);
        BigDecimal ingresosEfectivo = ventasEfectivo.add(otrosIngresosEfectivo);

        BigDecimal egresosEfectivo = movimientoRepository.sumMontoByMedioPagoAndTipos(
                cajaId, MedioPago.EFECTIVO, CajaMovimientoEstado.ACTIVO, TIPOS_EGRESO);
        return montoApertura.add(ingresosEfectivo).subtract(egresosEfectivo);
    }

    public CajaDiariaResponse toResponse(CajaDiaria caja) {
        CajaDiariaResponse resp = new CajaDiariaResponse();
        resp.setId(caja.getCajaDiariaId());
        resp.setSucursalId(caja.getWarehouseId());

        warehouseRepository.findById(caja.getWarehouseId())
                .ifPresent(w -> resp.setSucursalNombre(w.getWarehouseName()));

        resp.setFechaOperativa(caja.getFechaOperativa());
        resp.setEstado(caja.getEstado().name());
        resp.setUsuarioApertura(caja.getUsuarioApertura());
        resp.setUsuarioResponsable(caja.getUsuarioResponsable());
        resp.setUsuarioCierre(caja.getUsuarioCierre());
        resp.setFechaHoraApertura(caja.getFechaHoraApertura());
        resp.setFechaHoraCierre(caja.getFechaHoraCierre());
        resp.setMontoAperturaEfectivo(caja.getMontoAperturaEfectivo());
        resp.setMontoEsperadoEfectivo(caja.getMontoEsperadoEfectivo());
        resp.setMontoContadoEfectivo(caja.getMontoContadoEfectivo());
        resp.setMontoDiferencia(caja.getMontoDiferencia());
        resp.setObservacionApertura(caja.getObservacionApertura());
        resp.setObservacionCierre(caja.getObservacionCierre());
        resp.setRequiereRevision(caja.isRequiereRevision());
        return resp;
    }

    public CajaMovimientoResponse toMovimientoResponse(CajaMovimiento mov) {
        CajaMovimientoResponse resp = new CajaMovimientoResponse();
        resp.setId(mov.getMovimientoId());
        resp.setTipo(mov.getTipo().name());
        resp.setMedioPago(mov.getMedioPago().name());
        resp.setMonto(mov.getMonto());
        resp.setDescripcion(mov.getDescripcion());
        resp.setJustificacion(mov.getJustificacion());
        resp.setComprobanteTexto(mov.getComprobanteTexto());
        resp.setComprobanteNumero(mov.getComprobanteNumero());
        resp.setReferenciaTipo(mov.getReferenciaTipo());
        resp.setReferenciaId(mov.getReferenciaId());
        resp.setUsuarioCreador(mov.getUsuarioCreador());
        resp.setFechaHoraCreacion(mov.getFechaHoraCreacion());
        resp.setEstado(mov.getEstado().name());
        resp.setObservaciones(mov.getObservaciones());


        if (mov.getCategoriaEgresoId() != null) {
            categoriaRepository.findById(mov.getCategoriaEgresoId())
                    .ifPresent(cat -> resp.setCategoriaEgreso(cat.getNombre()));
        }

        return resp;
    }

    public List<CajaEgresoCategoria> obtenerCategoriasEgresoActivas() {
        return categoriaRepository.findByActivaTrueOrderByNombreAsc();
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
