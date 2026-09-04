package com.greenpocket.diagnosis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.bill.service.BillDiagnosisQueryService;
import com.greenpocket.bill.service.BillDiagnosisQueryService.MonthlyRecord;
import com.greenpocket.diagnosis.dto.DiagnosisMonthsResponse;
import com.greenpocket.diagnosis.dto.DiagnosisResponse;
import com.greenpocket.diagnosis.entity.RegionLevel;
import com.greenpocket.diagnosis.entity.RegionUtilitySnapshot;
import com.greenpocket.diagnosis.repository.RegionUtilitySnapshotRepository;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.eco.service.EcoCurrentRoundQueryService;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.type.UtilityType;
import com.greenpocket.user.service.UserRegionQueryService;
import com.greenpocket.user.service.UserRegionQueryService.UserDiagnosisProfile;

class DiagnosisResultServiceTest {

	private static final Long USER_ID = 1L;
	private static final YearMonth TARGET_MONTH = YearMonth.of(2026, 8);

	private BillDiagnosisQueryService billQueryService;
	private UserRegionQueryService userRegionQueryService;
	private EcoCurrentRoundQueryService ecoCurrentRoundQueryService;
	private RegionUtilitySnapshotRepository baselineRepository;
	private DiagnosisResultService diagnosisResultService;

	@BeforeEach
	void setUp() {
		billQueryService = mock(BillDiagnosisQueryService.class);
		userRegionQueryService = mock(UserRegionQueryService.class);
		ecoCurrentRoundQueryService = mock(EcoCurrentRoundQueryService.class);
		baselineRepository = mock(RegionUtilitySnapshotRepository.class);
		Clock clock = Clock.fixed(
			Instant.parse("2026-09-04T00:00:00Z"),
			ZoneId.of("Asia/Seoul")
		);
		diagnosisResultService = new DiagnosisResultService(
			billQueryService,
			userRegionQueryService,
			ecoCurrentRoundQueryService,
			baselineRepository,
			clock
		);
	}

	@Test
	void groupsRegisteredMonthsLatestFirst() {
		when(billQueryService.findAllBills(USER_ID)).thenReturn(List.of(
			record(TARGET_MONTH.minusMonths(1), UtilityType.ELECTRICITY, 40_000L),
			record(TARGET_MONTH, UtilityType.WATER, 8_900L),
			record(TARGET_MONTH, UtilityType.ELECTRICITY, 43_200L)
		));

		DiagnosisMonthsResponse response = diagnosisResultService.findMonths(USER_ID);

		assertThat(response.defaultMonth()).isEqualTo("2026-08");
		assertThat(response.months()).hasSize(2);
		assertThat(response.months().getFirst().utilities())
			.containsExactly(UtilityType.ELECTRICITY, UtilityType.WATER);
		assertThat(response.months().getFirst().totalAmount()).isEqualTo(52_100L);
	}

	@Test
	void returnsEmptyStateWithLatestUnregisteredBillingMonth() {
		when(billQueryService.findAllBills(USER_ID)).thenReturn(List.of());

		DiagnosisResponse response = diagnosisResultService.findDiagnosis(USER_ID, null);

		assertThat(response.empty()).isTrue();
		assertThat(response.targetYearMonth()).isEqualTo("2026-08");
		assertThat(response.screen()).isEqualTo("AN-01");
	}

