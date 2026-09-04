package com.greenpocket.eco.dto;

public record EcoMissionLogResponse(
	String date,
	int completedCount,
	int totalCount
) {
}
