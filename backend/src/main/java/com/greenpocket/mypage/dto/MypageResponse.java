package com.greenpocket.mypage.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.global.type.UtilityType;

public record MypageResponse(
	Profile profile,
	Links links,
	EcoAddress ecoAddress,
	Integration integration,
	String pocketAccountNo
) {

	public record Profile(
		String name,
		String sidoName,
		String sigunguName,
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
		boolean matchesProfile,
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
}
