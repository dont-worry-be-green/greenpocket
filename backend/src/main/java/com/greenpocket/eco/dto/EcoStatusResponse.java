package com.greenpocket.eco.dto;

import java.time.OffsetDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.global.type.UtilityType;

public record EcoStatusResponse(
	EcoLinkStatus linkStatus,
	OffsetDateTime linkedAt,
	boolean seoulResident,
	boolean linkable,
	String blockReason,
	List<RegisteredUtility> registeredUtilities,
	boolean eligibleForRound,
	EcoAddress ecoAddress,
	String externalUrl
) {

	public record RegisteredUtility(
		UtilityType utilityType,
		boolean registered,
		String unregisteredReason
	) {
	}

	@Schema(name = "EcoStatusAddress")
	public record EcoAddress(
		String label,
		String sidoCode,
		String sigunguCode,
		String registeredAt,
		boolean matchesProfile
	) {
	}
}
