package com.greenpocket.eco.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.eco.dto.EcoGoalFormResponse;
import com.greenpocket.eco.dto.EcoGoalPreviewResponse;
import com.greenpocket.eco.dto.EcoGoalRequest;
import com.greenpocket.eco.dto.EcoGoalResponse;
import com.greenpocket.eco.dto.EcoGoalSaveResponse;
import com.greenpocket.eco.dto.EcoMissionUpdateRequest;
import com.greenpocket.eco.dto.EcoMissionUpdateResponse;
import com.greenpocket.eco.entity.MissionDifficulty;
import com.greenpocket.eco.entity.RoundStatus;
import com.greenpocket.eco.entity.TargetTier;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.eco.exception.EcoErrorCode;
import com.greenpocket.eco.repository.EcoGoalRepository;
import com.greenpocket.eco.repository.EcoGoalRepository.GoalRoundSnapshot;
import com.greenpocket.eco.repository.EcoGoalRepository.GoalUtilitySnapshot;
import com.greenpocket.eco.repository.EcoGoalRepository.MissionSnapshot;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.type.UtilityType;

class EcoGoalServiceTest {

	private static final Long USER_ID = 1L;
	private static final Long ROUND_ID = 7L;

	private EcoGoalRepository ecoGoalRepository;
	private EcoGoalService ecoGoalService;

	@BeforeEach
	void setUp() {
		ecoGoalRepository = mock(EcoGoalRepository.class);
		ecoGoalService = new EcoGoalService(ecoGoalRepository);
		when(ecoGoalRepository.findRound(USER_ID, ROUND_ID)).thenReturn(Optional.of(round(null)));
		when(ecoGoalRepository.findUtilities(ROUND_ID)).thenReturn(registeredUtilities());
		when(ecoGoalRepository.findActiveMissions()).thenReturn(List.of());
		when(ecoGoalRepository.findSavedMissions(ROUND_ID)).thenReturn(List.of());
	}

	@Test
	void returnsThreeSegmentsAndCalculatesMissionRateForGoalForm() {
		when(ecoGoalRepository.findActiveMissions()).thenReturn(List.of(airConditionerHourMission()));

		EcoGoalFormResponse response = ecoGoalService.getGoalForm(USER_ID, ROUND_ID);

		assertThat(response.tiers())
			.extracting(EcoGoalFormResponse.TierOption::tier)
			.containsExactly(TargetTier.TIER_5, TargetTier.TIER_10, TargetTier.TIER_15);
		assertThat(response.segments())
			.extracting(EcoGoalFormResponse.Segment::utilityType)
			.containsExactly(UtilityType.ELECTRICITY, UtilityType.GAS, UtilityType.WATER);
		EcoGoalFormResponse.Segment electricity = response.segments().getFirst();
		assertThat(electricity.monthlyBaselineUsage()).isEqualByComparingTo("223.333");
		assertThat(electricity.missionRateCap()).isEqualByComparingTo("30.000");
		assertThat(electricity.missions().getFirst().computedRate()).isEqualByComparingTo("18.000");
	}

	@Test
	void calculatesExactPreviewForThreeTargetTiers() {
		EcoGoalPreviewResponse response = ecoGoalService.preview(USER_ID, ROUND_ID, standardRequest());

		assertThat(response.utilities())
			.extracting(EcoGoalPreviewResponse.UtilityTarget::targetUsage)
			.containsExactly(
				new BigDecimal("1206.000"),
				new BigDecimal("91.800"),
				new BigDecimal("62.700")
			);
		assertThat(response.utilities())
			.extracting(EcoGoalPreviewResponse.UtilityTarget::expectedSaving)
			.containsExactly(26_800L, 14_490L, 2_800L);
		assertThat(response.combined().baselineCarbonG()).isEqualByComparingTo("831992.000");
		assertThat(response.combined().targetCarbonG()).isEqualByComparingTo("737792.400");
		assertThat(response.combined().combinedRate()).isEqualByComparingTo("11.322");
		assertThat(response.combined().tier()).isEqualTo(TargetTier.TIER_10);
		assertThat(response.combined().expectedMileage()).isEqualTo(30_000L);
		assertThat(response.combined().totalExpectedSaving()).isEqualTo(44_090L);
		assertThat(response.combined().nextTier().gapPoint()).isEqualByComparingTo("3.678");

		verify(ecoGoalRepository, never()).updateRoundGoal(anyLong(), any(), anyLong(), anyLong());
	}

	@Test
	void changesMileageAtExactTierBoundaries() {
		assertThat(previewWithSameTier("TIER_5").combined().tier()).isEqualTo(TargetTier.TIER_5);
		assertThat(previewWithSameTier("TIER_10").combined().tier()).isEqualTo(TargetTier.TIER_10);
		assertThat(previewWithSameTier("TIER_15").combined().tier()).isEqualTo(TargetTier.TIER_15);
	}

