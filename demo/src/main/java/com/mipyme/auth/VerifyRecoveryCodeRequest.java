package com.mipyme.auth;

public record VerifyRecoveryCodeRequest(String ssoCode, String email, String code) {
}
