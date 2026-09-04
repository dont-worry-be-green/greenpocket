package com.greenpocket.user.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.bill.service.BillExistenceQueryService;
import com.greenpocket.eco.service.EcoCurrentRoundQueryService;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.user.dto.UserBootstrapResponse;
import com.greenpocket.user.dto.UserStartRequest;
import com.greenpocket.user.dto.UserStartResponse;
import com.greenpocket.user.exception.UserErrorCode;
import com.greenpocket.user.repository.UserRepository;
import com.greenpocket.user.repository.UserRepository.UserSnapshot;

@Service
@RequiredArgsConstructor
public class UserService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 20;
	private static final String ONBOARDING_SCREEN = "ONB-02";
	private static final String START_SCREEN = "ONB-01";
	private static final String HOME_SCREEN = "WF-06";

	private final UserRepository userRepository;
	private final PocketAccountNumberGenerator accountNumberGenerator;
	private final BillExistenceQueryService billExistenceQueryService;
	private final EcoCurrentRoundQueryService ecoCurrentRoundQueryService;

	@Transactional
	public UserStartResult start(UserStartRequest request) {
		validateDemoKey(request.demoKey());
		String name = normalizeAndValidateName(request.name());

		Optional<UserSnapshot> existingUser = userRepository.findByDemoKey(request.demoKey());
		if (existingUser.isPresent()) {
			return new UserStartResult(toStartResponse(existingUser.get()), false);
		}

		for (int attempt = 0; attempt < MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
			String accountNo = accountNumberGenerator.generate();
			if (userRepository.existsByPocketAccountNo(accountNo)) {
				continue;
			}

			try {
				userRepository.create(request.demoKey(), name, accountNo);
				UserSnapshot createdUser = userRepository.findByDemoKey(request.demoKey())
					.orElseThrow(UserService::internalError);
				return new UserStartResult(toStartResponse(createdUser), true);
			}
			catch (DuplicateKeyException exception) {
				Optional<UserSnapshot> concurrentUser = userRepository.findByDemoKey(request.demoKey());
				if (concurrentUser.isPresent()) {
					return new UserStartResult(toStartResponse(concurrentUser.get()), false);
				}
			}
		}

		throw internalError();
	}

	@Transactional(readOnly = true)
	public UserBootstrapResponse getBootstrap(Long userId) {
		UserSnapshot user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED_DEMO_KEY));
		boolean hasBill = billExistenceQueryService.existsByUserId(userId);
		Long currentRoundId = ecoCurrentRoundQueryService.findCurrentRoundId(userId).orElse(null);

		return new UserBootstrapResponse(
			user.id(),
			user.name(),
			user.onboardingCompleted(),
			user.ecoLinkStatus(),
			toOffsetDateTime(user.ecoLinkedAt()),
			user.greenlifeParticipating(),
			toOffsetDateTime(user.greenlifeLinkedAt()),
			hasBill,
			currentRoundId,
			user.onboardingCompleted() ? HOME_SCREEN : START_SCREEN
		);
	}

	private void validateDemoKey(String demoKey) {
		try {
			UUID uuid = UUID.fromString(demoKey);
			if (uuid.version() != 4 || !uuid.toString().equalsIgnoreCase(demoKey)) {
				throw new IllegalArgumentException();
			}
		}
		catch (RuntimeException exception) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "demoKey", null);
		}
	}

	private String normalizeAndValidateName(String rawName) {
		String name = rawName == null ? "" : rawName.strip();
		int characterCount = name.codePointCount(0, name.length());
		boolean hasLetterOrDigit = name.codePoints().anyMatch(Character::isLetterOrDigit);
		if (characterCount < 1 || characterCount > 20 || !hasLetterOrDigit) {
			throw new BusinessException(UserErrorCode.NAME_INVALID, "name", null);
		}
		return name;
	}

	private UserStartResponse toStartResponse(UserSnapshot user) {
		return new UserStartResponse(
			user.id(),
			user.name(),
			user.onboardingCompleted(),
			ONBOARDING_SCREEN,
			user.pocketAccountNo(),
			user.pocketHolder(),
			toOffsetDateTime(user.createdAt())
		);
	}

	private static OffsetDateTime toOffsetDateTime(java.time.LocalDateTime value) {
		return value == null ? null : value.atZone(KOREA_ZONE_ID).toOffsetDateTime();
	}

	private static BusinessException internalError() {
		return new BusinessException(CommonErrorCode.INTERNAL_ERROR);
	}

	public record UserStartResult(UserStartResponse response, boolean created) {
	}
}
