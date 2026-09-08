package com.greenpocket.diagnosis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.diagnosis.dto.BaselineCalculationBasis;
import com.greenpocket.diagnosis.dto.DiagnosisBaselineResponse;
import com.greenpocket.diagnosis.service.SingleHouseholdBaselineCatalog.Baseline;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

class DiagnosisBaselineServiceTest {

	private static final YearMonth REQUEST_MONTH = YearMonth.of(2026, 8);

	private SingleHouseholdBaselineCatalog baselineCatalog;
	private DiagnosisBaselineService diagnosisBaselineService;

	@BeforeEach
	void setUp() {
		baselineCatalog = mock(SingleHouseholdBaselineCatalog.class);
		diagnosisBaselineService = new DiagnosisBaselineService(baselineCatalog);
	}

	@Test
	void returnsSingleHouseholdUsageBaseline() {
		Baseline baseline = new Baseline(
			UtilityType.ELECTRICITY,
			"전국 1인 가구",
			new BigDecimal("247.633"),
			UsageUnit.kWh,
			"산업통상자원부·에너지경제연구원 2022년 기준 13차 가구에너지패널조사",
			"2022",
			BaselineCalculationBasis.ANNUAL_ENERGY_SHARE_MONTHLY_EQUIVALENT,
			"월평균 환산 참고값"
		);
		when(baselineCatalog.find(REQUEST_MONTH, UtilityType.ELECTRICITY))
			.thenReturn(Optional.of(baseline));

		DiagnosisBaselineResponse response = diagnosisBaselineService.findBaseline(
			REQUEST_MONTH,
			UtilityType.ELECTRICITY
		);

		assertThat(response.found()).isTrue();
		assertThat(response.targetYearMonth()).isEqualTo("2026-08");
		assertThat(response.comparisonLabel()).isEqualTo("전국 1인 가구");
		assertThat(response.averageUsage()).isEqualByComparingTo("247.633");
		assertThat(response.usageUnit()).isEqualTo(UsageUnit.kWh);
	}

	@Test
	void returnsNormalNotFoundResponseWhenNoBaselineExists() {
		when(baselineCatalog.find(REQUEST_MONTH, UtilityType.GAS)).thenReturn(Optional.empty());

		DiagnosisBaselineResponse response = diagnosisBaselineService.findBaseline(
			REQUEST_MONTH,
			UtilityType.GAS
		);

		assertThat(response.found()).isFalse();
		assertThat(response.targetYearMonth()).isEqualTo("2026-08");
		assertThat(response.utilityType()).isEqualTo(UtilityType.GAS);
		assertThat(response.averageUsage()).isNull();
	}
}
