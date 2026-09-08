package com.greenpocket.pocket.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.greenpocket.pocket.dto.PocketRecommendedProductResponse;

class PocketProductRecommendationServiceTest {

	private final PocketProductRecommendationService service = new PocketProductRecommendationService();

	@Test
	void returnsFixedKbCleanAirSavingsProduct() {
		PocketRecommendedProductResponse response = service.getRecommendedProduct();

		assertThat(response.productCode()).isEqualTo("DP01000942");
		assertThat(response.name()).isEqualTo("KB맑은하늘적금");
		assertThat(response.recommendation().badge()).isEqualTo("그린포켓 추천");
		assertThat(response.monthlyDeposit().minimumAmount()).isEqualTo(10_000L);
		assertThat(response.monthlyDeposit().maximumAmount()).isEqualTo(1_000_000L);
		assertThat(response.contractTermsMonths()).containsExactly(12, 24, 36);
		assertThat(response.preferentialMissions()).containsExactly(
			"종이통장 줄이기",
			"비대면 가입",
			"대중교통 이용",
			"미세먼지 퀴즈"
		);
		assertThat(response.informationBaseDate()).isEqualTo(LocalDate.of(2026, 8, 26));
		assertThat(response.applicationUrl())
			.isEqualTo("https://obank.kbstar.com/quics?cc=b061761:b061770&isNew=N&page=C020702&prcode=DP01000942");
	}
}
