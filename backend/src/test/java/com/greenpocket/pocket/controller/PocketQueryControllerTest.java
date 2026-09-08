package com.greenpocket.pocket.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.greenpocket.global.auth.CurrentUserIdArgumentResolver;
import com.greenpocket.global.auth.DemoKeyAuthenticationInterceptor;
import com.greenpocket.global.exception.GlobalExceptionHandler;
import com.greenpocket.pocket.dto.ConvertibleMileageResponse;
import com.greenpocket.pocket.dto.PocketBalanceResponse;
import com.greenpocket.pocket.dto.PocketMainResponse;
import com.greenpocket.pocket.dto.PocketManagementResponse;
import com.greenpocket.pocket.dto.PocketRecommendedProductResponse;
import com.greenpocket.pocket.dto.PocketTransactionListResponse;
import com.greenpocket.pocket.entity.TransactionDirection;
import com.greenpocket.pocket.entity.TransactionType;
import com.greenpocket.pocket.service.PocketProductRecommendationService;
import com.greenpocket.pocket.service.PocketQueryService;

class PocketQueryControllerTest {

	private static final Long USER_ID = 42L;

	private PocketQueryService pocketQueryService;
	private PocketProductRecommendationService pocketProductRecommendationService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		pocketQueryService = mock(PocketQueryService.class);
		pocketProductRecommendationService = mock(PocketProductRecommendationService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new PocketQueryController(
			pocketQueryService,
			pocketProductRecommendationService
		))
			.setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void returnsPocketMain() throws Exception {
		when(pocketQueryService.getPocket(USER_ID)).thenReturn(new PocketMainResponse(
			new PocketMainResponse.Pocket("1005-1234-5678-90", "김수현"),
			64_000L,
			new PocketMainResponse.Breakdown(40_000L, 24_000L),
			30_000L,
			new PocketMainResponse.ConvertibleSource(7L, "2026-04", "2026-09"),
			null,
			List.of(),
			new PocketMainResponse.Empty(true, true),
			List.of("마일리지 전환은 1일 1회만 가능해요")
		));

		performGet("/api/v1/pocket")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.pocket.accountNo").value("1005-1234-5678-90"))
			.andExpect(jsonPath("$.data.balance").value(64000))
			.andExpect(jsonPath("$.data.empty.noAccount").value(true));
	}

	@Test
	void returnsPocketBalance() throws Exception {
		when(pocketQueryService.getBalance(USER_ID)).thenReturn(new PocketBalanceResponse(
			64_000L,
			30_000L,
			OffsetDateTime.parse("2026-09-04T10:00:00+09:00")
		));

		performGet("/api/v1/pocket/balance")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.balance").value(64000))
			.andExpect(jsonPath("$.data.convertibleMileage").value(30000));
	}

	@Test
	void returnsConvertibleMileage() throws Exception {
		when(pocketQueryService.getConvertibleMileage(USER_ID)).thenReturn(new ConvertibleMileageResponse(
			30_000L,
			List.of(new ConvertibleMileageResponse.Round(7L, "2026-04", "2026-09", 30_000L)),
			true,
			null
		));

		performGet("/api/v1/pocket/convertible-mileage")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.rounds[0].roundId").value(7))
			.andExpect(jsonPath("$.data.convertible").value(true));
	}

	@Test
	void returnsFilteredTransactions() throws Exception {
		when(pocketQueryService.getTransactions(
			USER_ID, TransactionDirection.CREDIT, TransactionType.GREENLIFE, 0, 20
		)).thenReturn(new PocketTransactionListResponse(
			64_000L, 64_000L, 30_000L, List.of(), 0, 20, 0, 0, false
		));

		mockMvc.perform(get("/api/v1/pocket/transactions")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.param("direction", "CREDIT")
				.param("type", "GREENLIFE")
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.groups").isEmpty())
			.andExpect(jsonPath("$.data.totalCreditAmount").value(64000));
	}

	@Test
	void returnsPocketManagement() throws Exception {
		when(pocketQueryService.getManagement(USER_ID)).thenReturn(new PocketManagementResponse(
			new PocketManagementResponse.Pocket("1005-1234-5678-90", "김수현", 64_000L),
			List.of(),
			List.of()
		));

		performGet("/api/v1/pocket/management")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.pocket.holder").value("김수현"))
			.andExpect(jsonPath("$.data.recentWithdrawals").isEmpty());
	}

	@Test
	void returnsRecommendedProduct() throws Exception {
		when(pocketProductRecommendationService.getRecommendedProduct()).thenReturn(
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
				java.time.LocalDate.of(2026, 8, 26),
				"https://obank.kbstar.com/quics?prcode=DP01000942",
				"금리와 우대 조건은 가입 시점에 KB국민은행에서 확인해 주세요."
			)
		);

		performGet("/api/v1/pocket/recommended-product")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.productCode").value("DP01000942"))
			.andExpect(jsonPath("$.data.name").value("KB맑은하늘적금"))
			.andExpect(jsonPath("$.data.recommendation.badge").value("그린포켓 추천"))
			.andExpect(jsonPath("$.data.monthlyDeposit.minimumAmount").value(10000))
			.andExpect(jsonPath("$.data.contractTermsMonths[2]").value(36))
			.andExpect(jsonPath("$.data.preferentialMissions[2]").value("대중교통 이용"))
			.andExpect(jsonPath("$.data.informationBaseDate").value("2026-08-26"))
			.andExpect(jsonPath("$.data.applicationUrl").value(
				"https://obank.kbstar.com/quics?prcode=DP01000942"
			));
	}

	@Test
	void rejectsMissingAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/pocket"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
	}

	@Test
	void rejectsRecommendedProductWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/pocket/recommended-product"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
	}

	@Test
	void rejectsInvalidTransactionFilter() throws Exception {
		mockMvc.perform(get("/api/v1/pocket/transactions")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.param("direction", "INVALID"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
	}

	@Test
	void rejectsInvalidTransactionPaging() throws Exception {
		mockMvc.perform(get("/api/v1/pocket/transactions")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.param("page", "-1")
				.param("size", "101"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
	}

	private org.springframework.test.web.servlet.ResultActions performGet(String path) throws Exception {
		return mockMvc.perform(get(path)
			.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID));
	}
}
