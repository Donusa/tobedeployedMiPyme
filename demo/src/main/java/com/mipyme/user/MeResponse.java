package com.mipyme.user;

public record MeResponse(
		String id,
		String username,
		String name,
		String email,
		String role,
		CompanySummary company
) {
	public record CompanySummary(
			String ssoCode,
			String businessName,
			String tradeName,
			String cuit
	) {}
}
