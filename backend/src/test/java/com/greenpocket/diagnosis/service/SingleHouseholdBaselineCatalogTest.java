package com.greenpocket.diagnosis.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import java.util.stream.IntStream;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import com.greenpocket.diagnosis.dto.BaselineCalculationBasis;
import com.greenpocket.global.type.UtilityType;

class SingleHouseholdBaselineCatalogTest {

	private final SingleHouseholdBaselineCatalog catalog = new SingleHouseholdBaselineCatalog(
		new ObjectMapper()
	);

	@Test
	void returnsWeightedMonthlyMicrodataAveragesForElectricityAndGas() {
		var januaryElectricity = catalog.find(YearMonth.of(2026, 1), UtilityType.ELECTRICITY)
			.orElseThrow();
		var electricity = catalog.find(YearMonth.of(2026, 8), UtilityType.ELECTRICITY).orElseThrow();
		var gas = catalog.find(YearMonth.of(2026, 8), UtilityType.GAS).orElseThrow();

		assertThat(januaryElectricity.averageUsage()).isEqualByComparingTo("209.029");
		assertThat(electricity.averageUsage()).isEqualByComparingTo("257.617");
		assertThat(gas.averageUsage()).isEqualByComparingTo("16.781");
		assertThat(electricity.referencePeriod()).isEqualTo("2023");
		assertThat(electricity.calculationBasis())
			.isEqualTo(BaselineCalculationBasis.WEIGHTED_MONTHLY_MICRODATA_AVERAGE);
		assertThat(monthlyValues(UtilityType.ELECTRICITY)).containsExactly(
			"209.029", "204.551", "189.658", "184.783", "179.013", "186.260",
			"228.449", "257.617", "223.761", "192.937", "194.265", "205.169"
		);
		assertThat(monthlyValues(UtilityType.GAS)).containsExactly(
			"84.072", "86.620", "68.353", "47.285", "34.182", "23.414",
			"18.890", "16.781", "19.792", "29.590", "45.764", "65.299"
		);
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

	private java.util.List<String> monthlyValues(UtilityType utilityType) {
		return IntStream.rangeClosed(1, 12)
			.mapToObj(month -> catalog.find(YearMonth.of(2026, month), utilityType).orElseThrow())
			.map(baseline -> baseline.averageUsage().toPlainString())
			.toList();
	}
}
