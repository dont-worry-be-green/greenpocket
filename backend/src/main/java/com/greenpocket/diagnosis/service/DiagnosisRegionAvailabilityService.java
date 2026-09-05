package com.greenpocket.diagnosis.service;

import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.diagnosis.entity.RegionLevel;
import com.greenpocket.diagnosis.repository.RegionUtilitySnapshotRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiagnosisRegionAvailabilityService {

	private static final String SIDO_SIGUNGU_CODE = "";

	private final RegionUtilitySnapshotRepository regionUtilitySnapshotRepository;

	public boolean hasSidoAverage(String sidoCode) {
		return regionUtilitySnapshotRepository.existsByRegionLevelAndSidoCodeAndSigunguCode(
			RegionLevel.SIDO,
			sidoCode,
			SIDO_SIGUNGU_CODE
		);
	}

	public Set<String> findSigunguCodesWithAverage(String sidoCode) {
		return Set.copyOf(regionUtilitySnapshotRepository.findDistinctSigunguCodes(
			RegionLevel.SIGUNGU,
			sidoCode
		));
	}
}
