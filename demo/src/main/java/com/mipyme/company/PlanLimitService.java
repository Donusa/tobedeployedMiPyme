package com.mipyme.company;

import com.mipyme.caja.model.CajaEstado;
import com.mipyme.caja.repository.CajaDiariaRepository;
import com.mipyme.tenant.TenantContext;
import com.mipyme.user.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;


@Service
public class PlanLimitService {

    private static final Logger logger = LoggerFactory.getLogger(PlanLimitService.class);


    public static final int UNLIMITED = -1;

    private final CompanyRepository companyRepository;
    private final AppUserRepository userRepository;
    private final CajaDiariaRepository cajaDiariaRepository;

    public PlanLimitService(
            CompanyRepository companyRepository,
            AppUserRepository userRepository,
            CajaDiariaRepository cajaDiariaRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.cajaDiariaRepository = cajaDiariaRepository;
    }



    private boolean isPro(String tier) {
        return "pro".equalsIgnoreCase(tier) || "enterprise".equalsIgnoreCase(tier);
    }

    private boolean isEnterprise(String tier) {
        return "enterprise".equalsIgnoreCase(tier);
    }




    public void assertPlanAccess(String tenantId, PlanFeature feature) {
        Company company = resolveCompany(tenantId);
        String tier = company.getPlanTier();

        boolean allowed = switch (feature) {
            case FACTURACION_ARCA -> isPro(tier);
            case FREQUENT_CUSTOMERS -> isPro(tier);
            case METRICAS_AVANZADAS, AUDITORIA_AVANZADA,
                 BRANCH_MANAGEMENT, WAREHOUSE_MANAGEMENT,
                 ADVANCED_PERMISSIONS, EXPORTACIONES -> isEnterprise(tier);
        };

        if (!allowed) {
            String requiredTier = isEnterpriseFeature(feature) ? "enterprise" : "pro";
            throw new PlanAccessException(
                    String.format("Tu plan %s no incluye esta funcionalidad. Actualizá a %s para acceder.",
                            tier != null ? tier : "base", requiredTier));
        }
    }

    private boolean isEnterpriseFeature(PlanFeature feature) {
        return switch (feature) {
            case METRICAS_AVANZADAS, AUDITORIA_AVANZADA,
                 BRANCH_MANAGEMENT, WAREHOUSE_MANAGEMENT,
                 ADVANCED_PERMISSIONS, EXPORTACIONES -> true;
            default -> false;
        };
    }




    public void assertResourceLimit(String tenantId, ResourceType resource) {
        Company company = resolveCompany(tenantId);
        String tier = company.getPlanTier();

        int limit = getLimit(tier, resource);
        if (limit == UNLIMITED) return;

        long current = countCurrentUsage(tenantId, resource);
        if (current >= limit) {
            String tierLabel = tier != null ? tier : "base";
            String nextTier = "pro".equalsIgnoreCase(tierLabel) ? "Enterprise" : "Pro";
            throw new PlanLimitException(
                    String.format("Tu plan %s permite hasta %d %s. Actualizá a %s para agregar más.",
                            tierLabel, limit, resource.name().toLowerCase(), nextTier));
        }
    }




    public void applyDowngradePolicies(String tenantId) {
        Company company = resolveCompany(tenantId);
        String tier = company.getPlanTier();


        int maxCajas = getLimit(tier, ResourceType.CAJAS);
        if (maxCajas != UNLIMITED) {



            logger.info("Tenant {} downgraded to {}. maxCajas={}. Manual review may be required for excess active cajas.",
                    tenantId, tier, maxCajas);
        }
    }




    public PlanLimitsResponse getCurrentLimitsAndUsage(String tenantId) {
        Company company = resolveCompany(tenantId);
        String tier = company.getPlanTier() != null ? company.getPlanTier() : "base";

        PlanLimitsResponse.Limits limits = new PlanLimitsResponse.Limits(
                getLimit(tier, ResourceType.USERS),
                getLimit(tier, ResourceType.SUCURSALES),
                getLimit(tier, ResourceType.DEPOSITOS),
                getLimit(tier, ResourceType.CAJAS)
        );

        PlanLimitsResponse.Usage usage = new PlanLimitsResponse.Usage(
                countCurrentUsage(tenantId, ResourceType.USERS),
                countCurrentUsage(tenantId, ResourceType.SUCURSALES),
                countCurrentUsage(tenantId, ResourceType.DEPOSITOS),
                countCurrentUsage(tenantId, ResourceType.CAJAS)
        );

        return new PlanLimitsResponse(tier, limits, usage);
    }




    public int getLimit(String tier, ResourceType resource) {
        if (tier == null) tier = "base";
        return switch (tier.toLowerCase()) {
            case "enterprise" -> UNLIMITED;
            case "pro" -> switch (resource) {
                case USERS -> 5;
                case SUCURSALES -> 2;
                case DEPOSITOS -> 1;
                case CAJAS -> UNLIMITED;
            };
            default -> switch (resource) {
                case USERS -> 3;
                case SUCURSALES -> 1;
                case DEPOSITOS -> 1;
                case CAJAS -> 1;
            };
        };
    }

    private long countCurrentUsage(String tenantId, ResourceType resource) {
        return switch (resource) {
            case USERS -> userRepository.count();
            case CAJAS -> {


                List<CajaEstado> activeStates = Arrays.asList(CajaEstado.ABIERTA, CajaEstado.EN_RELEVO);
                yield cajaDiariaRepository.findAll().stream()
                        .filter(c -> activeStates.contains(c.getEstado()))
                        .count();
            }


            case SUCURSALES -> 1L;
            case DEPOSITOS -> 1L;
        };
    }

    private Company resolveCompany(String tenantId) {
        Optional<Company> opt = companyRepository.findByTenantSchema(tenantId);
        if (opt.isEmpty()) {
            opt = companyRepository.findByCompanyId(tenantId);
        }
        if (opt.isEmpty()) {

            String currentTenant = TenantContext.getCurrentTenant();
            if (currentTenant != null && !currentTenant.equals(tenantId)) {
                opt = companyRepository.findByTenantSchema(currentTenant);
            }
        }
        return opt.orElseThrow(() -> new IllegalStateException("Company not found for tenant: " + tenantId));
    }
}
