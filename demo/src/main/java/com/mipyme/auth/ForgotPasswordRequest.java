package com.mipyme.auth;

public record ForgotPasswordRequest(String ssoCode, String email) {
}
