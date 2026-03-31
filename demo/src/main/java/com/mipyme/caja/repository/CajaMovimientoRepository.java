package com.mipyme.caja.repository;

import com.mipyme.caja.model.CajaMovimiento;
import com.mipyme.caja.model.CajaMovimientoEstado;
import com.mipyme.caja.model.CajaMovimientoTipo;
import com.mipyme.caja.model.MedioPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CajaMovimientoRepository extends JpaRepository<CajaMovimiento, Long> {

    List<CajaMovimiento> findByCajaDiaria_CajaDiariaIdOrderByFechaHoraCreacionDesc(Long cajaDiariaId);

    Optional<CajaMovimiento> findByCajaDiaria_CajaDiariaIdAndIdempotencyKey(Long cajaDiariaId, String idempotencyKey);

    @Query("SELECT COALESCE(SUM(m.monto), 0) FROM CajaMovimiento m " +
            "WHERE m.cajaDiaria.cajaDiariaId = :cajaId " +
            "AND m.medioPago = :medio " +
            "AND m.estado = :estado " +
            "AND m.tipo IN :tipos")
    BigDecimal sumMontoByMedioPagoAndTipos(
            @Param("cajaId") Long cajaDiariaId,
            @Param("medio") MedioPago medioPago,
            @Param("estado") CajaMovimientoEstado estado,
            @Param("tipos") List<CajaMovimientoTipo> tipos);

    @Query("SELECT COALESCE(SUM(m.monto), 0) FROM CajaMovimiento m " +
            "WHERE m.cajaDiaria.cajaDiariaId = :cajaId " +
            "AND m.estado = com.mipyme.caja.model.CajaMovimientoEstado.ACTIVO " +
            "AND m.tipo IN :tipos")
    BigDecimal sumMontoByTipos(
            @Param("cajaId") Long cajaDiariaId,
            @Param("tipos") List<CajaMovimientoTipo> tipos);

    @Query("SELECT m.medioPago, COALESCE(SUM(m.monto), 0) FROM CajaMovimiento m " +
            "WHERE m.cajaDiaria.cajaDiariaId = :cajaId " +
            "AND m.estado = com.mipyme.caja.model.CajaMovimientoEstado.ACTIVO " +
            "AND m.tipo IN :tipos " +
            "GROUP BY m.medioPago")
    List<Object[]> sumMontoGroupByMedioPago(
            @Param("cajaId") Long cajaDiariaId,
            @Param("tipos") List<CajaMovimientoTipo> tipos);

    long countByCajaDiaria_CajaDiariaIdAndEstadoAndTipoIn(
            Long cajaDiariaId, CajaMovimientoEstado estado, List<CajaMovimientoTipo> tipos);
}
