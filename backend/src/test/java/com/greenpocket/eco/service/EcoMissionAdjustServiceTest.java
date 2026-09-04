package com.greenpocket.eco.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.eco.dto.EcoGoalFormResponse;
import com.greenpocket.eco.dto.EcoGoalResponse;
import com.greenpocket.eco.dto.EcoMissionAdjustResponse;
import com.greenpocket.eco.dto.EcoMonthlyReportResponse;
import com.greenpocket.eco.entity.MissionDifficulty;
import com.greenpocket.eco.entity.TargetTier;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.global.type.UtilityType;

class EcoMissionAdjustServiceTest {

	private static final Long USER_ID = 1L;
	private static final Long ROUND_ID = 7L;

	private EcoGoalService ecoGoalService;
	private EcoProgressService ecoProgressService;
	private EcoMissionAdjustService service;

	@BeforeEach
	void setUp() {
		ecoGoalService = mock(EcoGoalService.class);
		ecoProgressService = mock(EcoProgressService.class);
		service = new EcoMissionAdjustService(ecoGoalService, ecoProgressService);
		when(ecoGoalService.getGoalForm(USER_ID, ROUND_ID)).thenReturn(goalForm());
		when(ecoGoalService.getGoal(USER_ID, ROUND_ID)).thenReturn(savedGoal());
		when(ecoProgressService.getMonthlyReport(USER_ID, "2026-08")).thenReturn(monthlyReport(ROUND_ID));
	}

	@Test
	void recommendsNonOverlappingDeviceGroupsUntilRequiredRateIsCovered() {
		EcoMissionAdjustResponse response = service.getMissionAdjust(
			USER_ID,
			ROUND_ID,
			"ELECTRICITY",
			"2026-08"
		);

		assertThat(response.requiredRate()).isEqualByComparingTo("10.000");
		assertThat(response.carbonSharePercent()).isEqualByComparingTo("83.000");
		assertThat(response.comparison().selectedExpectedRate()).isEqualByComparingTo("3.000");
		assertThat(response.comparison().actualRate()).isEqualByComparingTo("-1.887");
		assertThat(response.currentSelectedCount()).isEqualTo(1);
		assertThat(response.missions())
			.filteredOn(EcoMissionAdjustResponse.Mission::recommended)
			.extracting(EcoMissionAdjustResponse.Mission::missionId)
			.containsExactly(14L, 15L);
		assertThat(response.missions().get(1).recommended()).isFalse();
		assertThat(response.preview().withRecommendedRate()).isEqualByComparingTo("14.000");
		assertThat(response.preview().coversRequired()).isTrue();
	}

	@Test
	void suggestsTierDowngradeOnlyAfterTwoConsecutiveMisses() {
		EcoMissionAdjustResponse response = service.getMissionAdjust(
			USER_ID,
			ROUND_ID,
			"electricity",
			"2026-08"
		);

		assertThat(response.tierDowngrade().suggest()).isTrue();
		assertThat(response.tierDowngrade().consecutiveMisses()).isEqualTo(2);
		assertThat(response.tierDowngrade().message()).contains("2개월 연속");
	}

	@Test
	void returnsEmptyRecommendationDataWhenBillDoesNotExist() {
		when(ecoProgressService.getMonthlyReport(USER_ID, null)).thenReturn(new EcoMonthlyReportResponse(
			null,
			ROUND_ID,
			null,
			null,
			null,
			null,
			null,
			List.of(),
			"NO_BILL"
		));

		EcoMissionAdjustResponse response = service.getMissionAdjust(
			USER_ID,
			ROUND_ID,
			"WATER",
			null
		);

		assertThat(response.reportMonth()).isNull();
		assertThat(response.requiredRate()).isNull();
		assertThat(response.missions()).isEmpty();
		assertThat(response.preview().coversRequired()).isFalse();
	}

