package com.mipyme.caja.repository;

import com.mipyme.caja.model.CajaEgresoCategoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CajaEgresoCategoriaRepository extends JpaRepository<CajaEgresoCategoria, Long> {
    List<CajaEgresoCategoria> findByActivaTrueOrderByNombreAsc();
}
