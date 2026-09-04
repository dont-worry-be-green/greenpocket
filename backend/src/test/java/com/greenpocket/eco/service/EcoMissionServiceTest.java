package com.greenpocket.eco.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.greenpocket.eco.dto.EcoMissionLogRequest;
import com.greenpocket.eco.dto.EcoMissionLogResponse;
import com.greenpocket.eco.dto.EcoTodayMissionsResponse;
import com.greenpocket.eco.entity.MissionDifficulty;
import com.greenpocket.eco.exception.EcoErrorCode;
import com.greenpocket.eco.repository.EcoMissionRepository;
import com.greenpocket.eco.repository.EcoMissionRepository.TodayMissionSnapshot;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.global.type.UtilityType;

class EcoMissionServiceTest {

	private static final Long USER_ID = 42L;
	private static final Long ROUND_ID = 7L;
	private static final LocalDate TODAY = LocalDate.of(2026, 9, 4);

	private EcoMissionRepository ecoMissionRepository;
	private EcoMissionService ecoMissionService;

	@BeforeEach
	void setUp() {
		ecoMissionRepository = mock(EcoMissionRepository.class);
		Clock clock = Clock.fixed(
			Instant.parse("2026-09-04T01:00:00Z"),
			ZoneId.of("Asia/Seoul")
		);
		ecoMissionService = new EcoMissionService(ecoMissionRepository, clock);
	}

	@Test
	void createsServiceBeanWithRepositoryDependency() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.registerBean(EcoMissionRepository.class, () -> ecoMissionRepository);
			context.register(EcoMissionService.class);
			context.refresh();

