package com.greenpocket.pocket.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.greenpocket.pocket.dto.PocketRecommendedProductResponse;

@Service
public class PocketProductRecommendationService {

	private static final PocketRecommendedProductResponse RECOMMENDED_PRODUCT =
		new PocketRecommendedProductResponse(
			"DP01000942",
			"KB맑은하늘적금",
			"맑은하늘 만들고 금리도 Up",
			new PocketRecommendedProductResponse.Recommendation(
				"그린포켓 추천",
				"친환경 실천과 가장 잘 어울리는 적금",
				"맑은하늘을 위한 생활 속 작은 실천에 우대금리를 제공해요."
			),
			"자유적립식",
			new PocketRecommendedProductResponse.MonthlyDeposit(10_000L, 1_000_000L),
			List.of(12, 24, 36),
			List.of("종이통장 줄이기", "비대면 가입", "대중교통 이용", "미세먼지 퀴즈"),
			LocalDate.of(2026, 8, 26),
			"https://obank.kbstar.com/quics?cc=b061761:b061770&isNew=N&page=C020702&prcode=DP01000942",
			"금리와 우대 조건은 가입 시점에 KB국민은행에서 확인해 주세요."
		);

	public PocketRecommendedProductResponse getRecommendedProduct() {
		return RECOMMENDED_PRODUCT;
	}
}
