package com.greenpocket.eco.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.greenpocket.global.type.UtilityType;

public record EcoGoalRequest(
	List<Target> targets,
	List<Long> selectedMissionIds
) {

	public record Target(
		UtilityType utilityType,
		@Schema(allowableValues = {"TIER_5", "TIER_10", "TIER_15"}) String tier
	) {
	}
}
