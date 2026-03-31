package com.mipyme.user;

public enum UserRole {
	ADMIN,
	EMPLOYEE;

	public static UserRole fromString(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim().toUpperCase();
		if (normalized.isEmpty()) {
			return null;
		}
		return UserRole.valueOf(normalized);
	}
}

