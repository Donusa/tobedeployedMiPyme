package com.mipyme.company;

public record CompanyRegisterRequest(
		String businessName,
		String tradeName,
		String cuit,
		String country,
		String province,
		String city,
		String companyEmail,
		String phone,
		String fiscalAddress,
		String ssoCode,
		String adminName,
		String adminEmail,
		String adminPassword,
		boolean termsAccepted,
		String planTier
) {
}
