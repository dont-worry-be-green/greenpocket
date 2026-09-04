package com.greenpocket.pocket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.pocket.crypto.AccountNumberCipher;
import com.greenpocket.pocket.dto.WithdrawalAccountCreateRequest;
import com.greenpocket.pocket.dto.WithdrawalAccountDefaultResponse;
import com.greenpocket.pocket.dto.WithdrawalAccountListResponse;
import com.greenpocket.pocket.dto.WithdrawalAccountResponse;
import com.greenpocket.pocket.dto.WithdrawalAccountUpdateRequest;
import com.greenpocket.pocket.entity.WithdrawalAccount;
import com.greenpocket.pocket.exception.PocketErrorCode;
import com.greenpocket.pocket.repository.WithdrawalAccountRepository;

class WithdrawalAccountServiceTest {

	private static final Long USER_ID = 42L;
	private static final Long ACCOUNT_ID = 3L;
	private static final byte[] ENCRYPTED_ACCOUNT_NUMBER = {1, 2, 3};

	private WithdrawalAccountRepository withdrawalAccountRepository;
	private AccountNumberCipher accountNumberCipher;
	private WithdrawalAccountService withdrawalAccountService;

	@BeforeEach
	void setUp() {
		withdrawalAccountRepository = mock(WithdrawalAccountRepository.class);
		accountNumberCipher = mock(AccountNumberCipher.class);
		withdrawalAccountService = new WithdrawalAccountService(
			withdrawalAccountRepository,
			accountNumberCipher
		);
	}

	@Test
	void returnsEmptyAccountList() {
		when(withdrawalAccountRepository.findByUserIdAndIsActiveTrueOrderByIsDefaultDescIdAsc(USER_ID))
			.thenReturn(List.of());

		WithdrawalAccountListResponse response = withdrawalAccountService.findAccounts(USER_ID);

		assertThat(response.accounts()).isEmpty();
	}

	@Test
	void returnsDecryptedActiveAccounts() {
		WithdrawalAccount account = account(ACCOUNT_ID, true);
		when(withdrawalAccountRepository.findByUserIdAndIsActiveTrueOrderByIsDefaultDescIdAsc(USER_ID))
			.thenReturn(List.of(account));
		when(accountNumberCipher.decrypt(ENCRYPTED_ACCOUNT_NUMBER)).thenReturn("110-123-456789");

		WithdrawalAccountListResponse response = withdrawalAccountService.findAccounts(USER_ID);

		assertThat(response.accounts()).singleElement().satisfies(item -> {
			assertThat(item.accountId()).isEqualTo(ACCOUNT_ID);
			assertThat(item.accountNo()).isEqualTo("110-123-456789");
			assertThat(item.isDefault()).isTrue();
			assertThat(item.verifiedAt()).isNull();
		});
	}

	@Test
	void createsDefaultAccountAfterClearingPreviousDefault() {
		WithdrawalAccountCreateRequest request = new WithdrawalAccountCreateRequest(
			"088",
			"신한은행",
			"110-123-456789",
			"김수현",
			true
		);
		when(accountNumberCipher.encrypt(request.accountNo())).thenReturn(ENCRYPTED_ACCOUNT_NUMBER);
		when(accountNumberCipher.decrypt(ENCRYPTED_ACCOUNT_NUMBER)).thenReturn(request.accountNo());
		when(withdrawalAccountRepository.save(any(WithdrawalAccount.class)))
			.thenAnswer(invocation -> invocation.<WithdrawalAccount>getArgument(0));

		WithdrawalAccountResponse response = withdrawalAccountService.createAccount(USER_ID, request);

		verify(withdrawalAccountRepository).clearDefaultByUserId(USER_ID);
		assertThat(response.accountNo()).isEqualTo(request.accountNo());
		assertThat(response.isDefault()).isTrue();
	}

	@Test
	void createsNonDefaultAccountWithoutClearingDefault() {
		WithdrawalAccountCreateRequest request = new WithdrawalAccountCreateRequest(
			"088",
			"신한은행",
			"110-123-456789",
			"김수현",
			false
		);
		when(accountNumberCipher.encrypt(request.accountNo())).thenReturn(ENCRYPTED_ACCOUNT_NUMBER);
		when(accountNumberCipher.decrypt(ENCRYPTED_ACCOUNT_NUMBER)).thenReturn(request.accountNo());
		when(withdrawalAccountRepository.save(any(WithdrawalAccount.class)))
			.thenAnswer(invocation -> invocation.<WithdrawalAccount>getArgument(0));

		WithdrawalAccountResponse response = withdrawalAccountService.createAccount(USER_ID, request);

		verify(withdrawalAccountRepository, never()).clearDefaultByUserId(any());
		assertThat(response.isDefault()).isFalse();
	}

