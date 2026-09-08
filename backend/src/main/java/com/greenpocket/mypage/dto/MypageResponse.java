package com.greenpocket.mypage.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.global.type.UtilityType;
import com.greenpocket.policy.dto.PolicyCardResponse;

public record MypageResponse(
	Profile profile,
	Links links,
	EcoAddress ecoAddress,
	Integration integration,
	String pocketAccountNo,
	YouthPolicy youthPolicy
) {

	public record Profile(
		String name,
		LocalDate birthDate,
		String housingType,
		String areaBand,
		String profileSummary
	) {
	}

	public record Links(
		ArchiveLink billArchive,
		ArchiveLink reportArchive
	) {
	}

	public record ArchiveLink(
		long count,
		String screen
	) {
	}

	public record EcoAddress(
		String label,
		String registeredAt,
		String notice
	) {
	}

	public record Integration(
		EcoLinkStatus ecoLinkStatus,
		OffsetDateTime ecoLinkedAt,
		boolean greenlifeParticipating,
		OffsetDateTime greenlifeLinkedAt,
		List<UtilityType> registeredUtilities
	) {
	}

	public record YouthPolicy(
		boolean profileCompleted,
		boolean regionLinked,
		long recommendedCount,
		List<PolicyCardResponse> preview,
		OffsetDateTime lastSyncedAt
	) {
	}
}
