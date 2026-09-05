package com.greenpocket.user.service;

import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import com.greenpocket.diagnosis.service.DiagnosisRegionAvailabilityService;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.profile.exception.ProfileErrorCode;
import com.greenpocket.user.dto.RegionListResponse;

@Service
@RequiredArgsConstructor
public class RegionService {

	private final SeoulRegionCatalog regionCatalog;
	private final DiagnosisRegionAvailabilityService regionAvailabilityService;

	public RegionListResponse findRegions(String sidoCode) {
		SeoulRegionCatalog.Sido seoul = regionCatalog.sido();
		if (sidoCode == null || sidoCode.isBlank()) {
			return new RegionListResponse(
				RegionListResponse.Level.SIDO,
				List.of(new RegionListResponse.Item(
					seoul.code(),
					seoul.name(),
					seoul.code(),
					regionAvailabilityService.hasSidoAverage(seoul.code())
				))
			);
		}
		if (!regionCatalog.supportsSido(sidoCode)) {
			throw new BusinessException(ProfileErrorCode.REGION_NOT_FOUND, "sidoCode", null);
		}

		Set<String> availableCodes = regionAvailabilityService.findSigunguCodesWithAverage(sidoCode);
		List<RegionListResponse.Item> items = regionCatalog.sigungu().stream()
			.map(region -> new RegionListResponse.Item(
				region.code(),
				region.name(),
				sidoCode,
				availableCodes.contains(region.code())
			))
			.toList();
		return new RegionListResponse(RegionListResponse.Level.SIGUNGU, items);
	}
}