	@Test
	void updatesOwnedActiveAccount() {
		WithdrawalAccount account = account(ACCOUNT_ID, false);
		WithdrawalAccountUpdateRequest request = new WithdrawalAccountUpdateRequest(
			"004",
			"KB국민은행",
			"123-456-789012",
			"김수현"
		);
		when(withdrawalAccountRepository.findByIdAndUserIdAndIsActiveTrue(ACCOUNT_ID, USER_ID))
			.thenReturn(Optional.of(account));
		when(accountNumberCipher.encrypt(request.accountNo())).thenReturn(ENCRYPTED_ACCOUNT_NUMBER);
		when(accountNumberCipher.decrypt(ENCRYPTED_ACCOUNT_NUMBER)).thenReturn(request.accountNo());

		WithdrawalAccountResponse response = withdrawalAccountService.updateAccount(USER_ID, ACCOUNT_ID, request);

		verify(account).update("004", "KB국민은행", ENCRYPTED_ACCOUNT_NUMBER, "김수현");
		assertThat(response.accountNo()).isEqualTo(request.accountNo());
	}

	@Test
	void makesAccountDefaultAndReturnsPreviousDefaultId() {
		WithdrawalAccount target = account(ACCOUNT_ID, false);
		WithdrawalAccount refreshedTarget = account(ACCOUNT_ID, false);
		WithdrawalAccount previousDefault = account(2L, true);
		when(withdrawalAccountRepository.findByIdAndUserIdAndIsActiveTrue(ACCOUNT_ID, USER_ID))
			.thenReturn(Optional.of(target))
			.thenReturn(Optional.of(refreshedTarget));
		when(withdrawalAccountRepository.findFirstByUserIdAndIsDefaultTrueAndIsActiveTrue(USER_ID))
			.thenReturn(Optional.of(previousDefault));

		WithdrawalAccountDefaultResponse response = withdrawalAccountService.makeDefault(USER_ID, ACCOUNT_ID);

		verify(withdrawalAccountRepository).clearDefaultByUserId(USER_ID);
		verify(refreshedTarget).makeDefault();
		assertThat(response.accountId()).isEqualTo(ACCOUNT_ID);
		assertThat(response.previousDefaultAccountId()).isEqualTo(2L);
	}

	@Test
	void rejectsMissingOrOtherUsersAccount() {
		when(withdrawalAccountRepository.findByIdAndUserIdAndIsActiveTrue(ACCOUNT_ID, USER_ID))
			.thenReturn(Optional.empty());
		WithdrawalAccountUpdateRequest request = new WithdrawalAccountUpdateRequest(
			"004",
			"KB국민은행",
			"123-456-789012",
			"김수현"
		);

		assertThatThrownBy(() -> withdrawalAccountService.updateAccount(USER_ID, ACCOUNT_ID, request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(PocketErrorCode.POCKET_ACCOUNT_NOT_FOUND));
	}

	@Test
	void rejectsMakingMissingOrOtherUsersAccountDefault() {
		when(withdrawalAccountRepository.findByIdAndUserIdAndIsActiveTrue(ACCOUNT_ID, USER_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> withdrawalAccountService.makeDefault(USER_ID, ACCOUNT_ID))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(PocketErrorCode.POCKET_ACCOUNT_NOT_FOUND));
	}

	private WithdrawalAccount account(Long accountId, boolean isDefault) {
		WithdrawalAccount account = mock(WithdrawalAccount.class);
		when(account.getId()).thenReturn(accountId);
		when(account.getUserId()).thenReturn(USER_ID);
		when(account.getBankCode()).thenReturn("088");
		when(account.getBankName()).thenReturn("신한은행");
		when(account.getEncryptedAccountNumber()).thenReturn(ENCRYPTED_ACCOUNT_NUMBER);
		when(account.getHolder()).thenReturn("김수현");
		when(account.isDefault()).thenReturn(isDefault);
		when(account.isActive()).thenReturn(true);
		return account;
	}
}
