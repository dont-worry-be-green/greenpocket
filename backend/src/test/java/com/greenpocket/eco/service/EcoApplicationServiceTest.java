package com.greenpocket.eco.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.eco.dto.EcoApplicationResponse;
import com.greenpocket.eco.entity.ApplicationStatus;
import com.greenpocket.eco.exception.EcoErrorCode;
import com.greenpocket.eco.repository.EcoApplicationRepository;
import com.greenpocket.eco.repository.EcoApplicationRepository.ApplicationSnapshot;
import com.greenpocket.global.exception.BusinessException;

class EcoApplicationServiceTest {

	private static final Long USER_ID = 42L;
	private static final Long ROUND_ID = 7L;
	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final LocalDateTime APPLIED_AT = LocalDateTime.of(2026, 9, 3, 18, 45);

	private EcoApplicationRepository ecoApplicationRepository;
	private EcoApplicationService ecoApplicationService;

	@BeforeEach
	void setUp() {
		ecoApplicationRepository = mock(EcoApplicationRepository.class);
		Clock clock = Clock.fixed(Instant.parse("2026-09-03T09:45:00Z"), KOREA_ZONE_ID);
		ecoApplicationService = new EcoApplicationService(ecoApplicationRepository, clock);
	}

	@Test
	void appliesForEcoMileage() {
		when(ecoApplicationRepository.findByUserIdAndRoundId(USER_ID, ROUND_ID))
			.thenReturn(Optional.of(new ApplicationSnapshot(
				ROUND_ID,
				ApplicationStatus.NOT_APPLIED,
				LocalDateTime.of(2026, 9, 1, 9, 0)
			)));
		when(ecoApplicationRepository.markApplied(USER_ID, ROUND_ID, APPLIED_AT)).thenReturn(true);

		EcoApplicationResponse response = ecoApplicationService.apply(USER_ID, ROUND_ID);

		assertThat(response.roundId()).isEqualTo(ROUND_ID);
		assertThat(response.applicationStatus()).isEqualTo(ApplicationStatus.APPLIED);
		assertThat(response.appliedAt()).isEqualTo("2026-09-03T18:45+09:00");
		assertThat(response.showBanner()).isFalse();
		verify(ecoApplicationRepository).markApplied(USER_ID, ROUND_ID, APPLIED_AT);
	}

	@Test
	void returnsExistingApplicationWithoutChangingAppliedAt() {
		LocalDateTime existingAppliedAt = LocalDateTime.of(2026, 9, 2, 15, 20);
		when(ecoApplicationRepository.findByUserIdAndRoundId(USER_ID, ROUND_ID))
			.thenReturn(Optional.of(new ApplicationSnapshot(
				ROUND_ID,
				ApplicationStatus.APPLIED,
				existingAppliedAt
			)));

		EcoApplicationResponse response = ecoApplicationService.apply(USER_ID, ROUND_ID);

		assertThat(response.applicationStatus()).isEqualTo(ApplicationStatus.APPLIED);
		assertThat(response.appliedAt()).isEqualTo("2026-09-02T15:20+09:00");
		assertThat(response.showBanner()).isFalse();
		verify(ecoApplicationRepository, never()).markApplied(USER_ID, ROUND_ID, APPLIED_AT);
	}

	@Test
	void returnsApplicationCompletedByConcurrentRequest() {
		ApplicationSnapshot before = new ApplicationSnapshot(
			ROUND_ID,
			ApplicationStatus.APPLYING,
			LocalDateTime.of(2026, 9, 3, 18, 40)
		);
		ApplicationSnapshot completed = new ApplicationSnapshot(
			ROUND_ID,
			ApplicationStatus.APPLIED,
			LocalDateTime.of(2026, 9, 3, 18, 44)
		);
		when(ecoApplicationRepository.findByUserIdAndRoundId(USER_ID, ROUND_ID))
			.thenReturn(Optional.of(before))
			.thenReturn(Optional.of(completed));
		when(ecoApplicationRepository.markApplied(USER_ID, ROUND_ID, APPLIED_AT)).thenReturn(false);

		EcoApplicationResponse response = ecoApplicationService.apply(USER_ID, ROUND_ID);

		assertThat(response.applicationStatus()).isEqualTo(ApplicationStatus.APPLIED);
		assertThat(response.appliedAt()).isEqualTo("2026-09-03T18:44+09:00");
	}

	@Test
	void rejectsUnknownOrUnownedRound() {
		when(ecoApplicationRepository.findByUserIdAndRoundId(USER_ID, ROUND_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> ecoApplicationService.apply(USER_ID, ROUND_ID))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> assertThat(((BusinessException)error).getErrorCode())
				.isEqualTo(EcoErrorCode.ECO_ROUND_NOT_FOUND));
	}
}
