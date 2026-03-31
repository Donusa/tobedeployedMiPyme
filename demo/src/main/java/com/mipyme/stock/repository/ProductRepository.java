package com.mipyme.stock.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mipyme.stock.model.Product;

import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {
        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "productVariants",
                        "productVariants.attributes" })
        List<Product> findByInternalCode(String internalCode);

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {
                        "productVariants",
                        "productVariants.attributes",
                        "measurementUnit",
                        "productCategory",
                        "productBrand",
                        "warehouse",
                        "storage"
        })
        List<Product> findByTiendaNubeId(Long tiendaNubeId);

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "productVariants",
                        "productVariants.attributes" })
        List<Product> findByProductName(String productName);

        @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.productVariants v LEFT JOIN FETCH v.attributes WHERE p.isActive = true OR p.isActive IS NULL")
        List<Product> findAllWithVariants();

        @Query("SELECT p FROM Product p LEFT JOIN FETCH p.productVariants v LEFT JOIN FETCH v.attributes WHERE p.productId = :id")
        java.util.Optional<Product> findByIdWithVariants(
                        @org.springframework.data.repository.query.Param("id") Long id);

        @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.productVariants v LEFT JOIN FETCH v.attributes WHERE p.internalCode IN (SELECT p2.internalCode FROM Product p2 WHERE p2.internalCode IS NOT NULL GROUP BY p2.internalCode HAVING COUNT(p2) > 1)")
        java.util.List<Product> findDuplicateInternalCodes();

        @org.springframework.data.jpa.repository.Modifying
        @Query("UPDATE Product p SET p.tiendaNubeId = NULL WHERE p.tiendaNubeId IS NOT NULL AND p.isActive = false")
        void unlinkInactiveTiendaNubeProducts();

        @org.springframework.data.jpa.repository.Modifying
        @Query("UPDATE Product p SET p.tiendaNubeId = NULL")
        int unlinkAllTiendaNubeProducts();

        @org.springframework.data.jpa.repository.Modifying
        @Query("UPDATE Product p SET p.mercadoLibreId = NULL")
        void unlinkAllMercadoLibreProducts();

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {
                        "productVariants",
                        "productVariants.attributes",
                        "measurementUnit",
                        "productCategory",
                        "productBrand",
                        "warehouse",
                        "storage"
        })
        List<Product> findByTiendaNubeIdIsNotNull();

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {
                        "productVariants",
                        "productVariants.attributes",
                        "measurementUnit",
                        "productCategory",
                        "productBrand",
                        "warehouse",
                        "storage"
        })
        List<Product> findByMercadoLibreIdIsNotNull();

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {
                        "productVariants",
                        "productVariants.attributes",
                        "measurementUnit",
                        "productCategory",
                        "productBrand",
                        "warehouse",
                        "storage"
        })
        java.util.Optional<Product> findByMercadoLibreId(String mercadoLibreId);


        Long countByProductIdInAndTiendaNubeIdIsNotNull(List<Long> ids);

        Long countByProductIdInAndMercadoLibreIdIsNotNull(List<Long> ids);

        Long countByProductCategoryCategoryIdInAndTiendaNubeIdIsNotNull(List<Long> categoryIds);

        Long countByProductCategoryCategoryIdInAndMercadoLibreIdIsNotNull(List<Long> categoryIds);

        Long countByProductBrandBrandIdInAndTiendaNubeIdIsNotNull(List<Long> brandIds);

        Long countByProductBrandBrandIdInAndMercadoLibreIdIsNotNull(List<Long> brandIds);

        Long countByWarehouseWarehouseIdInAndTiendaNubeIdIsNotNull(List<Long> warehouseIds);

        Long countByWarehouseWarehouseIdInAndMercadoLibreIdIsNotNull(List<Long> warehouseIds);

        Long countByStorageStorageLocationIdInAndTiendaNubeIdIsNotNull(List<Long> storageIds);

        Long countByStorageStorageLocationIdInAndMercadoLibreIdIsNotNull(List<Long> storageIds);


        @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.productVariants WHERE p.productId IN :ids")
        List<Product> findByProductIdInWithVariants(
                        @org.springframework.data.repository.query.Param("ids") List<Long> ids);

        @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.productVariants WHERE p.productCategory.categoryId IN :catIds")
        List<Product> findByCategoryIdsWithVariants(
                        @org.springframework.data.repository.query.Param("catIds") List<Long> categoryIds);

        @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.productVariants WHERE p.productBrand.brandId IN :brandIds")
        List<Product> findByBrandIdsWithVariants(
                        @org.springframework.data.repository.query.Param("brandIds") List<Long> brandIds);

        @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.productVariants WHERE p.warehouse.warehouseId IN :whIds")
        List<Product> findByWarehouseIdsWithVariants(
                        @org.springframework.data.repository.query.Param("whIds") List<Long> warehouseIds);

        @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.productVariants WHERE p.storage.storageLocationId IN :slIds")
        List<Product> findByStorageIdsWithVariants(
                        @org.springframework.data.repository.query.Param("slIds") List<Long> storageIds);
}