	@Test
	void rejectsExplicitUnregisteredMonth() {
		when(billQueryService.findAllBills(USER_ID)).thenReturn(List.of(
			record(TARGET_MONTH, UtilityType.ELECTRICITY, 43_200L)
		));

		assertThatThrownBy(() -> diagnosisResultService.findDiagnosis(USER_ID, TARGET_MONTH.minusMonths(1)))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode().code())
			.isEqualTo("DIAGNOSIS_MONTH_EMPTY");
	}

	@Test
	void returnsCompleteDiagnosisWithSignedDifferencesAndFallback() {
		List<MonthlyRecord> current = List.of(
			record(TARGET_MONTH, UtilityType.ELECTRICITY, 43_200L),
			record(TARGET_MONTH, UtilityType.GAS, 12_400L),
			record(TARGET_MONTH, UtilityType.WATER, 8_900L)
		);
		List<MonthlyRecord> previous = List.of(
			record(TARGET_MONTH.minusYears(1), UtilityType.ELECTRICITY, 40_100L),
			record(TARGET_MONTH.minusYears(1), UtilityType.GAS, 14_200L),
			record(TARGET_MONTH.minusYears(1), UtilityType.WATER, 8_300L)
		);
		when(billQueryService.findAllBills(USER_ID)).thenReturn(current);
		when(billQueryService.findPreviousYearBaseline(USER_ID, TARGET_MONTH.minusYears(1)))
			.thenReturn(previous);
		when(billQueryService.findBills(USER_ID, TARGET_MONTH.minusMonths(5), TARGET_MONTH))
			.thenReturn(current);
		when(userRegionQueryService.findDiagnosisProfile(USER_ID)).thenReturn(Optional.of(
			new UserDiagnosisProfile("11", "서울", "11620", "관악구", "APARTMENT", "OVER_20")
		));
		RegionUtilitySnapshot baseline = baseline(RegionLevel.SIDO, "", 38_900L);
		when(baselineRepository
			.findFirstByRegionLevelAndSidoCodeAndSigunguCodeAndUtilityTypeAndBaseMonthLessThanEqualAndAvgUsageIsNotNullAndAvgAmountIsNotNullOrderByBaseMonthDesc(
				eq(RegionLevel.SIGUNGU), eq("11"), eq("11620"),
				eq(UtilityType.ELECTRICITY), any(LocalDate.class)
			))
			.thenReturn(Optional.empty());
		when(baselineRepository
			.findFirstByRegionLevelAndSidoCodeAndSigunguCodeAndUtilityTypeAndBaseMonthLessThanEqualAndAvgUsageIsNotNullAndAvgAmountIsNotNullOrderByBaseMonthDesc(
				eq(RegionLevel.SIDO), eq("11"), eq(""),
				eq(UtilityType.ELECTRICITY), any(LocalDate.class)
			))
			.thenReturn(Optional.of(baseline));
		when(ecoCurrentRoundQueryService.findCurrentRoundLink(USER_ID)).thenReturn(Optional.of(
			new EcoCurrentRoundQueryService.CurrentRoundLink(7L, true)
		));

		DiagnosisResponse response = diagnosisResultService.findDiagnosis(USER_ID, null);

		assertThat(response.empty()).isFalse();
		assertThat(response.profileSummary()).isEqualTo("서울 관악구 · 아파트 20평 이상");
		assertThat(response.summary().currentTotal()).isEqualTo(64_500L);
		assertThat(response.summary().previousYearTotal()).isEqualTo(62_600L);
		assertThat(response.summary().diffLastYearTotal()).isEqualTo(1_900L);
		assertThat(response.lastYearComparison().items())
			.extracting(DiagnosisResponse.LastYearItem::diff)
			.containsExactly(3_100L, -1_800L, 600L);
		assertThat(response.regionComparison().regionLevel()).isEqualTo(RegionLevel.SIDO);
		assertThat(response.regionComparison().regionLabel()).isEqualTo("서울");
		assertThat(response.regionComparison().fallbackApplied()).isTrue();
		assertThat(response.regionComparison().tabs().getFirst().diffRegion()).isEqualTo(4_300L);
		assertThat(response.regionComparison().tabs().getFirst().series()).hasSize(6);
		assertThat(response.regionComparison().tabs().get(1).unavailableReason())
			.isEqualTo("REGION_DATA_NOT_PUBLISHED");
		assertThat(response.whatIfLink()).isEqualTo(new DiagnosisResponse.WhatIfLink(7L, true));
	}

	@Test
	void reportsUnavailableComparisonsWithoutInventingValues() {
		List<MonthlyRecord> current = List.of(
			record(TARGET_MONTH, UtilityType.ELECTRICITY, 43_200L)
		);
		when(billQueryService.findAllBills(USER_ID)).thenReturn(current);
		when(billQueryService.findPreviousYearBaseline(USER_ID, TARGET_MONTH.minusYears(1)))
			.thenReturn(List.of());
		when(userRegionQueryService.findDiagnosisProfile(USER_ID)).thenReturn(Optional.empty());
		when(ecoCurrentRoundQueryService.findCurrentRoundLink(USER_ID)).thenReturn(Optional.empty());

		DiagnosisResponse response = diagnosisResultService.findDiagnosis(USER_ID, TARGET_MONTH);

		assertThat(response.summary().hasPreviousYear()).isFalse();
		assertThat(response.summary().previousYearTotal()).isNull();
		assertThat(response.lastYearComparison().available()).isFalse();
		assertThat(response.lastYearComparison().unavailableReason()).isEqualTo("NO_BASELINE");
		assertThat(response.regionComparison().tabs().getFirst().available()).isFalse();
		assertThat(response.regionComparison().tabs().getFirst().regionAvgAmount()).isNull();
		assertThat(response.whatIfLink()).isEqualTo(new DiagnosisResponse.WhatIfLink(null, false));
	}

	private static MonthlyRecord record(YearMonth month, UtilityType utilityType, long amount) {
		return new MonthlyRecord(
			month,
			utilityType,
			amount,
			new BigDecimal("10.000"),
			utilityType == UtilityType.ELECTRICITY ? UsageUnit.kWh : UsageUnit.m3
		);
	}

	private static RegionUtilitySnapshot baseline(
		RegionLevel regionLevel,
		String sigunguCode,
		long averageAmount
	) {
		RegionUtilitySnapshot snapshot = mock(RegionUtilitySnapshot.class);
		when(snapshot.getRegionLevel()).thenReturn(regionLevel);
		when(snapshot.getSidoCode()).thenReturn("11");
		when(snapshot.getSigunguCode()).thenReturn(sigunguCode);
		when(snapshot.getBaseMonth()).thenReturn(LocalDate.of(2026, 7, 1));
		when(snapshot.getUtilityType()).thenReturn(UtilityType.ELECTRICITY);
		when(snapshot.getAvgUsage()).thenReturn(new BigDecimal("289.400"));
		when(snapshot.getAvgAmount()).thenReturn(averageAmount);
		when(snapshot.getSourceName()).thenReturn("한국전력공사 전력데이터 개방포털");
		when(snapshot.getExtractedAt()).thenReturn(LocalDateTime.of(2026, 8, 28, 0, 0));
		return snapshot;
	}
}
