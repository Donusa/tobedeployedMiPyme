package com.mipyme.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(nullable = false)
	private String role;

	@Column(columnDefinition = "TEXT")
	private String permissions;

	@Column(name = "warehouse_ids", columnDefinition = "TEXT")
	private String warehouseIds;

	@Column(name = "is_two_factor_enabled")
	private boolean isTwoFactorEnabled = false;

	@Column(name = "two_factor_code")
	private String twoFactorCode;

	@Column(name = "two_factor_expires_at")
	private java.time.LocalDateTime twoFactorCodeExpiresAt;

	@Column(name = "password_recovery_code")
	private String passwordRecoveryCode;

	@Column(name = "password_recovery_expires_at")
	private java.time.LocalDateTime passwordRecoveryExpiresAt;

	@Column(name = "theme", nullable = false)
	private String theme = "light";

	protected AppUser() {
	}

	public AppUser(String username, String name, String email, String passwordHash, String role, String permissions) {
		this.username = username;
		this.name = name;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
		this.permissions = permissions;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getRole() {
		return role;
	}

	public String getPermissions() {
		return permissions;
	}

	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}

	public String getWarehouseIds() {
		return warehouseIds;
	}

	public void setWarehouseIds(String warehouseIds) {
		this.warehouseIds = warehouseIds;
	}

	public boolean isTwoFactorEnabled() {
		return isTwoFactorEnabled;
	}

	public void setTwoFactorEnabled(boolean isTwoFactorEnabled) {
		this.isTwoFactorEnabled = isTwoFactorEnabled;
	}

	public String getTwoFactorCode() {
		return twoFactorCode;
	}

	public void setTwoFactorCode(String twoFactorCode) {
		this.twoFactorCode = twoFactorCode;
	}

	public java.time.LocalDateTime getTwoFactorCodeExpiresAt() {
		return twoFactorCodeExpiresAt;
	}

	public void setTwoFactorCodeExpiresAt(java.time.LocalDateTime twoFactorCodeExpiresAt) {
		this.twoFactorCodeExpiresAt = twoFactorCodeExpiresAt;
	}

	public String getPasswordRecoveryCode() {
		return passwordRecoveryCode;
	}

	public void setPasswordRecoveryCode(String passwordRecoveryCode) {
		this.passwordRecoveryCode = passwordRecoveryCode;
	}

	public java.time.LocalDateTime getPasswordRecoveryExpiresAt() {
		return passwordRecoveryExpiresAt;
	}

	public void setPasswordRecoveryExpiresAt(java.time.LocalDateTime passwordRecoveryExpiresAt) {
		this.passwordRecoveryExpiresAt = passwordRecoveryExpiresAt;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getTheme() {
		return theme != null ? theme : "light";
	}

	public void setTheme(String theme) {
		this.theme = theme;
	}
}
