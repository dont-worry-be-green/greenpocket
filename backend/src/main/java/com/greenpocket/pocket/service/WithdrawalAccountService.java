package com.greenpocket.pocket.service;

import java.time.ZoneId;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WithdrawalAccountService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

	private final WithdrawalAccountRepository withdrawalAccountRepository;
	private final AccountNumberCipher accountNumberCipher;

	public WithdrawalAccountListResponse findAccounts(Long userId) {
		List<WithdrawalAccountResponse> accounts = withdrawalAccountRepository
			.findByUserIdAndIsActiveTrueOrderByIsDefaultDescIdAsc(userId)
			.stream()
			.map(this::toResponse)
			.toList();
		return new WithdrawalAccountListResponse(accounts);
	}

	@Transactional
	public WithdrawalAccountResponse createAccount(
		Long userId,
		WithdrawalAccountCreateRequest request
	) {
		if (request.isDefault()) {
			withdrawalAccountRepository.clearDefaultByUserId(userId);
		}

		WithdrawalAccount account = WithdrawalAccount.create(
			userId,
			request.bankCode(),
			request.bankName(),
			accountNumberCipher.encrypt(request.accountNo()),
			request.holder(),
			request.isDefault()
		);
		return toResponse(withdrawalAccountRepository.save(account));
	}

	@Transactional
	public WithdrawalAccountResponse updateAccount(
		Long userId,
		Long accountId,
		WithdrawalAccountUpdateRequest request
	) {
		WithdrawalAccount account = findActiveAccount(userId, accountId);
		account.update(
			request.bankCode(),
			request.bankName(),
			accountNumberCipher.encrypt(request.accountNo()),
			request.holder()
		);
		return toResponse(account);
	}

	@Transactional
	public WithdrawalAccountDefaultResponse makeDefault(Long userId, Long accountId) {
		WithdrawalAccount target = findActiveAccount(userId, accountId);
		Long previousDefaultAccountId = withdrawalAccountRepository
			.findFirstByUserIdAndIsDefaultTrueAndIsActiveTrue(userId)
			.map(WithdrawalAccount::getId)
			.filter(previousId -> !previousId.equals(target.getId()))
			.orElse(null);

		withdrawalAccountRepository.clearDefaultByUserId(userId);
		WithdrawalAccount refreshedTarget = findActiveAccount(userId, accountId);
		refreshedTarget.makeDefault();

		return new WithdrawalAccountDefaultResponse(
			refreshedTarget.getId(),
			true,
			previousDefaultAccountId
		);
	}

	@Transactional
	public void deleteAccount(Long userId, Long accountId) {
		WithdrawalAccount account = findActiveAccount(userId, accountId);
		account.deactivate();
	}

	private WithdrawalAccount findActiveAccount(Long userId, Long accountId) {
		return withdrawalAccountRepository.findByIdAndUserIdAndIsActiveTrue(accountId, userId)
			.orElseThrow(() -> new BusinessException(PocketErrorCode.POCKET_ACCOUNT_NOT_FOUND));
	}

	private WithdrawalAccountResponse toResponse(WithdrawalAccount account) {
		return new WithdrawalAccountResponse(
			account.getId(),
			account.getBankCode(),
			account.getBankName(),
			accountNumberCipher.decrypt(account.getEncryptedAccountNumber()),
			account.getHolder(),
			account.isDefault(),
			account.isActive(),
			account.getVerifiedAt() == null
				? null
				: account.getVerifiedAt().atZone(KOREA_ZONE_ID).toOffsetDateTime()
		);
	}
}
