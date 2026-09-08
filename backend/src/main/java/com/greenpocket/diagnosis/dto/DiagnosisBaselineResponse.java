package com.greenpocket.diagnosis.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.greenpocket.diagnosis.service.SingleHouseholdBaselineCatalog.Baseline;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiagnosisBaselineResponse(
	boolean found,
	String targetYearMonth,
	UtilityType utilityType,
	String comparisonLabel,
	BigDecimal averageUsage,
	UsageUnit usageUnit,
	String sourceName,
	String referencePeriod,
	BaselineCalculationBasis calculationBasis,
	String note
) {

	public static DiagnosisBaselineResponse found(YearMonth targetMonth, Baseline baseline) {
		return new DiagnosisBaselineResponse(
			true,
			targetMonth.toString(),
			baseline.utilityType(),
			baseline.comparisonLabel(),
			baseline.averageUsage(),
			baseline.usageUnit(),
			baseline.sourceName(),
			baseline.referencePeriod(),
			baseline.calculationBasis(),
			baseline.note()
		);
	}

	public static DiagnosisBaselineResponse notFound(YearMonth targetMonth, UtilityType utilityType) {
		return new DiagnosisBaselineResponse(
			false,
			targetMonth.toString(),
			utilityType,
			null,
			null,
			null,
			null,
			null,
			null,
			null
		);
	}
}
