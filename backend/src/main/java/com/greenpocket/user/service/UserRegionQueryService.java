package com.greenpocket.user.service;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.user.repository.UserRegionQueryRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRegionQueryService {

	private final UserRegionQueryRepository userRegionQueryRepository;

	public Optional<String> findSidoCode(Long userId) {
		return userRegionQueryRepository.findSidoCodeByUserId(userId);
	}

	public Optional<UserDiagnosisProfile> findDiagnosisProfile(Long userId) {
		return userRegionQueryRepository.findDiagnosisProfileByUserId(userId)
			.map(snapshot -> new UserDiagnosisProfile(
				snapshot.sidoCode(),
				snapshot.sidoName(),
				snapshot.sigunguCode(),
				snapshot.sigunguName(),
				snapshot.housingType(),
				snapshot.areaBand()
			));
	}

	public record UserDiagnosisProfile(
		String sidoCode,
		String sidoName,
		String sigunguCode,
		String sigunguName,
		String housingType,
		String areaBand
	) {
		public String regionLabel() {
			return joinNonBlank(sidoName, sigunguName);
		}

		public String sidoLabel() {
			return sidoName;
		}

		public String profileSummary() {
			String residence = joinNonBlank(sidoName, sigunguName);
			String home = joinNonBlank(housingTypeLabel(housingType), areaBandLabel(areaBand));
			if (residence.isBlank()) {
				return home;
			}
			return home.isBlank() ? residence : residence + " · " + home;
		}

		private static String joinNonBlank(String left, String right) {
			if (left == null || left.isBlank()) {
				return right == null ? "" : right;
			}
			return right == null || right.isBlank() ? left : left + " " + right;
		}

		private static String housingTypeLabel(String value) {
			if (value == null) {
				return "";
			}
			return switch (value) {
				case "ONE_ROOM" -> "원룸";
				case "OFFICETEL" -> "오피스텔";
				case "APARTMENT" -> "아파트";
				case "MULTI_HOUSE" -> "다세대";
				default -> "";
			};
		}

		private static String areaBandLabel(String value) {
			if (value == null) {
				return "";
			}
			return switch (value) {
				case "UNDER_10" -> "10평 이하";
				case "FROM_10_TO_20" -> "10~20평";
				case "OVER_20" -> "20평 이상";
				default -> "";
			};
		}
	}
}
