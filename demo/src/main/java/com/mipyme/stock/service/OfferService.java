package com.mipyme.stock.service;

import com.mipyme.stock.model.Offer;
import com.mipyme.stock.model.OfferTargetType;
import com.mipyme.stock.repository.OfferRepository;
import com.mipyme.stock.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final ProductRepository productRepository;

    public OfferService(OfferRepository offerRepository, ProductRepository productRepository) {
        this.offerRepository = offerRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Offer> getAll() {
        List<Offer> offers = offerRepository.findAll();
        offers.forEach(this::enrichWithLinkStatus);
        return offers;
    }

    @Transactional(readOnly = true)
    public Offer getById(Long id) {
        Offer offer = offerRepository.findById(id).orElse(null);
        if (offer != null) {
            enrichWithLinkStatus(offer);
        }
        return offer;
    }

    private void enrichWithLinkStatus(Offer offer) {
        if (offer.getTargetType() == null || offer.getTargetIds() == null || offer.getTargetIds().isEmpty()) {
            offer.setHasTiendaNubeLinks(false);
            offer.setHasMercadoLibreLinks(false);
            return;
        }

        OfferTargetType type = offer.getTargetType();
        List<Long> ids = offer.getTargetIds();

        if (type == OfferTargetType.SPECIFIC) {
            offer.setHasTiendaNubeLinks(productRepository.countByProductIdInAndTiendaNubeIdIsNotNull(ids) > 0);
            offer.setHasMercadoLibreLinks(productRepository.countByProductIdInAndMercadoLibreIdIsNotNull(ids) > 0);
        } else if (type == OfferTargetType.CATEGORY) {
            offer.setHasTiendaNubeLinks(productRepository.countByProductCategoryCategoryIdInAndTiendaNubeIdIsNotNull(ids) > 0);
            offer.setHasMercadoLibreLinks(productRepository.countByProductCategoryCategoryIdInAndMercadoLibreIdIsNotNull(ids) > 0);
        } else if (type == OfferTargetType.BRAND) {
            offer.setHasTiendaNubeLinks(productRepository.countByProductBrandBrandIdInAndTiendaNubeIdIsNotNull(ids) > 0);
            offer.setHasMercadoLibreLinks(productRepository.countByProductBrandBrandIdInAndMercadoLibreIdIsNotNull(ids) > 0);
        } else if (type == OfferTargetType.WAREHOUSE) {
            offer.setHasTiendaNubeLinks(productRepository.countByWarehouseWarehouseIdInAndTiendaNubeIdIsNotNull(ids) > 0);
            offer.setHasMercadoLibreLinks(productRepository.countByWarehouseWarehouseIdInAndMercadoLibreIdIsNotNull(ids) > 0);
        } else if (type == OfferTargetType.LOCATION) {
            offer.setHasTiendaNubeLinks(productRepository.countByStorageStorageLocationIdInAndTiendaNubeIdIsNotNull(ids) > 0);
            offer.setHasMercadoLibreLinks(productRepository.countByStorageStorageLocationIdInAndMercadoLibreIdIsNotNull(ids) > 0);
        }
    }

    @Transactional
    public Offer create(Offer offer) {
        offer.setOfferId(null);
        return offerRepository.save(offer);
    }

    @Transactional
    public Offer update(Long id, Offer offer) {
        return offerRepository.findById(id).map(existing -> {
            existing.setName(offer.getName());
            existing.setStartDate(offer.getStartDate());
            existing.setEndDate(offer.getEndDate());
            existing.setIndefinite(offer.isIndefinite());
            existing.setTargetType(offer.getTargetType());
            existing.setTargetIds(offer.getTargetIds());
            existing.setDiscountValue(offer.getDiscountValue());
            existing.setDiscountType(offer.getDiscountType());
            existing.setBuyQuantity(offer.getBuyQuantity());
            existing.setPayQuantity(offer.getPayQuantity());
            return offerRepository.save(existing);
        }).orElse(null);
    }

    @Transactional
    public void delete(Long id) {
        offerRepository.deleteById(id);
    }

    @Transactional
    public Offer publishToTiendaNube(Long id) {
        return offerRepository.findById(id).map(existing -> {
             existing.setPublishedTiendaNube(true);
             return offerRepository.save(existing);
        }).orElse(null);
    }

    @Transactional
    public Offer unpublishFromTiendaNube(Long id) {
        return offerRepository.findById(id).map(existing -> {
             existing.setPublishedTiendaNube(false);
             return offerRepository.save(existing);
        }).orElse(null);
    }

    @Transactional
    public Offer publishToMercadoLibre(Long id) {
        return offerRepository.findById(id).map(existing -> {
             existing.setPublishedMercadoLibre(true);
             return offerRepository.save(existing);
        }).orElse(null);
    }

    @Transactional
    public Offer unpublishFromMercadoLibre(Long id) {
        return offerRepository.findById(id).map(existing -> {
             existing.setPublishedMercadoLibre(false);
             return offerRepository.save(existing);
        }).orElse(null);
    }
}
