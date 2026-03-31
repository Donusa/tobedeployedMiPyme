package com.mipyme.notification;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mipyme.company.Company;
import com.mipyme.company.CompanyRepository;
import com.mipyme.notification.model.Notification.NotificationType;
import com.mipyme.sales.model.Sale;
import com.mipyme.sales.repository.SaleRepository;
import com.mipyme.stock.model.Product;
import com.mipyme.stock.repository.ProductRepository;
import com.mipyme.tenant.TenantContext;

@Component
public class NotificationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationService notificationService;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final CompanyRepository companyRepository;

    public NotificationScheduler(NotificationService notificationService,
            ProductRepository productRepository,
            SaleRepository saleRepository,
            CompanyRepository companyRepository) {
        this.notificationService = notificationService;
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.companyRepository = companyRepository;
    }


    @Scheduled(fixedRate = 3600000)
    public void cleanupOldNotifications() {
        runForAllTenants(() -> {
            try {
                notificationService.cleanupOldNotifications();
            } catch (Exception e) {
                logger.error("Error cleaning up notifications: {}", e.getMessage());
            }
        });
    }


    @Scheduled(fixedRate = 1800000)
    public void checkStockAlerts() {
        runForAllTenants(() -> {
            try {
                List<Product> products = productRepository.findAll();
                for (Product p : products) {
                    if (p.getMinStock() != null
                            && p.getMinStock().compareTo(BigDecimal.ZERO) > 0
                            && p.getStockQuantity() != null
                            && p.getStockQuantity().compareTo(p.getMinStock()) <= 0) {

                        String refKey = "stock-product-" + p.getProductId();
                        String msg = "Stock bajo: " + p.getProductName()
                                + " (" + p.getStockQuantity() + "/" + p.getMinStock() + ")";
                        notificationService.createIfNotExists(
                                NotificationType.STOCK_ALERT,
                                msg,
                                "/stock",
                                refKey);
                    }
                }
            } catch (Exception e) {
                logger.error("Error checking stock alerts: {}", e.getMessage());
            }
        });
    }


    @Scheduled(fixedRate = 1800000)
    public void checkUninvoicedSales() {
        runForAllTenants(() -> {
            try {
                List<Sale> sales = saleRepository.findAll();
                for (Sale s : sales) {
                    if (s.getFacturado() == null || !s.getFacturado()) {
                        String refKey = "uninvoiced-sale-" + s.getSaleId();
                        String msg = "Venta #" + s.getSaleId() + " sin facturar ($" + s.getTotalAmount() + ")";
                        notificationService.createIfNotExists(
                                NotificationType.UNINVOICED_SALE,
                                msg,
                                "/ventas",
                                refKey);
                    }
                }
            } catch (Exception e) {
                logger.error("Error checking uninvoiced sales: {}", e.getMessage());
            }
        });
    }

    private void runForAllTenants(Runnable action) {
        List<Company> companies = companyRepository.findAll();
        for (Company company : companies) {
            String schema = company.getTenantSchema();
            if (schema == null || schema.isBlank())
                continue;
            TenantContext.setCurrentTenant(schema);
            try {
                action.run();
            } catch (Exception e) {
                logger.error("Error running scheduled task for tenant {}: {}", schema, e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
