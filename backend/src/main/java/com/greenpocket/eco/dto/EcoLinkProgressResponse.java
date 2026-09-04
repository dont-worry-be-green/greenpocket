package com.greenpocket.eco.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import com.greenpocket.eco.entity.JobStatus;
import com.greenpocket.global.type.UtilityType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EcoLinkProgressResponse(
	String linkJobId,
	JobStatus status,
	Integer elapsedSeconds,
	List<UtilityProgress> utilityStatus,
	OffsetDateTime linkedAt,
	Long roundId,
	List<UtilityType> registeredUtilities,
	Integer baselineMonthsLoaded,
	EcoAddress ecoAddress,
	String nextScreen
) {

	public static EcoLinkProgressResponse running(String linkJobId) {
		return new EcoLinkProgressResponse(
			linkJobId,
			JobStatus.RUNNING,
			12,
			List.of(
				new UtilityProgress(UtilityType.ELECTRICITY, JobStatus.SUCCEEDED),
				new UtilityProgress(UtilityType.GAS, JobStatus.SUCCEEDED),
				new UtilityProgress(UtilityType.WATER, JobStatus.RUNNING)
			),
			null,
			null,
			null,
			null,
			null,
			null
		);
	}

	public static EcoLinkProgressResponse succeeded(
		String linkJobId,
		OffsetDateTime linkedAt,
		Long roundId,
		EcoAddress ecoAddress
	) {
		return new EcoLinkProgressResponse(
			linkJobId,
			JobStatus.SUCCEEDED,
			null,
			null,
			linkedAt,
			roundId,
			List.of(UtilityType.ELECTRICITY, UtilityType.GAS, UtilityType.WATER),
			24,
			ecoAddress,
			"WF-03"
		);
	}

	public record UtilityProgress(
		UtilityType utilityType,
		JobStatus status
	) {
	}

	@Schema(name = "EcoLinkProgressAddress")
	public record EcoAddress(
		String label,
		String sidoCode,
		String sigunguCode,
		String registeredAt
	) {
	}
}
