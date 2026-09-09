package com.greenpocket.eco.dto;

import java.util.List;

import com.greenpocket.eco.entity.MissionDifficulty;
import com.greenpocket.global.type.UtilityType;

public record EcoTodayMissionsResponse(
	String date,
	String season,
	int completedCount,
	int totalCount,
	List<Mission> missions,
	String emptyReason
) {

	public record Mission(
		Long missionId,
		String title,
		UtilityType utilityType,
		MissionDifficulty difficulty,
		boolean completed
	) {
	}
}
