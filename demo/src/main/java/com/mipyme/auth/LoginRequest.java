package com.mipyme.auth;

public record LoginRequest(String ssoCode, String username, String password) {
}
