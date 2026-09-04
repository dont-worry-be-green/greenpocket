package com.greenpocket.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import com.greenpocket.bill.service.BillExistenceQueryService;
import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.eco.service.EcoCurrentRoundQueryService;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.user.dto.UserBootstrapResponse;
import com.greenpocket.user.dto.UserStartRequest;
import com.greenpocket.user.repository.UserRepository;
import com.greenpocket.user.repository.UserRepository.UserSnapshot;

class UserServiceTest {

	private static final String DEMO_KEY = "9f2c1a7e-4b30-4c88-9a11-6d0e5b7c2f41";
	private static final String ACCOUNT_NO = "1005-1234-5678-90";

	private UserRepository userRepository;
	private PocketAccountNumberGenerator accountNumberGenerator;
	private BillExistenceQueryService billExistenceQueryService;
	private EcoCurrentRoundQueryService ecoCurrentRoundQueryService;
	private UserService userService;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		accountNumberGenerator = mock(PocketAccountNumberGenerator.class);
		billExistenceQueryService = mock(BillExistenceQueryService.class);
		ecoCurrentRoundQueryService = mock(EcoCurrentRoundQueryService.class);
		userService = new UserService(
			userRepository,
			accountNumberGenerator,
			billExistenceQueryService,
			ecoCurrentRoundQueryService
		);
	}

	@Test
	void createsUserWithTrimmedNameAndPocketAccount() {
		UserSnapshot createdUser = user(false);
		when(userRepository.findByDemoKey(DEMO_KEY))
			.thenReturn(Optional.empty(), Optional.of(createdUser));
		when(accountNumberGenerator.generate()).thenReturn(ACCOUNT_NO);
		when(userRepository.existsByPocketAccountNo(ACCOUNT_NO)).thenReturn(false);

		UserService.UserStartResult result = userService.start(new UserStartRequest(DEMO_KEY, "  김수현  "));

		assertThat(result.created()).isTrue();
		assertThat(result.response().name()).isEqualTo("김수현");
		assertThat(result.response().pocketAccountNo()).isEqualTo(ACCOUNT_NO);
		assertThat(result.response().pocketHolder()).isEqualTo("김수현");
		assertThat(result.response().nextScreen()).isEqualTo("ONB-02");
		verify(userRepository).create(DEMO_KEY, "김수현", ACCOUNT_NO);
	}

	@Test
	void returnsExistingUserWithoutUpdatingNameOrAccount() {
		when(userRepository.findByDemoKey(DEMO_KEY)).thenReturn(Optional.of(user(false)));

		UserService.UserStartResult result = userService.start(new UserStartRequest(DEMO_KEY, "다른 이름"));

		assertThat(result.created()).isFalse();
		assertThat(result.response().name()).isEqualTo("김수현");
		assertThat(result.response().pocketAccountNo()).isEqualTo(ACCOUNT_NO);
		verify(userRepository, never()).create(DEMO_KEY, "다른 이름", ACCOUNT_NO);
	}

	@Test
	void retriesWhenGeneratedAccountNumberAlreadyExists() {
		String retryAccountNo = "1005-9999-9999-99";
		when(userRepository.findByDemoKey(DEMO_KEY))
			.thenReturn(Optional.empty(), Optional.of(user(false)));
		when(accountNumberGenerator.generate()).thenReturn("1005-0000-0000-00", retryAccountNo);
		when(userRepository.existsByPocketAccountNo("1005-0000-0000-00")).thenReturn(true);
		when(userRepository.existsByPocketAccountNo(retryAccountNo)).thenReturn(false);

		userService.start(new UserStartRequest(DEMO_KEY, "김수현"));

		verify(userRepository).create(DEMO_KEY, "김수현", retryAccountNo);
	}

	@Test
	void returnsConcurrentUserWhenDemoKeyWasCreatedAtSameTime() {
		when(userRepository.findByDemoKey(DEMO_KEY))
			.thenReturn(Optional.empty(), Optional.of(user(false)));
		when(accountNumberGenerator.generate()).thenReturn(ACCOUNT_NO);
		when(userRepository.existsByPocketAccountNo(ACCOUNT_NO)).thenReturn(false);
		doThrow(new DuplicateKeyException("duplicate"))
			.when(userRepository).create(DEMO_KEY, "김수현", ACCOUNT_NO);

		UserService.UserStartResult result = userService.start(new UserStartRequest(DEMO_KEY, "김수현"));

		assertThat(result.created()).isFalse();
		assertThat(result.response().userId()).isEqualTo(1L);
	}

	@Test
	void rejectsBlankSpecialOnlyAndTooLongNames() {
		assertNameInvalid("   ");
		assertNameInvalid("!@#$");
		assertNameInvalid("123456789012345678901");
	}

	@Test
	void rejectsNonUuidV4DemoKey() {
		assertThatThrownBy(() -> userService.start(new UserStartRequest("not-a-uuid", "김수현")))
			.isInstanceOfSatisfying(BusinessException.class, exception -> {
				assertThat(exception.getErrorCode().code()).isEqualTo("INVALID_REQUEST");
				assertThat(exception.getField()).isEqualTo("demoKey");
			});
	}

	@Test
	void returnsCompletedBootstrapWithBillAndCurrentRound() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user(true)));
		when(billExistenceQueryService.existsByUserId(1L)).thenReturn(true);
		when(ecoCurrentRoundQueryService.findCurrentRoundId(1L)).thenReturn(Optional.of(7L));

		UserBootstrapResponse response = userService.getBootstrap(1L);

		assertThat(response.hasBill()).isTrue();
		assertThat(response.currentRoundId()).isEqualTo(7L);
		assertThat(response.entryScreen()).isEqualTo("WF-06");
		assertThat(response.ecoLinkedAt()).isEqualTo(OffsetDateTime.parse("2026-09-01T09:00:00+09:00"));
	}

	@Test
	void returnsOnboardingBootstrapWithoutBillOrRound() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user(false)));
		when(billExistenceQueryService.existsByUserId(1L)).thenReturn(false);
		when(ecoCurrentRoundQueryService.findCurrentRoundId(1L)).thenReturn(Optional.empty());

		UserBootstrapResponse response = userService.getBootstrap(1L);

		assertThat(response.hasBill()).isFalse();
		assertThat(response.currentRoundId()).isNull();
		assertThat(response.entryScreen()).isEqualTo("ONB-01");
	}

	private void assertNameInvalid(String name) {
		assertThatThrownBy(() -> userService.start(new UserStartRequest(DEMO_KEY, name)))
			.isInstanceOfSatisfying(BusinessException.class, exception -> {
				assertThat(exception.getErrorCode().code()).isEqualTo("NAME_INVALID");
				assertThat(exception.getField()).isEqualTo("name");
			});
	}

	private UserSnapshot user(boolean onboardingCompleted) {
		return new UserSnapshot(
			1L,
			DEMO_KEY,
			"김수현",
			onboardingCompleted,
			EcoLinkStatus.LINKED,
			LocalDateTime.of(2026, 9, 1, 9, 0),
			true,
			LocalDateTime.of(2026, 9, 1, 9, 12),
			ACCOUNT_NO,
			"김수현",
			LocalDateTime.of(2026, 9, 3, 18, 30)
		);
	}
}
