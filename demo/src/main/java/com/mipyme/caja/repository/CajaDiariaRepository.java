package com.mipyme.caja.repository;

import com.mipyme.caja.model.CajaDiaria;
import com.mipyme.caja.model.CajaEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CajaDiariaRepository extends JpaRepository<CajaDiaria, Long> {

    @Query("SELECT c FROM CajaDiaria c WHERE c.warehouseId = :wid AND c.estado IN :estados")
    List<CajaDiaria> findByWarehouseIdAndEstadoIn(
            @Param("wid") Long warehouseId,
            @Param("estados") List<CajaEstado> estados);

    @Query("SELECT c FROM CajaDiaria c WHERE c.warehouseId = :wid AND c.estado IN ('ABIERTA','EN_RELEVO')")
    Optional<CajaDiaria> findCajaAbiertaByWarehouse(@Param("wid") Long warehouseId);

    @Query("SELECT c FROM CajaDiaria c WHERE c.estado IN ('ABIERTA','EN_RELEVO') ORDER BY c.fechaHoraApertura DESC")
    Optional<CajaDiaria> findAnyCajaAbierta();

    @Query("SELECT c FROM CajaDiaria c LEFT JOIN FETCH c.movimientos WHERE c.cajaDiariaId = :id")
    Optional<CajaDiaria> findByIdWithMovimientos(@Param("id") Long id);

    List<CajaDiaria> findByWarehouseIdAndFechaOperativaBetweenOrderByFechaOperativaDesc(
            Long warehouseId, LocalDate desde, LocalDate hasta);

    List<CajaDiaria> findByWarehouseIdOrderByFechaOperativaDesc(Long warehouseId);
}
