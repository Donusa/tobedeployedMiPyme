package com.mipyme.auth;

public record ResetPasswordRequest(String ssoCode, String email, String code, String newPassword) {
}
