package com.mipyme.auth;

public record RegisterRequest(String ssoCode, String name, String email, String password, String role) {
}
