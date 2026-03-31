package com.mipyme.auth;

public record VerifyTwoFactorRequest(String ssoCode, String username, String code) {
}
