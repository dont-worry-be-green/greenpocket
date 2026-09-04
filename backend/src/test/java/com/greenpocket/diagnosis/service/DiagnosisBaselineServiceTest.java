package com.greenpocket.diagnosis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.diagnosis.dto.DiagnosisBaselineResponse;
import com.greenpocket.diagnosis.entity.RegionLevel;
import com.greenpocket.diagnosis.entity.RegionUtilitySnapshot;
import com.greenpocket.diagnosis.repository.RegionUtilitySnapshotRepository;
import com.greenpocket.global.type.UtilityType;
import com.greenpocket.user.service.UserRegionQueryService;

class DiagnosisBaselineServiceTest {

	private static final Long USER_ID = 1L;
	private static final String SIDO_CODE = "11";
	private static final String SIGUNGU_CODE = "11620";
	private static final YearMonth REQUEST_MONTH = YearMonth.of(2026, 8);

	private RegionUtilitySnapshotRepository regionUtilitySnapshotRepository;
	private UserRegionQueryService userRegionQueryService;
	private DiagnosisBaselineService diagnosisBaselineService;

	@BeforeEach
	void setUp() {
		regionUtilitySnapshotRepository = mock(RegionUtilitySnapshotRepository.class);
		userRegionQueryService = mock(UserRegionQueryService.class);
		diagnosisBaselineService = new DiagnosisBaselineService(
			regionUtilitySnapshotRepository,
			userRegionQueryService
		);
		when(userRegionQueryService.findSidoCode(USER_ID)).thenReturn(Optional.of(SIDO_CODE));
	}

	@Test
	void returnsLatestAvailableSigunguBaseline() {
		RegionUtilitySnapshot snapshot = snapshot(
			RegionLevel.SIGUNGU,
			SIGUNGU_CODE,
			LocalDate.of(2026, 7, 1)
		);
		when(findBaseline(RegionLevel.SIGUNGU, SIGUNGU_CODE)).thenReturn(Optional.of(snapshot));

		DiagnosisBaselineResponse response = diagnosisBaselineService.findBaseline(
			USER_ID,
			SIGUNGU_CODE,
			REQUEST_MONTH,
			UtilityType.ELECTRICITY
		);

		assertThat(response.found()).isTrue();
		assertThat(response.regionLevel()).isEqualTo(RegionLevel.SIGUNGU);
		assertThat(response.baseMonth()).isEqualTo("2026-07");
		assertThat(response.avgAmount()).isEqualTo(38_900L);
	}

	@Test
	void fallsBackToLatestAvailableSidoBaseline() {
		RegionUtilitySnapshot snapshot = snapshot(
			RegionLevel.SIDO,
			"",
			LocalDate.of(2026, 7, 1)
		);
		when(findBaseline(RegionLevel.SIGUNGU, SIGUNGU_CODE)).thenReturn(Optional.empty());
		when(findBaseline(RegionLevel.SIDO, "")).thenReturn(Optional.of(snapshot));

		DiagnosisBaselineResponse response = diagnosisBaselineService.findBaseline(
			USER_ID,
			SIGUNGU_CODE,
			REQUEST_MONTH,
			UtilityType.ELECTRICITY
		);

		assertThat(response.found()).isTrue();
		assertThat(response.regionLevel()).isEqualTo(RegionLevel.SIDO);
		assertThat(response.sidoCode()).isEqualTo(SIDO_CODE);
		assertThat(response.sigunguCode()).isEmpty();
	}

	@Test
	void returnsNormalNotFoundResponseWhenNoBaselineExists() {
		when(findBaseline(RegionLevel.SIGUNGU, SIGUNGU_CODE)).thenReturn(Optional.empty());
		when(findBaseline(RegionLevel.SIDO, "")).thenReturn(Optional.empty());

		DiagnosisBaselineResponse response = diagnosisBaselineService.findBaseline(
			USER_ID,
			SIGUNGU_CODE,
			REQUEST_MONTH,
			UtilityType.GAS
		);

		assertThat(response.found()).isFalse();
		assertThat(response.regionLevel()).isNull();
		assertThat(response.sidoCode()).isEqualTo(SIDO_CODE);
		assertThat(response.sigunguCode()).isEqualTo(SIGUNGU_CODE);
		assertThat(response.utilityType()).isEqualTo(UtilityType.GAS);
	}

	private Optional<RegionUtilitySnapshot> findBaseline(RegionLevel regionLevel, String sigunguCode) {
		return regionUtilitySnapshotRepository
			.findFirstByRegionLevelAndSidoCodeAndSigunguCodeAndUtilityTypeAndBaseMonthLessThanEqualAndAvgUsageIsNotNullAndAvgAmountIsNotNullOrderByBaseMonthDesc(
				regionLevel,
				SIDO_CODE,
				sigunguCode,
				UtilityType.ELECTRICITY,
				REQUEST_MONTH.atDay(1)
			);
	}

	private RegionUtilitySnapshot snapshot(
		RegionLevel regionLevel,
		String sigunguCode,
		LocalDate baseMonth
	) {
		RegionUtilitySnapshot snapshot = mock(RegionUtilitySnapshot.class);
		when(snapshot.getRegionLevel()).thenReturn(regionLevel);
		when(snapshot.getSidoCode()).thenReturn(SIDO_CODE);
		when(snapshot.getSigunguCode()).thenReturn(sigunguCode);
		when(snapshot.getBaseMonth()).thenReturn(baseMonth);
		when(snapshot.getUtilityType()).thenReturn(UtilityType.ELECTRICITY);
		when(snapshot.getHouseholdCount()).thenReturn(132_840L);
		when(snapshot.getAvgUsage()).thenReturn(new BigDecimal("289.400"));
		when(snapshot.getAvgAmount()).thenReturn(38_900L);
		when(snapshot.getSourceName()).thenReturn("한국전력공사 전력데이터 개방포털");
		when(snapshot.getExtractedAt()).thenReturn(LocalDateTime.of(2026, 8, 28, 0, 0));
		return snapshot;
	}
}
