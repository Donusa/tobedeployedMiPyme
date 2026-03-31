package com.mipyme.user;

public record ChangeEmailRequest(String currentEmail, String newEmail, String password) {
}
