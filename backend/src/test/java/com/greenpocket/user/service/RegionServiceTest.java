package com.greenpocket.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.diagnosis.service.DiagnosisRegionAvailabilityService;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.user.dto.RegionListResponse;

class RegionServiceTest {

	private DiagnosisRegionAvailabilityService availabilityService;
	private RegionService regionService;

	@BeforeEach
	void setUp() {
		availabilityService = mock(DiagnosisRegionAvailabilityService.class);
		regionService = new RegionService(new SeoulRegionCatalog(new ObjectMapper()), availabilityService);
	}

	@Test
	void returnsOnlySeoulForSidoList() {
		when(availabilityService.hasSidoAverage("11")).thenReturn(false);

		RegionListResponse response = regionService.findRegions(null);

		assertThat(response.level()).isEqualTo(RegionListResponse.Level.SIDO);
		assertThat(response.items()).singleElement().satisfies(item -> {
			assertThat(item.code()).isEqualTo("11");
			assertThat(item.name()).isEqualTo("서울특별시");
			assertThat(item.hasRegionAverage()).isFalse();
		});
	}

	@Test
	void returnsAllTwentyFiveDistrictsAndDatabaseAvailability() {
		when(availabilityService.findSigunguCodesWithAverage("11")).thenReturn(Set.of("11620"));

		RegionListResponse response = regionService.findRegions("11");

		assertThat(response.level()).isEqualTo(RegionListResponse.Level.SIGUNGU);
		assertThat(response.items()).hasSize(25);
		assertThat(response.items()).filteredOn(item -> item.code().equals("11620"))
			.singleElement().satisfies(item -> {
				assertThat(item.name()).isEqualTo("관악구");
				assertThat(item.hasRegionAverage()).isTrue();
			});
		assertThat(response.items()).filteredOn(RegionListResponse.Item::hasRegionAverage).hasSize(1);
	}

	@Test
	void rejectsUnsupportedSido() {
		assertThatThrownBy(() -> regionService.findRegions("26"))
			.isInstanceOfSatisfying(BusinessException.class, exception -> {
				assertThat(exception.getErrorCode().code()).isEqualTo("REGION_NOT_FOUND");
				assertThat(exception.getField()).isEqualTo("sidoCode");
			});
	}
}
