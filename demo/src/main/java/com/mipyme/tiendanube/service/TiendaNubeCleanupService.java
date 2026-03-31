package com.mipyme.tiendanube.service;

import com.mipyme.stock.repository.ProductRepository;
import com.mipyme.stock.repository.ProductVariantRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TiendaNubeCleanupService implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    public TiendaNubeCleanupService(ProductRepository productRepository, ProductVariantRepository productVariantRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {



        productRepository.unlinkInactiveTiendaNubeProducts();


        productVariantRepository.unlinkInactiveTiendaNubeVariants();
    }
}
