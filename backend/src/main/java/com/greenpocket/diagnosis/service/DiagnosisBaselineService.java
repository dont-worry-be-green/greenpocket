package com.greenpocket.diagnosis.service;

import java.time.YearMonth;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import com.greenpocket.diagnosis.dto.DiagnosisBaselineResponse;
import com.greenpocket.global.type.UtilityType;

@Service
@RequiredArgsConstructor
public class DiagnosisBaselineService {

	private final SingleHouseholdBaselineCatalog baselineCatalog;

	public DiagnosisBaselineResponse findBaseline(YearMonth month, UtilityType utilityType) {
		return baselineCatalog.find(month, utilityType)
			.map(baseline -> DiagnosisBaselineResponse.found(month, baseline))
			.orElseGet(() -> DiagnosisBaselineResponse.notFound(month, utilityType));
	}
}
