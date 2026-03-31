package com.mipyme.auth;

public record AuthResponse(String token, String refreshToken, String companyId, String role, String companyName, String name, String permissions, boolean requiresTwoFactor, String theme, String planTier, String planStatus) {
}
