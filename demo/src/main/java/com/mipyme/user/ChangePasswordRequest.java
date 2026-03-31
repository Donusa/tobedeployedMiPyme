package com.mipyme.user;

public record ChangePasswordRequest(String currentPassword, String newPassword) {
}
