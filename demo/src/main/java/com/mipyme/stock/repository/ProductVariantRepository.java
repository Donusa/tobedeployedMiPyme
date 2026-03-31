package com.mipyme.stock.repository;

import java.util.List;
import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mipyme.stock.model.ProductVariant;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    @Query("SELECT DISTINCT v FROM ProductVariant v LEFT JOIN FETCH v.attributes WHERE v.product.productId = :productId AND (v.isActive = true OR v.isActive IS NULL)")
    List<ProductVariant> findAllByProductIdEager(@Param("productId") Long productId);

    @Query("SELECT v FROM ProductVariant v LEFT JOIN FETCH v.attributes WHERE v.productVariantId = :id")
    java.util.Optional<ProductVariant> findWithAttributesById(@Param("id") Long id);

    @Query("SELECT v FROM ProductVariant v LEFT JOIN FETCH v.attributes WHERE v.variantSku = :sku")
    java.util.Optional<ProductVariant> findByVariantSku(@Param("sku") String sku);

    java.util.Optional<ProductVariant> findByTiendaNubeId(Long tiendaNubeId);

    @Query("SELECT v FROM ProductVariant v LEFT JOIN FETCH v.attributes JOIN FETCH v.product WHERE (v.variantSku = :code OR v.variantGtin = :code) AND (v.isActive = true OR v.isActive IS NULL)")
    List<ProductVariant> findBySkuOrGtin(@Param("code") String code);

    @Query("SELECT COALESCE(SUM(v.stockQuantity), 0) FROM ProductVariant v WHERE v.product.productId = :productId AND (v.isActive = true OR v.isActive IS NULL)")
    BigDecimal sumStockByProductId(@Param("productId") Long productId);

    long countByProductProductId(Long productId);

    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product LEFT JOIN FETCH v.attributes WHERE v.variantSku IN (SELECT v2.variantSku FROM ProductVariant v2 WHERE v2.variantSku IS NOT NULL GROUP BY v2.variantSku HAVING COUNT(v2) > 1)")
    List<ProductVariant> findDuplicateSkus();

    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product LEFT JOIN FETCH v.attributes WHERE v.variantGtin IN (SELECT v2.variantGtin FROM ProductVariant v2 WHERE v2.variantGtin IS NOT NULL GROUP BY v2.variantGtin HAVING COUNT(v2) > 1)")
    List<ProductVariant> findDuplicateGtins();

    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product LEFT JOIN FETCH v.attributes WHERE v.isActive = true OR v.isActive IS NULL")
    List<ProductVariant> findAllEager();

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE ProductVariant v SET v.tiendaNubeId = NULL WHERE v.tiendaNubeId IS NOT NULL AND v.isActive = false")
    void unlinkInactiveTiendaNubeVariants();

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE ProductVariant v SET v.tiendaNubeId = NULL")
    int unlinkAllTiendaNubeVariants();

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE ProductVariant v SET v.mercadoLibreId = NULL")
    void unlinkAllMercadoLibreVariants();

    java.util.Optional<ProductVariant> findByMercadoLibreId(String mercadoLibreId);
}
