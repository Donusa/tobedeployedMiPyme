package com.mipyme.company;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Company> getCompanyByTenant(String tenantSchema) {
        return companyRepository.findByTenantSchema(tenantSchema);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<Company> findById(String companyId) {
        return companyRepository.findByCompanyId(companyId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Company updateCompany(String tenantSchema, CompanyUpdateRequest request) {
        Company company = companyRepository.findByTenantSchema(tenantSchema).orElse(null);

        if (company == null) {
            return null;
        }

        if (request.tradeName() != null && !request.tradeName().isBlank()) {
            company.setName(request.tradeName());
        }

        if (request.businessName() != null) company.setBusinessName(request.businessName());

        if (request.cuit() != null) {
            String newCuit = request.cuit().trim();
            if (!newCuit.isBlank() && !newCuit.equals(company.getCuit())) {
                if (companyRepository.existsByCuit(newCuit)) {
                    throw new IllegalStateException("CUIT already exists");
                }
            }
            company.setCuit(request.cuit());
        }

        if (request.country() != null) company.setCountry(request.country());
        if (request.province() != null) company.setProvince(request.province());
        if (request.city() != null) company.setCity(request.city());
        if (request.companyEmail() != null) company.setEmail(request.companyEmail());
        if (request.phone() != null) company.setPhone(request.phone());
        if (request.fiscalAddress() != null) company.setFiscalAddress(request.fiscalAddress());

        return companyRepository.save(company);
    }
}
