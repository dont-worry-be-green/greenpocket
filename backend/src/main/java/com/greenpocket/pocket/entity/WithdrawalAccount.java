package com.greenpocket.pocket.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "withdrawal_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawalAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "bank_code", nullable = false, length = 10)
	private String bankCode;

	@Column(name = "bank_name", nullable = false, length = 30)
	private String bankName;

	@Column(name = "account_no_encrypted", nullable = false, length = 512)
	private byte[] encryptedAccountNumber;

	@Column(name = "holder", nullable = false, length = 30)
	private String holder;

	@Column(name = "is_default", nullable = false)
	private boolean isDefault;

	@Column(name = "is_active", nullable = false)
	private boolean isActive;

	@Column(name = "verified_at")
	private LocalDateTime verifiedAt;

	@Column(name = "default_slot")
	private Long defaultSlot;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	private WithdrawalAccount(
		Long userId,
		String bankCode,
		String bankName,
		byte[] encryptedAccountNumber,
		String holder,
		boolean isDefault
	) {
		this.userId = userId;
		this.bankCode = bankCode;
		this.bankName = bankName;
		this.encryptedAccountNumber = encryptedAccountNumber.clone();
		this.holder = holder;
		this.isDefault = isDefault;
		this.isActive = true;
		this.defaultSlot = isDefault ? userId : null;
	}

	public static WithdrawalAccount create(
		Long userId,
		String bankCode,
		String bankName,
		byte[] encryptedAccountNumber,
		String holder,
		boolean isDefault
	) {
		return new WithdrawalAccount(
			userId,
			bankCode,
			bankName,
			encryptedAccountNumber,
			holder,
			isDefault
		);
	}

	public void update(
		String bankCode,
		String bankName,
		byte[] encryptedAccountNumber,
		String holder
	) {
		this.bankCode = bankCode;
		this.bankName = bankName;
		this.encryptedAccountNumber = encryptedAccountNumber.clone();
		this.holder = holder;
	}

	public void makeDefault() {
		this.isDefault = true;
		this.defaultSlot = userId;
	}

	public void deactivate() {
		this.isActive = false;
		this.isDefault = false;
		this.defaultSlot = null;
	}

	public byte[] getEncryptedAccountNumber() {
		return encryptedAccountNumber.clone();
	}
}
