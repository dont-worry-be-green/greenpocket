package com.greenpocket.diagnosis.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import tools.jackson.databind.ObjectMapper;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.greenpocket.diagnosis.dto.BaselineCalculationBasis;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

@Component
public class SingleHouseholdBaselineCatalog {

	private static final String RESOURCE_PATH = "data/single-household-utility-baselines.json";
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");
	private static final int USAGE_SCALE = 3;
	private static final int MONTHS_IN_YEAR = 12;

	private final Map<UtilityType, BaselineDefinition> definitions;

	public SingleHouseholdBaselineCatalog(ObjectMapper objectMapper) {
		this.definitions = index(load(objectMapper));
	}

	public Optional<Baseline> find(YearMonth targetMonth, UtilityType utilityType) {
		BaselineDefinition definition = definitions.get(utilityType);
		if (definition == null) {
			return Optional.empty();
		}
		return Optional.of(new Baseline(
			utilityType,
			definition.comparisonLabel(),
			calculateAverageUsage(targetMonth, definition),
			definition.usageUnit(),
			definition.sourceName(),
			definition.referencePeriod(),
			definition.calculationBasis(),
			definition.note()
		));
	}

	private static Catalog load(ObjectMapper objectMapper) {
		try (InputStream inputStream = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
			return objectMapper.readValue(inputStream, Catalog.class);
		}
		catch (IOException exception) {
			throw new IllegalStateException("1인 가구 사용량 기준 데이터를 읽지 못했습니다.", exception);
		}
	}

	private static Map<UtilityType, BaselineDefinition> index(Catalog catalog) {
		if (catalog.items() == null || catalog.items().size() != UtilityType.values().length) {
			throw new IllegalStateException("1인 가구 사용량 기준은 전기·도시가스·수도 3종이어야 합니다.");
		}
		Map<UtilityType, BaselineDefinition> indexed = new EnumMap<>(UtilityType.class);
		for (BaselineDefinition definition : catalog.items()) {
			validate(definition);
			if (indexed.putIfAbsent(definition.utilityType(), definition) != null) {
				throw new IllegalStateException("1인 가구 사용량 기준에 중복 에너지원이 있습니다.");
			}
		}
		return Collections.unmodifiableMap(indexed);
	}

	private static void validate(BaselineDefinition definition) {
		if (definition.utilityType() == null || definition.comparisonLabel() == null
			|| definition.usageUnit() == null || definition.sourceName() == null
			|| definition.referencePeriod() == null || definition.calculationBasis() == null
			|| definition.note() == null) {
			throw new IllegalStateException("1인 가구 사용량 기준의 필수 메타데이터가 없습니다.");
		}
		switch (definition.calculationBasis()) {
			case WEIGHTED_MONTHLY_MICRODATA_AVERAGE -> {
				if (definition.monthlyAverageUsage() == null
					|| definition.monthlyAverageUsage().size() != MONTHS_IN_YEAR
					|| definition.monthlyAverageUsage().stream().anyMatch(value -> !positive(value))) {
					throw new IllegalStateException("월별 마이크로데이터 평균은 1월부터 12월까지 양수 12개여야 합니다.");
				}
			}
			case ANNUAL_ENERGY_SHARE_MONTHLY_EQUIVALENT -> {
				if (!positive(definition.annualTotalEnergyMcal())
					|| !positive(definition.energyShareRate())
					|| !positive(definition.mcalPerUsageUnit())) {
					throw new IllegalStateException("연간 에너지 소비 환산값이 올바르지 않습니다.");
				}
			}
			case DAILY_USAGE_MONTH_EQUIVALENT -> {
				if (!positive(definition.dailyUsage())) {
					throw new IllegalStateException("일 사용량 환산값이 올바르지 않습니다.");
				}
			}
		}
	}

	private static BigDecimal calculateAverageUsage(
		YearMonth targetMonth,
		BaselineDefinition definition
	) {
		return switch (definition.calculationBasis()) {
			case WEIGHTED_MONTHLY_MICRODATA_AVERAGE -> definition.monthlyAverageUsage()
				.get(targetMonth.getMonthValue() - 1)
				.setScale(USAGE_SCALE, RoundingMode.HALF_UP);
			case ANNUAL_ENERGY_SHARE_MONTHLY_EQUIVALENT -> definition.annualTotalEnergyMcal()
				.multiply(definition.energyShareRate())
				.divide(ONE_HUNDRED, 12, RoundingMode.HALF_UP)
				.divide(definition.mcalPerUsageUnit(), 12, RoundingMode.HALF_UP)
				.divide(MONTHS_PER_YEAR, USAGE_SCALE, RoundingMode.HALF_UP);
			case DAILY_USAGE_MONTH_EQUIVALENT -> definition.dailyUsage()
				.multiply(BigDecimal.valueOf(targetMonth.lengthOfMonth()))
				.setScale(USAGE_SCALE, RoundingMode.HALF_UP);
		};
	}

	private static boolean positive(BigDecimal value) {
		return value != null && value.signum() > 0;
	}

	private record Catalog(List<BaselineDefinition> items) {
	}

	private record BaselineDefinition(
		UtilityType utilityType,
		String comparisonLabel,
		UsageUnit usageUnit,
		String sourceName,
		String sourceUrl,
		String referencePeriod,
		BaselineCalculationBasis calculationBasis,
		BigDecimal annualTotalEnergyMcal,
		BigDecimal energyShareRate,
		BigDecimal mcalPerUsageUnit,
		BigDecimal dailyUsage,
		List<BigDecimal> monthlyAverageUsage,
		String note
	) {
	}

	public record Baseline(
		UtilityType utilityType,
		String comparisonLabel,
		BigDecimal averageUsage,
		UsageUnit usageUnit,
		String sourceName,
		String referencePeriod,
		BaselineCalculationBasis calculationBasis,
		String note
	) {
	}
}
