package com.mipyme.company;

public record CompanyUpdateRequest(
    String businessName,
    String tradeName,
    String cuit,
    String country,
    String province,
    String city,
    String industry,
    String companyEmail,
    String phone,
    String fiscalAddress
) {}
