package com.greenpocket.user.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.bill.service.BillExistenceQueryService;
import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.eco.service.EcoCurrentRoundQueryService;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.user.dto.UserBootstrapResponse;
import com.greenpocket.user.dto.UserStartRequest;
import com.greenpocket.user.dto.UserStartResponse;
import com.greenpocket.user.entity.Gender;
import com.greenpocket.user.exception.UserErrorCode;
import com.greenpocket.user.repository.UserRepository;
import com.greenpocket.user.repository.UserRepository.UserSnapshot;

@Service
@RequiredArgsConstructor
public class UserService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 20;
	private static final String ECO_LINK_SCREEN = "WF-01";
	private static final String ECO_LINKING_SCREEN = "WF-02";
	private static final String HOME_SCREEN = "WF-06";
	private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^01[016789][0-9]{7,8}$");

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
			.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED));
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
			entryScreen(user.ecoLinkStatus())
		);
	}

	@Transactional
	public RegisteredUser createRegisteredUser(
		String rawName,
		LocalDate birthDate,
		Gender gender,
		String rawPhoneNumber
	) {
		String name = normalizeAndValidateName(rawName);
		validateBirthDate(birthDate);
		if (gender == null) {
			throw new BusinessException(UserErrorCode.GENDER_REQUIRED, "gender", null);
		}
		String phoneNumber = normalizeAndValidatePhoneNumber(rawPhoneNumber);
		if (userRepository.existsByPhoneNumber(phoneNumber)) {
			throw new BusinessException(UserErrorCode.PHONE_NUMBER_ALREADY_USED, "phoneNumber", null);
		}
		for (int attempt = 0; attempt < MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
			String accountNo = accountNumberGenerator.generate();
			if (userRepository.existsByPocketAccountNo(accountNo)) {
				continue;
			}

			try {
				userRepository.createRegistered(name, birthDate, gender, phoneNumber, accountNo);
				UserSnapshot createdUser = userRepository.findByPocketAccountNo(accountNo)
					.orElseThrow(UserService::internalError);
				return new RegisteredUser(
					createdUser.id(),
					createdUser.name(),
					createdUser.onboardingCompleted()
				);
			}
			catch (DuplicateKeyException exception) {
				if (userRepository.existsByPhoneNumber(phoneNumber)) {
					throw new BusinessException(
						UserErrorCode.PHONE_NUMBER_ALREADY_USED, "phoneNumber", null
					);
				}
				// 포켓 계좌번호 충돌은 새 번호를 생성해 다시 시도한다.
			}
		}

		throw internalError();
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

	private static void validateBirthDate(LocalDate birthDate) {
		if (birthDate == null || birthDate.isAfter(LocalDate.now(KOREA_ZONE_ID))) {
			throw new BusinessException(UserErrorCode.BIRTH_DATE_INVALID, "birthDate", null);
		}
	}

	private static String normalizeAndValidatePhoneNumber(String rawPhoneNumber) {
		String phoneNumber = rawPhoneNumber == null ? "" : rawPhoneNumber.replaceAll("[^0-9]", "");
		if (!PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches()) {
			throw new BusinessException(UserErrorCode.PHONE_NUMBER_INVALID, "phoneNumber", null);
		}
		return phoneNumber;
	}

	private UserStartResponse toStartResponse(UserSnapshot user) {
		return new UserStartResponse(
			user.id(),
			user.name(),
			user.onboardingCompleted(),
			entryScreen(user.ecoLinkStatus()),
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

	private static String entryScreen(EcoLinkStatus status) {
		return switch (status) {
			case LINKING -> ECO_LINKING_SCREEN;
			case LINKED -> HOME_SCREEN;
			case UNLINKED, FAILED -> ECO_LINK_SCREEN;
		};
	}

	public record UserStartResult(UserStartResponse response, boolean created) {
	}

	public record RegisteredUser(Long userId, String name, boolean onboardingCompleted) {
	}
}
