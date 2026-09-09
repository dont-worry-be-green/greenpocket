package com.greenpocket.diagnosis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
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
import com.greenpocket.diagnosis.dto.BaselineCalculationBasis;
import com.greenpocket.diagnosis.service.SingleHouseholdBaselineCatalog.Baseline;
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
	private SingleHouseholdBaselineCatalog baselineCatalog;
	private DiagnosisResultService diagnosisResultService;

	@BeforeEach
	void setUp() {
		billQueryService = mock(BillDiagnosisQueryService.class);
		userRegionQueryService = mock(UserRegionQueryService.class);
		ecoCurrentRoundQueryService = mock(EcoCurrentRoundQueryService.class);
		baselineCatalog = mock(SingleHouseholdBaselineCatalog.class);
		Clock clock = Clock.fixed(
			Instant.parse("2026-09-04T00:00:00Z"),
			ZoneId.of("Asia/Seoul")
		);
		diagnosisResultService = new DiagnosisResultService(
			billQueryService,
			userRegionQueryService,
			ecoCurrentRoundQueryService,
			baselineCatalog,
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
	void returnsCompleteDiagnosisWithSignedAmountAndUsageDifferences() {
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
		when(billQueryService.findAllBills(USER_ID)).thenReturn(List.of(
			current.get(0), current.get(1), current.get(2),
			record(TARGET_MONTH.minusMonths(1), UtilityType.ELECTRICITY, 40_100L)
		));
		when(billQueryService.findPreviousYearBaseline(USER_ID, TARGET_MONTH.minusYears(1)))
			.thenReturn(previous);
		when(userRegionQueryService.findDiagnosisProfile(USER_ID)).thenReturn(Optional.of(
			new UserDiagnosisProfile("11", "서울", "11620", "관악구", "APARTMENT", "OVER_20")
		));
		mockSeriesBaselines(UtilityType.ELECTRICITY, UsageUnit.kWh,
			"189.658", "184.783", "179.013", "186.260", "228.449", "257.617");
		mockSeriesBaselines(UtilityType.GAS, UsageUnit.m3,
			"68.353", "47.285", "34.182", "23.414", "18.890", "16.781");
		mockSeriesBaselines(UtilityType.WATER, UsageUnit.m3,
			"13.578", "13.140", "13.578", "13.140", "13.578", "13.578");
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
		assertThat(response.singleHouseholdComparison().comparisonLabel()).isEqualTo("1인 가구 평균 사용량");
		assertThat(response.singleHouseholdComparison().tabs()).hasSize(3);
		assertThat(response.singleHouseholdComparison().tabs().getFirst().differenceUsage())
			.isEqualByComparingTo("-247.617");
		assertThat(response.singleHouseholdComparison().tabs().getFirst().differenceRate())
			.isEqualByComparingTo("-96.118");
		assertThat(response.singleHouseholdComparison().tabs().getFirst().series())
			.extracting(DiagnosisResponse.SingleHouseholdSeriesPoint::yearMonth)
			.containsExactly("2026-03", "2026-04", "2026-05", "2026-06", "2026-07", "2026-08");
		assertThat(response.singleHouseholdComparison().tabs().getFirst().series())
			.extracting(DiagnosisResponse.SingleHouseholdSeriesPoint::averageUsage)
			.containsExactly(
				new BigDecimal("189.658"), new BigDecimal("184.783"),
				new BigDecimal("179.013"), new BigDecimal("186.260"),
				new BigDecimal("228.449"), new BigDecimal("257.617")
			);
		assertThat(response.singleHouseholdComparison().tabs().getFirst().series())
			.extracting(DiagnosisResponse.SingleHouseholdSeriesPoint::myUsage)
			.containsExactly(null, null, null, null, new BigDecimal("10.000"), new BigDecimal("10.000"));
		assertThat(response.singleHouseholdComparison().tabs().get(1).available()).isTrue();
		assertThat(response.singleHouseholdComparison().tabs().get(2).available()).isTrue();
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
		assertThat(response.singleHouseholdComparison().tabs().getFirst().available()).isFalse();
		assertThat(response.singleHouseholdComparison().tabs().getFirst().averageUsage()).isNull();
		assertThat(response.singleHouseholdComparison().tabs().getFirst().series()).isEmpty();
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

	private static Baseline baseline(
		UtilityType utilityType,
		String averageUsage,
		UsageUnit usageUnit
	) {
		return new Baseline(
			utilityType,
			utilityType == UtilityType.WATER ? "서울 아파트 1인 가구" : "전국 1인 가구",
			new BigDecimal(averageUsage),
			usageUnit,
			"공식 출처",
			"2023",
			BaselineCalculationBasis.WEIGHTED_MONTHLY_MICRODATA_AVERAGE,
			"환산 참고값"
		);
	}

	private void mockSeriesBaselines(
		UtilityType utilityType,
		UsageUnit usageUnit,
		String... monthlyAverages
	) {
		for (int offset = 0; offset < monthlyAverages.length; offset++) {
			YearMonth month = TARGET_MONTH.minusMonths(monthlyAverages.length - 1L - offset);
			when(baselineCatalog.find(month, utilityType))
				.thenReturn(Optional.of(baseline(utilityType, monthlyAverages[offset], usageUnit)));
		}
	}
}
