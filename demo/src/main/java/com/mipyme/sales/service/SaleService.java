package com.mipyme.sales.service;

import com.mipyme.sales.dto.CreateSaleRequest;
import com.mipyme.sales.dto.SalesMetricsResponse;
import com.mipyme.sales.model.Sale;
import com.mipyme.sales.model.SaleItem;
import com.mipyme.sales.repository.SaleRepository;
import com.mipyme.stock.model.Product;
import com.mipyme.stock.model.ProductVariant;
import com.mipyme.stock.repository.ProductRepository;
import com.mipyme.stock.repository.ProductVariantRepository;
import com.mipyme.caja.model.*;
import com.mipyme.caja.repository.CajaDiariaRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CajaDiariaRepository cajaDiariaRepository;

    public SaleService(SaleRepository saleRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            CajaDiariaRepository cajaDiariaRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.cajaDiariaRepository = cajaDiariaRepository;
    }

    public List<Sale> getAllSales() {
        return saleRepository.findAllByOrderBySaleDateDesc();
    }

    @Transactional
    public Sale markAsFacturado(Long saleId) {
        Sale sale = saleRepository.findByIdWithItems(saleId)
                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada: " + saleId));
        sale.setFacturado(true);
        return saleRepository.save(sale);
    }

    @Transactional
    public Sale createSale(CreateSaleRequest request) {
        Sale sale = new Sale();

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        sale.setCreatedBy(username);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateSaleRequest.SaleItemRequest itemRequest : request.getItems()) {
            SaleItem saleItem = new SaleItem();
            saleItem.setQuantity(itemRequest.getQuantity());

            BigDecimal price = BigDecimal.ZERO;

            if (itemRequest.getProductVariantId() != null) {
                ProductVariant variant = productVariantRepository.findById(itemRequest.getProductVariantId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Variant not found: " + itemRequest.getProductVariantId()));

                if (variant.getStockQuantity().compareTo(itemRequest.getQuantity()) < 0) {
                    throw new IllegalArgumentException("Insufficient stock for variant: " + variant.getVariantSku());
                }

                variant.setStockQuantity(variant.getStockQuantity().subtract(itemRequest.getQuantity()));
                productVariantRepository.save(variant);


                Product product = variant.getProduct();
                if (product != null) {
                    BigDecimal sum = productVariantRepository.sumStockByProductId(product.getProductId());
                    product.setStockQuantity(sum == null ? BigDecimal.ZERO : sum);
                    productRepository.save(product);
                }

                price = variant.getPrice();
                if (price == null && product != null) {
                    price = product.getPrice();
                }


                saleItem.setProductVariantId(variant.getProductVariantId());
                if (product != null) {
                    saleItem.setProductId(product.getProductId());
                    saleItem.setProductName(product.getProductName());
                    saleItem.setInternalCode(product.getInternalCode());
                }
                saleItem.setVariantSku(variant.getVariantSku());

            } else if (itemRequest.getProductId() != null) {
                Product product = productRepository.findById(itemRequest.getProductId())
                        .orElseThrow(
                                () -> new IllegalArgumentException("Product not found: " + itemRequest.getProductId()));

                BigDecimal currentStock = product.getStockQuantity() != null ? product.getStockQuantity()
                        : BigDecimal.ZERO;
                if (currentStock.compareTo(itemRequest.getQuantity()) < 0) {
                    throw new IllegalArgumentException("Insufficient stock for product: " + product.getProductName());
                }

                product.setStockQuantity(currentStock.subtract(itemRequest.getQuantity()));
                productRepository.save(product);

                price = product.getPrice();


                saleItem.setProductId(product.getProductId());
                saleItem.setProductName(product.getProductName());
                saleItem.setInternalCode(product.getInternalCode());
            } else {
                throw new IllegalArgumentException("Item must have product or variant ID");
            }

            if (price == null) {
                throw new IllegalArgumentException("Price not defined for product/variant");
            }

            saleItem.setUnitPrice(price);
            BigDecimal subtotal = price.multiply(itemRequest.getQuantity());
            saleItem.setSubtotal(subtotal);

            totalAmount = totalAmount.add(subtotal);
            sale.addItem(saleItem);
        }

        sale.setTotalAmount(totalAmount);


        if (request.getMedioPago() != null && !request.getMedioPago().isBlank()) {
            sale.setMedioPago(request.getMedioPago());
        } else {
            sale.setMedioPago("EFECTIVO");
        }

        sale = saleRepository.save(sale);


        CajaDiaria caja = null;
        if (request.getCajaId() != null) {
            caja = cajaDiariaRepository.findById(request.getCajaId())
                    .orElseThrow(() -> new IllegalArgumentException("Caja no encontrada: " + request.getCajaId()));
            if (caja.getEstado() != CajaEstado.ABIERTA && caja.getEstado() != CajaEstado.EN_RELEVO) {
                throw new IllegalStateException("La caja no está abierta. Estado actual: " + caja.getEstado());
            }
        } else {

            caja = cajaDiariaRepository.findAnyCajaAbierta().orElse(null);
        }

        if (caja != null) {
            MedioPago medioPago = MedioPago.EFECTIVO;
            if (request.getMedioPago() != null && !request.getMedioPago().isBlank()) {
                try {
                    medioPago = MedioPago.valueOf(request.getMedioPago());
                } catch (IllegalArgumentException ignored) {

                }
            }

            CajaMovimiento mov = new CajaMovimiento();
            mov.setTipo(CajaMovimientoTipo.VENTA);
            mov.setMedioPago(medioPago);
            mov.setMonto(sale.getTotalAmount());
            mov.setDescripcion("Venta #" + sale.getSaleId());
            mov.setReferenciaTipo("VENTA");
            mov.setReferenciaId(String.valueOf(sale.getSaleId()));
            mov.setUsuarioCreador(username);
            mov.setFechaHoraCreacion(LocalDateTime.now());
            mov.setIdempotencyKey("VENTA_" + sale.getSaleId());
            caja.addMovimiento(mov);

            cajaDiariaRepository.save(caja);
        }

        return sale;
    }

    public SalesMetricsResponse getSalesMetrics(String range, LocalDate customStart, LocalDate customEnd) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentStart, currentEnd, prevStart, prevEnd;
        String periodType = "DAILY";

        if ("TODAY".equalsIgnoreCase(range)) {
            currentStart = now.toLocalDate().atStartOfDay();
            currentEnd = now;
            prevStart = currentStart.minusDays(1);
            prevEnd = currentEnd.minusDays(1);
            periodType = "HOURLY";
        } else if ("7D".equalsIgnoreCase(range)) {
            currentEnd = now;
            currentStart = now.minusDays(7);
            prevEnd = currentStart;
            prevStart = prevEnd.minusDays(7);
        } else if ("30D".equalsIgnoreCase(range)) {
            currentEnd = now;
            currentStart = now.minusDays(30);
            prevEnd = currentStart;
            prevStart = prevEnd.minusDays(30);
        } else if ("1Y".equalsIgnoreCase(range)) {
            currentEnd = now;
            currentStart = now.minusYears(1);
            prevEnd = currentStart;
            prevStart = prevEnd.minusYears(1);
            periodType = "MONTHLY";
        } else if ("CUSTOM".equalsIgnoreCase(range) && customStart != null && customEnd != null) {
            currentStart = customStart.atStartOfDay();
            currentEnd = customEnd.atTime(23, 59, 59);
            long days = java.time.temporal.ChronoUnit.DAYS.between(customStart, customEnd);
            prevEnd = currentStart;
            prevStart = prevEnd.minusDays(days > 0 ? days : 1);
            if (days <= 1)
                periodType = "HOURLY";
            else if (days > 365)
                periodType = "MONTHLY";
        } else {

            currentEnd = now;
            currentStart = now.minusDays(7);
            prevEnd = currentStart;
            prevStart = prevEnd.minusDays(7);
        }

        List<Sale> currentSales = saleRepository.findBySaleDateBetween(currentStart, currentEnd);
        List<Sale> prevSales = saleRepository.findBySaleDateBetween(prevStart, prevEnd);

        return new SalesMetricsResponse(
                aggregateSales(currentSales, periodType, currentStart, currentEnd),
                aggregateSales(prevSales, periodType, prevStart, prevEnd),
                periodType);
    }

    private List<SalesMetricsResponse.SalesMetricPoint> aggregateSales(List<Sale> sales, String periodType,
            LocalDateTime start, LocalDateTime end) {
        Map<String, SalesMetricsResponse.SalesMetricPoint> aggregationMap = new TreeMap<>();
        DateTimeFormatter formatter;

        if ("HOURLY".equals(periodType)) {
            formatter = DateTimeFormatter.ofPattern("HH:00");
        } else if ("MONTHLY".equals(periodType)) {
            formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        } else {
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        }


        LocalDateTime cursor = start;
        while (cursor.isBefore(end) || cursor.isEqual(end)) {
            String key = cursor.format(formatter);
            if (!aggregationMap.containsKey(key)) {
                aggregationMap.put(key, new SalesMetricsResponse.SalesMetricPoint(key, BigDecimal.ZERO, 0, 0));
            }

            if ("HOURLY".equals(periodType))
                cursor = cursor.plusHours(1);
            else if ("MONTHLY".equals(periodType))
                cursor = cursor.plusMonths(1);
            else
                cursor = cursor.plusDays(1);
        }

        for (Sale sale : sales) {
            String key = sale.getSaleDate().format(formatter);
            SalesMetricsResponse.SalesMetricPoint point = aggregationMap.get(key);
            if (point != null) {
                point.setTotalAmount(point.getTotalAmount().add(sale.getTotalAmount()));
                point.setOrderCount(point.getOrderCount() + 1);
                int units = sale.getItems().stream()
                        .mapToInt(item -> item.getQuantity().intValue())
                        .sum();
                point.setUnitCount(point.getUnitCount() + units);
            }
        }

        return new ArrayList<>(aggregationMap.values());
    }
}