	@Test
	void rejectsInvalidUtility() {
		assertThatThrownBy(() -> service.getMissionAdjust(
			USER_ID,
			ROUND_ID,
			"HEAT",
			"2026-08"
		))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> {
				BusinessException businessException = (BusinessException)error;
				assertThat(businessException.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_REQUEST);
				assertThat(businessException.getField()).isEqualTo("utility");
			});
	}

	@Test
	void rejectsReportMonthThatBelongsToAnotherRound() {
		when(ecoProgressService.getMonthlyReport(USER_ID, "2025-12")).thenReturn(monthlyReport(8L));

		assertThatThrownBy(() -> service.getMissionAdjust(
			USER_ID,
			ROUND_ID,
			"ELECTRICITY",
			"2025-12"
		))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> assertThat(((BusinessException)error).getField()).isEqualTo("month"));
	}

	private EcoGoalFormResponse goalForm() {
		return new EcoGoalFormResponse(
			ROUND_ID,
			"2026-04",
			"2026-09",
			List.of(),
			List.of(
				new EcoGoalFormResponse.Segment(
					UtilityType.ELECTRICITY,
					true,
					null,
					268_000L,
					new BigDecimal("1340.000"),
					new BigDecimal("223.333"),
					UsageUnit.kWh,
					new BigDecimal("30.000"),
					TargetTier.TIER_10,
					null,
					false,
					List.of(
						mission(12L, "냉방 온도 조절", "3.000", "냉방", true),
						mission(13L, "에어컨 사용 줄이기", "18.000", "냉방", false),
						mission(14L, "조명 끄기", "6.000", "조명", false),
						mission(15L, "대기전력 줄이기", "5.000", "대기전력", false)
					)
				),
				emptySegment(UtilityType.GAS, UsageUnit.m3),
				emptySegment(UtilityType.WATER, UsageUnit.m3)
			)
		);
	}

	private EcoGoalResponse savedGoal() {
		return new EcoGoalResponse(
			ROUND_ID,
			true,
			OffsetDateTime.parse("2026-04-01T09:00:00+09:00"),
			new BigDecimal("10.000"),
			TargetTier.TIER_10,
			30_000L,
			44_090L,
			List.of(),
			List.of(new EcoGoalResponse.SavedMission(
				12L,
				"냉방 온도 조절",
				UtilityType.ELECTRICITY,
				new BigDecimal("3.000"),
				true,
				null
			))
		);
	}

	private EcoMonthlyReportResponse monthlyReport(Long roundId) {
		return new EcoMonthlyReportResponse(
			"2026-08",
			roundId,
			OffsetDateTime.parse("2026-09-03T09:00:00+09:00"),
			"2024·2025년 8월 평균",
			new EcoMonthlyReportResponse.Result(
				new BigDecimal("-1.887"),
				new BigDecimal("10.000"),
				false,
				new BigDecimal("8.000"),
				List.of("2026-07", "2026-08")
			),
			new EcoMonthlyReportResponse.Cause(
				List.of(new EcoMonthlyReportResponse.UtilityResult(
					UtilityType.ELECTRICITY,
					new BigDecimal("223.333"),
					new BigDecimal("227.548"),
					UsageUnit.kWh,
					new BigDecimal("-1.887"),
					false,
					new BigDecimal("83.000"),
					true
				)),
				UtilityType.ELECTRICITY,
				List.of()
			),
			new EcoMonthlyReportResponse.Prescription(
				1,
				List.of(9),
				new BigDecimal("10.000"),
				true,
				List.of(new EcoMonthlyReportResponse.RequiredUtility(
					UtilityType.ELECTRICITY,
					new BigDecimal("10.000"),
					"도시가스 16%, 수도 11% 감축을 지금처럼 유지할 때예요"
				)),
				new BigDecimal("3.000"),
				UtilityType.ELECTRICITY
			),
			List.of(
				new EcoMonthlyReportResponse.MonthlyRate("2026-07", new BigDecimal("8.000"), false),
				new EcoMonthlyReportResponse.MonthlyRate("2026-08", new BigDecimal("7.000"), false)
			),
			null
		);
	}

	private EcoGoalFormResponse.Segment emptySegment(UtilityType utilityType, UsageUnit usageUnit) {
		return new EcoGoalFormResponse.Segment(
			utilityType,
			true,
			null,
			0L,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			usageUnit,
			null,
			TargetTier.TIER_10,
			null,
			false,
			List.of()
		);
	}

	private EcoGoalFormResponse.Mission mission(
		Long id,
		String title,
		String rate,
		String deviceGroup,
		boolean selected
	) {
		return new EcoGoalFormResponse.Mission(
			id,
			"MISSION_" + id,
			title,
			"설명",
			MissionDifficulty.NORMAL,
			BigDecimal.ONE,
			UsageUnit.kWh,
			"근거",
			"계산 기준",
			"한국에너지공단",
			deviceGroup,
			List.of("SUMMER"),
			new BigDecimal(rate),
			false,
			selected
		);
	}
}
