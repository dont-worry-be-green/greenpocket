package com.greenpocket.diagnosis.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

import com.greenpocket.diagnosis.entity.RegionLevel;
import com.greenpocket.diagnosis.entity.RegionUtilitySnapshot;
import com.greenpocket.global.type.UtilityType;

public record DiagnosisBaselineResponse(
	boolean found,
	RegionLevel regionLevel,
	String sidoCode,
	String sigunguCode,
	String baseMonth,
	UtilityType utilityType,
	Long householdCount,
	BigDecimal avgUsage,
	Long avgAmount,
	String sourceName,
	OffsetDateTime extractedAt
) {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

	public static DiagnosisBaselineResponse found(RegionUtilitySnapshot snapshot) {
		return new DiagnosisBaselineResponse(
			true,
			snapshot.getRegionLevel(),
			snapshot.getSidoCode(),
			snapshot.getSigunguCode(),
			YearMonth.from(snapshot.getBaseMonth()).toString(),
			snapshot.getUtilityType(),
			snapshot.getHouseholdCount(),
			snapshot.getAvgUsage(),
			snapshot.getAvgAmount(),
			snapshot.getSourceName(),
			snapshot.getExtractedAt().atZone(KOREA_ZONE_ID).toOffsetDateTime()
		);
	}

	public static DiagnosisBaselineResponse notFound(
		String sidoCode,
		String sigunguCode,
		UtilityType utilityType
	) {
		return new DiagnosisBaselineResponse(
			false,
			null,
			sidoCode,
			sigunguCode,
			null,
			utilityType,
			null,
			null,
			null,
			null,
			null
		);
	}
}
