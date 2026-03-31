package com.mipyme.company;

public record CompanyResponse(
    String companyId,
    String ssoCode,
    String businessName,
    String tradeName,
    String cuit,
    String country,
    String province,
    String city,
    String companyEmail,
    String phone,
    String fiscalAddress,
    String tenantSchema,
    String planStatus,
    String planTier,
    String validUntil,
    String graceUntil
) {}