	@Test
	void countsOnlyLargestMissionInSameDeviceGroup() {
		when(ecoGoalRepository.findActiveMissions()).thenReturn(List.of(
			airConditionerTemperatureMission(),
			airConditionerHourMission()
		));
		EcoGoalRequest request = new EcoGoalRequest(
			standardRequest().targets(),
			List.of(12L, 13L)
		);

		EcoGoalPreviewResponse response = ecoGoalService.preview(USER_ID, ROUND_ID, request);

		assertThat(response.missions().combinedMissionRate()).isEqualByComparingTo("18.000");
		assertThat(response.missions().items()).satisfiesExactly(
			first -> {
				assertThat(first.computedRate()).isEqualByComparingTo("3.000");
				assertThat(first.counted()).isFalse();
				assertThat(first.exclusionReason()).isEqualTo("냉방 겹침 · 합계 제외");
			},
			second -> {
				assertThat(second.computedRate()).isEqualByComparingTo("18.000");
				assertThat(second.counted()).isTrue();
			}
		);
	}

	@Test
	void rejectsTargetForUnregisteredUtility() {
		when(ecoGoalRepository.findUtilities(ROUND_ID)).thenReturn(List.of(
			utility(UtilityType.ELECTRICITY, true, 268_000L, "1340.000", "424.000"),
			utility(UtilityType.GAS, true, 96_600L, "108.000", "2240.000"),
			utility(UtilityType.WATER, false, null, null, "332.000")
		));

		assertThatThrownBy(() -> ecoGoalService.preview(USER_ID, ROUND_ID, standardRequest()))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> assertThat(((BusinessException)error).getErrorCode())
				.isEqualTo(EcoErrorCode.ECO_UTILITY_NOT_REGISTERED));
	}

	@Test
	void rejectsInvalidTier() {
		EcoGoalRequest request = new EcoGoalRequest(
			List.of(new EcoGoalRequest.Target(UtilityType.ELECTRICITY, "TIER_20")),
			List.of()
		);

		assertThatThrownBy(() -> ecoGoalService.preview(USER_ID, ROUND_ID, request))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> assertThat(((BusinessException)error).getErrorCode())
				.isEqualTo(EcoErrorCode.ECO_TIER_INVALID));
	}

	@Test
	void savesCalculatedTargetsAndSelectedMissions() {
		when(ecoGoalRepository.findActiveMissions()).thenReturn(List.of(airConditionerHourMission()));
		when(ecoGoalRepository.findRound(USER_ID, ROUND_ID))
			.thenReturn(Optional.of(round(null)))
			.thenReturn(Optional.of(round(LocalDateTime.of(2026, 9, 4, 18, 30))));
		EcoGoalRequest request = new EcoGoalRequest(standardRequest().targets(), List.of(13L));

		EcoGoalSaveResponse response = ecoGoalService.save(USER_ID, ROUND_ID, request);

		assertThat(response.roundStatus()).isEqualTo(RoundStatus.GOAL_SET);
		assertThat(response.combinedTargetRate()).isEqualByComparingTo("11.322");
		assertThat(response.savedMissionCount()).isEqualTo(1);
		assertThat(response.nextScreen()).isEqualTo("WF-06");
		verify(ecoGoalRepository).clearUtilityTargets(ROUND_ID);
		verify(ecoGoalRepository).updateRoundGoal(
			ROUND_ID,
			new BigDecimal("11.322"),
			30_000L,
			44_090L
		);
		verify(ecoGoalRepository).saveMission(
			USER_ID,
			ROUND_ID,
			13L,
			new BigDecimal("18.000"),
			true,
			null
		);
	}

	@Test
	void returnsGoalSetFalseBeforeFirstSave() {
		EcoGoalResponse response = ecoGoalService.getGoal(USER_ID, ROUND_ID);

		assertThat(response.goalSet()).isFalse();
		assertThat(response.combinedTargetRate()).isNull();
		assertThat(response.utilities()).isNull();
		assertThat(response.missions()).isNull();
	}

	@Test
	void updatesOnlySelectedMissionsWithoutChangingGoalTargets() {
		when(ecoGoalRepository.findActiveMissions()).thenReturn(List.of(
			airConditionerTemperatureMission(),
			airConditionerHourMission()
		));

		EcoMissionUpdateResponse response = ecoGoalService.updateMissions(
			USER_ID,
			ROUND_ID,
			new EcoMissionUpdateRequest(List.of(12L, 13L))
		);

		assertThat(response.combinedMissionRate()).isEqualByComparingTo("18.000");
		assertThat(response.todayMissionsUpdated()).isTrue();
		assertThat(response.items()).satisfiesExactly(
			first -> {
				assertThat(first.missionId()).isEqualTo(12L);
				assertThat(first.counted()).isFalse();
				assertThat(first.exclusionReason()).isEqualTo("냉방 겹침 · 합계 제외");
			},
			second -> {
				assertThat(second.missionId()).isEqualTo(13L);
				assertThat(second.counted()).isTrue();
			}
		);
		verify(ecoGoalRepository).deleteSavedMissions(ROUND_ID);
		verify(ecoGoalRepository, never()).clearUtilityTargets(anyLong());
		verify(ecoGoalRepository, never()).updateRoundGoal(anyLong(), any(), anyLong(), anyLong());
	}

	@Test
	void clearsSelectedMissionsWhenUpdateListIsEmpty() {
		EcoMissionUpdateResponse response = ecoGoalService.updateMissions(
			USER_ID,
			ROUND_ID,
			new EcoMissionUpdateRequest(List.of())
		);

		assertThat(response.combinedMissionRate()).isEqualByComparingTo("0.000");
		assertThat(response.items()).isEmpty();
		verify(ecoGoalRepository).deleteSavedMissions(ROUND_ID);
		verify(ecoGoalRepository, never()).saveMission(anyLong(), anyLong(), anyLong(), any(), anyBoolean(), any());
	}

	@Test
	void rejectsUnknownMissionDuringMissionUpdate() {
		assertThatThrownBy(() -> ecoGoalService.updateMissions(
			USER_ID,
			ROUND_ID,
			new EcoMissionUpdateRequest(List.of(999L))
		))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> {
				BusinessException businessException = (BusinessException)error;
				assertThat(businessException.getField()).isEqualTo("selectedMissionIds");
				assertThat(businessException.getDetails()).containsEntry("invalidMissionIds", List.of(999L));
			});
		verify(ecoGoalRepository, never()).deleteSavedMissions(anyLong());
	}

	@Test
	void hidesRoundOwnedByAnotherUser() {
		when(ecoGoalRepository.findRound(USER_ID, ROUND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> ecoGoalService.getGoalForm(USER_ID, ROUND_ID))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> assertThat(((BusinessException)error).getErrorCode())
				.isEqualTo(EcoErrorCode.ECO_ROUND_NOT_FOUND));
	}

	private EcoGoalPreviewResponse previewWithSameTier(String tier) {
		return ecoGoalService.preview(
			USER_ID,
			ROUND_ID,
			new EcoGoalRequest(
				List.of(
					new EcoGoalRequest.Target(UtilityType.ELECTRICITY, tier),
					new EcoGoalRequest.Target(UtilityType.GAS, tier),
					new EcoGoalRequest.Target(UtilityType.WATER, tier)
				),
				List.of()
			)
		);
	}

	private EcoGoalRequest standardRequest() {
		return new EcoGoalRequest(
			List.of(
				new EcoGoalRequest.Target(UtilityType.ELECTRICITY, "TIER_10"),
				new EcoGoalRequest.Target(UtilityType.GAS, "TIER_15"),
				new EcoGoalRequest.Target(UtilityType.WATER, "TIER_5")
			),
			List.of()
		);
	}

	private GoalRoundSnapshot round(LocalDateTime goalSetAt) {
		return new GoalRoundSnapshot(
			ROUND_ID,
			LocalDate.of(2026, 4, 1),
			LocalDate.of(2026, 9, 1),
			goalSetAt == null ? RoundStatus.READY : RoundStatus.GOAL_SET,
			420_600L,
			new BigDecimal("831992.000"),
			goalSetAt,
			goalSetAt == null ? null : new BigDecimal("11.322"),
			goalSetAt == null ? 0L : 30_000L,
			goalSetAt == null ? null : 44_090L
		);
	}

	private List<GoalUtilitySnapshot> registeredUtilities() {
		return List.of(
			utility(UtilityType.ELECTRICITY, true, 268_000L, "1340.000", "424.000"),
			utility(UtilityType.GAS, true, 96_600L, "108.000", "2240.000"),
			utility(UtilityType.WATER, true, 56_000L, "66.000", "332.000")
		);
	}

	private GoalUtilitySnapshot utility(
		UtilityType utilityType,
		boolean registered,
		Long amount,
		String usage,
		String factor
	) {
		return new GoalUtilitySnapshot(
			utilityType,
			registered,
			registered ? null : "세대 명의 계약이 없어 사용량을 불러올 수 없어요",
			new BigDecimal(factor),
			amount,
			usage == null ? null : new BigDecimal(usage),
			null,
			null,
			null,
			null,
			null
		);
	}

	private MissionSnapshot airConditionerTemperatureMission() {
		return mission(12L, "AC_TEMP_26", "냉방 온도 26℃로 맞추기", "7.000");
	}

	private MissionSnapshot airConditionerHourMission() {
		return mission(13L, "AC_HOUR_1", "에어컨 하루 1시간 줄이기", "40.000");
	}

	private MissionSnapshot mission(Long id, String code, String title, String evidenceAmount) {
		return new MissionSnapshot(
			id,
			code,
			UtilityType.ELECTRICITY,
			title,
			"여름철 냉방 실천이에요",
			MissionDifficulty.NORMAL,
			new BigDecimal(evidenceAmount),
			UsageUnit.kWh,
			"공식 절감 수치",
			"공식 절감량을 우리 집 월 기준 사용량으로 환산",
			"한국에너지공단",
			"냉방",
			List.of("SUMMER"),
			new BigDecimal("30.000"),
			id.intValue()
		);
	}
}
