package com.greenpocket.diagnosis.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.diagnosis.dto.DiagnosisBaselineResponse;
import com.greenpocket.diagnosis.entity.RegionLevel;
import com.greenpocket.diagnosis.entity.RegionUtilitySnapshot;
import com.greenpocket.diagnosis.repository.RegionUtilitySnapshotRepository;
import com.greenpocket.global.type.UtilityType;
import com.greenpocket.user.service.UserRegionQueryService;
import com.greenpocket.user.service.UserRegionQueryService.UserDiagnosisProfile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiagnosisBaselineService {

	private static final String SIDO_SIGUNGU_CODE = "";

	private final RegionUtilitySnapshotRepository regionUtilitySnapshotRepository;
	private final UserRegionQueryService userRegionQueryService;

	public DiagnosisBaselineResponse findBaseline(
		Long userId,
		String sigunguCode,
		YearMonth month,
		UtilityType utilityType
	) {
		Optional<UserDiagnosisProfile> profile = userRegionQueryService.findDiagnosisProfile(userId)
			.filter(value -> value.sidoCode() != null && value.sigunguCode() != null);
		if (profile.isEmpty()) {
			return DiagnosisBaselineResponse.notFound(null, null, utilityType, "ECO_ADDRESS_REQUIRED");
		}
		String sidoCode = profile.get().sidoCode();
		String linkedSigunguCode = profile.get().sigunguCode();

		LocalDate latestBaseMonth = month.atDay(1);
		Optional<RegionUtilitySnapshot> sigunguBaseline = findLatestAvailable(
			RegionLevel.SIGUNGU,
			sidoCode,
			linkedSigunguCode,
			utilityType,
			latestBaseMonth
		);
		if (sigunguBaseline.isPresent()) {
			return DiagnosisBaselineResponse.found(sigunguBaseline.get());
		}

		return findLatestAvailable(
			RegionLevel.SIDO,
			sidoCode,
			SIDO_SIGUNGU_CODE,
			utilityType,
			latestBaseMonth
		)
			.map(DiagnosisBaselineResponse::found)
			.orElseGet(() -> DiagnosisBaselineResponse.notFound(
				sidoCode,
				linkedSigunguCode,
				utilityType,
				"NO_BASELINE"
			));
	}

	private Optional<RegionUtilitySnapshot> findLatestAvailable(
		RegionLevel regionLevel,
		String sidoCode,
		String sigunguCode,
		UtilityType utilityType,
		LocalDate latestBaseMonth
	) {
		return regionUtilitySnapshotRepository
			.findFirstByRegionLevelAndSidoCodeAndSigunguCodeAndUtilityTypeAndBaseMonthLessThanEqualAndAvgUsageIsNotNullAndAvgAmountIsNotNullOrderByBaseMonthDesc(
				regionLevel,
				sidoCode,
				sigunguCode,
				utilityType,
				latestBaseMonth
			);
	}
}
