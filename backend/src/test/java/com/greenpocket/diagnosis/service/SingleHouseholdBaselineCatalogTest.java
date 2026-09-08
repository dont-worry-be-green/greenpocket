package com.greenpocket.diagnosis.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import com.greenpocket.diagnosis.dto.BaselineCalculationBasis;
import com.greenpocket.global.type.UtilityType;

class SingleHouseholdBaselineCatalogTest {

	private final SingleHouseholdBaselineCatalog catalog = new SingleHouseholdBaselineCatalog(
		new ObjectMapper()
	);

	@Test
	void calculatesOfficialAnnualEnergyStatisticsAsMonthlyUsage() {
		var electricity = catalog.find(YearMonth.of(2026, 8), UtilityType.ELECTRICITY).orElseThrow();
		var gas = catalog.find(YearMonth.of(2026, 8), UtilityType.GAS).orElseThrow();

		assertThat(electricity.averageUsage()).isEqualByComparingTo("247.633");
		assertThat(gas.averageUsage()).isEqualByComparingTo("25.429");
		assertThat(electricity.calculationBasis())
			.isEqualTo(BaselineCalculationBasis.ANNUAL_ENERGY_SHARE_MONTHLY_EQUIVALENT);
	}

	@Test
	void convertsDailyWaterUsageUsingActualDaysInRequestedMonth() {
		var february = catalog.find(YearMonth.of(2026, 2), UtilityType.WATER).orElseThrow();
		var august = catalog.find(YearMonth.of(2026, 8), UtilityType.WATER).orElseThrow();

		assertThat(february.averageUsage()).isEqualByComparingTo("12.264");
		assertThat(august.averageUsage()).isEqualByComparingTo("13.578");
		assertThat(august.comparisonLabel()).isEqualTo("서울 아파트 1인 가구");
		assertThat(august.calculationBasis())
			.isEqualTo(BaselineCalculationBasis.DAILY_USAGE_MONTH_EQUIVALENT);
	}
}
