package com.greenpocket.pocket.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WithdrawalAccountTest {

	@Test
	void synchronizesDefaultSlotWhenCreatingDefaultAccount() {
		WithdrawalAccount account = WithdrawalAccount.create(
			42L,
			"088",
			"신한은행",
			new byte[] {1, 2, 3},
			"김수현",
			true
		);

		assertThat(account.isDefault()).isTrue();
		assertThat(account.getDefaultSlot()).isEqualTo(42L);
	}

	@Test
	void synchronizesDefaultSlotWhenMakingAccountDefault() {
		WithdrawalAccount account = WithdrawalAccount.create(
			42L,
			"088",
			"신한은행",
			new byte[] {1, 2, 3},
			"김수현",
			false
		);

		account.makeDefault();

		assertThat(account.isDefault()).isTrue();
		assertThat(account.getDefaultSlot()).isEqualTo(42L);
	}

	@Test
	void deactivatesAccountAndClearsDefaultSlot() {
		WithdrawalAccount account = WithdrawalAccount.create(
			42L,
			"088",
			"신한은행",
			new byte[] {1, 2, 3},
			"김수현",
			true
		);

		account.deactivate();

		assertThat(account.isActive()).isFalse();
		assertThat(account.isDefault()).isFalse();
		assertThat(account.getDefaultSlot()).isNull();
	}
}
