package com.greenpocket.profile.service;

import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.eco.service.EcoCurrentRoundQueryService;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.profile.dto.ProfileResponse;
import com.greenpocket.profile.dto.ProfileSaveRequest;
import com.greenpocket.profile.dto.ProfileSaveResponse;
import com.greenpocket.profile.dto.ProfileUpdateRequest;
import com.greenpocket.profile.dto.ProfileUpdateResponse;
import com.greenpocket.profile.entity.AreaBand;
import com.greenpocket.profile.entity.HousingType;
import com.greenpocket.profile.exception.ProfileErrorCode;
import com.greenpocket.profile.repository.ProfileRepository;
import com.greenpocket.profile.repository.ProfileRepository.ProfileSnapshot;
import com.greenpocket.user.exception.UserErrorCode;
import com.greenpocket.user.service.SeoulRegionCatalog;

@Service
@RequiredArgsConstructor
public class ProfileService {

	private static final String NEXT_SCREEN = "WF-06";
	private static final String BASELINE_CHANGE_WARNING =
		"지역을 변경하면 진행 중인 평가 기준과 진단 비교 지역이 바뀔 수 있어요. 계속하시겠어요?";

	private final ProfileRepository profileRepository;
	private final SeoulRegionCatalog regionCatalog;
	private final EcoCurrentRoundQueryService ecoCurrentRoundQueryService;

	@Transactional
	public ProfileSaveResponse save(Long userId, ProfileSaveRequest request) {
		validateRequired(request.sidoCode(), request.sigunguCode(), request.housingType(), request.areaBand());
		ProfileSnapshot current = findUser(userId);
		SeoulRegionCatalog.Sigungu sigungu = resolveRegion(request.sidoCode(), request.sigunguCode());
		SeoulRegionCatalog.Sido sido = regionCatalog.sido();

		update(current.name(), userId, sido, sigungu, request.housingType(), request.areaBand());
		return new ProfileSaveResponse(
			true,
			profileSummary(sigungu.name(), request.housingType(), request.areaBand()),
			NEXT_SCREEN,
			true
		);
	}

	@Transactional(readOnly = true)
	public ProfileResponse find(Long userId) {
		ProfileSnapshot profile = findUser(userId);
		validateComplete(profile);
		return toResponse(profile);
	}

	@Transactional
	public ProfileUpdateResponse update(Long userId, ProfileUpdateRequest request) {
		validateRequired(request.sidoCode(), request.sigunguCode(), request.housingType(), request.areaBand());
		ProfileSnapshot current = findUser(userId);
		validateComplete(current);
		SeoulRegionCatalog.Sigungu sigungu = resolveRegion(request.sidoCode(), request.sigunguCode());
		SeoulRegionCatalog.Sido sido = regionCatalog.sido();
		String name = request.name() == null ? current.name() : normalizeAndValidateName(request.name());
		boolean regionChanged = !request.sidoCode().equals(current.sidoCode())
			|| !request.sigunguCode().equals(current.sigunguCode());
		Optional<Long> activeRoundId = regionChanged
			? ecoCurrentRoundQueryService.findGoalActiveRoundId(userId)
			: Optional.empty();
		if (activeRoundId.isPresent() && !Boolean.TRUE.equals(request.confirmBaselineChange())) {
			throw new BusinessException(
				CommonErrorCode.CONFLICT,
				BASELINE_CHANGE_WARNING,
				"confirmBaselineChange",
				Map.of("warning", BASELINE_CHANGE_WARNING, "affectedRoundId", activeRoundId.get())
			);
		}

		update(name, userId, sido, sigungu, request.housingType(), request.areaBand());
		return new ProfileUpdateResponse(
			profileSummary(sigungu.name(), request.housingType(), request.areaBand()),
			regionChanged,
			activeRoundId.orElse(null)
		);
	}

	private void update(
		String name,
		Long userId,
		SeoulRegionCatalog.Sido sido,
		SeoulRegionCatalog.Sigungu sigungu,
		HousingType housingType,
		AreaBand areaBand
	) {
		if (profileRepository.update(
			userId, name, sido.code(), sido.name(), sigungu.code(), sigungu.name(), housingType, areaBand
		) != 1) {
			throw unauthenticated();
		}
	}

	private ProfileResponse toResponse(ProfileSnapshot profile) {
		return new ProfileResponse(
			profile.name(),
			profile.sidoCode(),
			profile.sidoName(),
			profile.sigunguCode(),
			profile.sigunguName(),
			profile.housingType(),
			profile.areaBand(),
			profileSummary(profile.sigunguName(), profile.housingType(), profile.areaBand()),
			"11".equals(profile.sidoCode()),
			profile.onboardingCompleted()
		);
	}

	private SeoulRegionCatalog.Sigungu resolveRegion(String sidoCode, String sigunguCode) {
		return regionCatalog.findSigungu(sidoCode, sigunguCode)
			.orElseThrow(() -> new BusinessException(ProfileErrorCode.REGION_NOT_FOUND, "sigunguCode", null));
	}

	private ProfileSnapshot findUser(Long userId) {
		return profileRepository.findByUserId(userId).orElseThrow(ProfileService::unauthenticated);
	}

	private static void validateRequired(
		String sidoCode,
		String sigunguCode,
		HousingType housingType,
		AreaBand areaBand
	) {
		if (sidoCode == null || sidoCode.isBlank() || sigunguCode == null || sigunguCode.isBlank()
			|| housingType == null || areaBand == null) {
			throw new BusinessException(ProfileErrorCode.PROFILE_INCOMPLETE);
		}
	}

	private static void validateComplete(ProfileSnapshot profile) {
		if (!profile.onboardingCompleted() || profile.sidoCode() == null || profile.sigunguCode() == null
			|| profile.housingType() == null || profile.areaBand() == null) {
			throw new BusinessException(ProfileErrorCode.PROFILE_INCOMPLETE);
		}
	}

	private static String normalizeAndValidateName(String rawName) {
		String name = rawName.strip();
		int characterCount = name.codePointCount(0, name.length());
		boolean hasLetterOrDigit = name.codePoints().anyMatch(Character::isLetterOrDigit);
		if (characterCount < 1 || characterCount > 20 || !hasLetterOrDigit) {
			throw new BusinessException(UserErrorCode.NAME_INVALID, "name", null);
		}
		return name;
	}

	private static String profileSummary(String sigunguName, HousingType housingType, AreaBand areaBand) {
		return "서울 " + sigunguName + " · " + housingType.label() + " " + areaBand.label();
	}

	private static BusinessException unauthenticated() {
		return new BusinessException(CommonErrorCode.UNAUTHENTICATED_DEMO_KEY);
	}
}