			assertThat(context.getBean(EcoMissionService.class)).isNotNull();
		}
	}

	@Test
	void returnsSeasonalMissionsAndCompletionProgress() {
		when(ecoMissionRepository.findOwnedRoundId(USER_ID, ROUND_ID)).thenReturn(Optional.of(ROUND_ID));
		when(ecoMissionRepository.findTodayMissions(USER_ID, ROUND_ID, LocalDate.of(2026, 8, 3), "SUMMER"))
			.thenReturn(List.of(
				mission(12L, "냉방 온도 26℃로 맞추기", UtilityType.ELECTRICITY, true),
				mission(31L, "온수 온도 낮추기", UtilityType.GAS, false)
			));

		EcoTodayMissionsResponse response = ecoMissionService.getTodayMissions(USER_ID, ROUND_ID, "2026-08-03");

		assertThat(response.date()).isEqualTo("2026-08-03");
		assertThat(response.season()).isEqualTo("SUMMER");
		assertThat(response.completedCount()).isEqualTo(1);
		assertThat(response.totalCount()).isEqualTo(2);
		assertThat(response.missions()).hasSize(2);
		assertThat(response.missions().getFirst().completed()).isTrue();
		assertThat(response.emptyReason()).isNull();
	}

	@Test
	void usesTodayAndReturnsEmptyReasonWhenSeasonHasNoMission() {
		when(ecoMissionRepository.findOwnedRoundId(USER_ID, ROUND_ID)).thenReturn(Optional.of(ROUND_ID));
		when(ecoMissionRepository.findTodayMissions(USER_ID, ROUND_ID, TODAY, "AUTUMN"))
			.thenReturn(List.of());

		EcoTodayMissionsResponse response = ecoMissionService.getTodayMissions(USER_ID, ROUND_ID, null);

		assertThat(response.date()).isEqualTo("2026-09-04");
		assertThat(response.season()).isEqualTo("AUTUMN");
		assertThat(response.completedCount()).isZero();
		assertThat(response.totalCount()).isZero();
		assertThat(response.missions()).isEmpty();
		assertThat(response.emptyReason()).isEqualTo("SEASON_FILTERED_EMPTY");
	}

	@Test
	void savesDistinctCompletedMissionIds() {
		LocalDate date = LocalDate.of(2026, 9, 3);
		when(ecoMissionRepository.findOwnedRoundId(USER_ID, ROUND_ID)).thenReturn(Optional.of(ROUND_ID));
		when(ecoMissionRepository.findTodayMissions(USER_ID, ROUND_ID, date, "AUTUMN"))
			.thenReturn(List.of(
				mission(12L, "절전 실천", UtilityType.ELECTRICITY, false),
				mission(31L, "온수 절약", UtilityType.GAS, false),
				mission(44L, "물 절약", UtilityType.WATER, false)
			));

		EcoMissionLogResponse response = ecoMissionService.saveMissionLog(
			USER_ID,
			ROUND_ID,
			"2026-09-03",
			new EcoMissionLogRequest(List.of(12L, 31L, 12L))
		);

		assertThat(response.date()).isEqualTo("2026-09-03");
		assertThat(response.completedCount()).isEqualTo(2);
		assertThat(response.totalCount()).isEqualTo(3);
		verify(ecoMissionRepository).saveDailyLog(USER_ID, ROUND_ID, date, List.of(12L, 31L));
	}

	@Test
	void rejectsMissionThatIsNotAvailableForDate() {
		LocalDate date = LocalDate.of(2026, 9, 3);
		when(ecoMissionRepository.findOwnedRoundId(USER_ID, ROUND_ID)).thenReturn(Optional.of(ROUND_ID));
		when(ecoMissionRepository.findTodayMissions(USER_ID, ROUND_ID, date, "AUTUMN"))
			.thenReturn(List.of(mission(12L, "절전 실천", UtilityType.ELECTRICITY, false)));

		assertThatThrownBy(() -> ecoMissionService.saveMissionLog(
			USER_ID,
			ROUND_ID,
			"2026-09-03",
			new EcoMissionLogRequest(List.of(12L, 999L))
		))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> {
				BusinessException businessException = (BusinessException)error;
				assertThat(businessException.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_REQUEST);
				assertThat(businessException.getField()).isEqualTo("completedMissionIds");
				assertThat(businessException.getDetails()).containsEntry("invalidMissionIds", List.of(999L));
			});
	}

	@Test
	void rejectsMissingCompletedMissionIds() {
		when(ecoMissionRepository.findOwnedRoundId(USER_ID, ROUND_ID)).thenReturn(Optional.of(ROUND_ID));
		when(ecoMissionRepository.findTodayMissions(USER_ID, ROUND_ID, TODAY, "AUTUMN"))
			.thenReturn(List.of());

		assertThatThrownBy(() -> ecoMissionService.saveMissionLog(
			USER_ID,
			ROUND_ID,
			"2026-09-04",
			new EcoMissionLogRequest(null)
		))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> assertThat(((BusinessException)error).getField())
				.isEqualTo("completedMissionIds"));
	}

	@Test
	void rejectsInvalidDate() {
		when(ecoMissionRepository.findOwnedRoundId(USER_ID, ROUND_ID)).thenReturn(Optional.of(ROUND_ID));

		assertThatThrownBy(() -> ecoMissionService.getTodayMissions(USER_ID, ROUND_ID, "2026-9-4"))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> {
				BusinessException businessException = (BusinessException)error;
				assertThat(businessException.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_REQUEST);
				assertThat(businessException.getField()).isEqualTo("date");
			});
	}

	@Test
	void rejectsUnknownOrUnownedRound() {
		when(ecoMissionRepository.findOwnedRoundId(USER_ID, ROUND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> ecoMissionService.getTodayMissions(USER_ID, ROUND_ID, "2026-09-04"))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> assertThat(((BusinessException)error).getErrorCode())
				.isEqualTo(EcoErrorCode.ECO_ROUND_NOT_FOUND));
	}

	private TodayMissionSnapshot mission(
		Long missionId,
		String title,
		UtilityType utilityType,
		boolean completed
	) {
		return new TodayMissionSnapshot(
			missionId,
			title,
			utilityType,
			MissionDifficulty.EASY,
			completed
		);
	}
}
