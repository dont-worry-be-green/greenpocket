package com.greenpocket.greenlife.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.greenpocket.global.auth.CurrentUserIdArgumentResolver;
import com.greenpocket.global.auth.DemoKeyAuthenticationInterceptor;
import com.greenpocket.greenlife.dto.GreenlifeItemsResponse;
import com.greenpocket.greenlife.dto.GreenlifeItemDetailResponse;
import com.greenpocket.greenlife.dto.GreenlifeLinkResponse;
import com.greenpocket.greenlife.dto.GreenlifeStatusResponse;
import com.greenpocket.greenlife.entity.RewardStatus;
import com.greenpocket.greenlife.service.GreenlifeService;

class GreenlifeControllerTest {

	private static final Long USER_ID = 42L;

	private GreenlifeService greenlifeService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		greenlifeService = mock(GreenlifeService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new GreenlifeController(greenlifeService))
			.setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
			.build();
	}

	@Test
	void returnsWrappedParticipatingStatus() throws Exception {
		when(greenlifeService.getStatus(USER_ID, "2026-08")).thenReturn(new GreenlifeStatusResponse(
			true,
			"BN-02",
			OffsetDateTime.parse("2026-09-02T18:30:00+09:00"),
			null,
			null,
			"2026-08",
			new GreenlifeStatusResponse.MonthSummary(44, 5_540L, 3_140L, "2026-07"),
			new GreenlifeStatusResponse.Annual(2026, 18_600L, 70_000L, new BigDecimal("26.6"), false),
			"실적 반영까지 최소 3일~익월 말이 걸릴 수 있어요",
			2026
		));

		mockMvc.perform(get("/api/v1/greenlife/status")
				.param("month", "2026-08")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.screen").value("BN-02"))
			.andExpect(jsonPath("$.data.monthSummary.activityCount").value(44))
			.andExpect(jsonPath("$.data.annual.paidAmount").value(18_600));
	}

	@Test
	void returnsWrappedLinkResult() throws Exception {
		when(greenlifeService.link(USER_ID)).thenReturn(new GreenlifeLinkResponse(
			true,
			OffsetDateTime.parse("2026-09-02T18:30:00+09:00"),
			44,
			"BN-02"
		));

		mockMvc.perform(post("/api/v1/greenlife/link")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.participating").value(true))
			.andExpect(jsonPath("$.data.syncedActivityCount").value(44));
	}

	@Test
	void returnsWrappedItemsList() throws Exception {
		when(greenlifeService.getItems(USER_ID, "2026-08")).thenReturn(new GreenlifeItemsResponse(
			"2026-08",
			2026,
			List.of(new GreenlifeItemsResponse.Item(
				1L, "E_RECEIPT", "전자영수증", 10L, "건", "receipt", 1,
				new BigDecimal("24"), 240L, null, null, false
			)),
			17,
			6
		));

		mockMvc.perform(get("/api/v1/greenlife/items")
				.param("month", "2026-08")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.month").value("2026-08"))
			.andExpect(jsonPath("$.data.items[0].itemCode").value("E_RECEIPT"))
			.andExpect(jsonPath("$.data.totalCount").value(17))
			.andExpect(jsonPath("$.data.collapsedAfter").value(6));
	}

	@Test
	void returnsWrappedItemDetail() throws Exception {
		when(greenlifeService.getItemDetail(USER_ID, 1L, "2026-08"))
			.thenReturn(new GreenlifeItemDetailResponse(
				1L,
				"E_RECEIPT",
				"전자영수증",
				10L,
				"건",
				2026,
				List.of("전자영수증을 설정해요", "전자영수증을 받아요", "실적을 확인해요"),
				"2026-08",
				new BigDecimal("24"),
				240L,
				null,
				false,
				List.of(new GreenlifeItemDetailResponse.History(
					301L,
					OffsetDateTime.parse("2026-08-28T13:20:00+09:00"),
					BigDecimal.ONE,
					10L,
					RewardStatus.PENDING,
					null
				)),
				"https://cpoint.or.kr/netzero/entGuide/nv_entGuideList.do",
				OffsetDateTime.parse("2026-09-02T18:30:00+09:00"),
				"실적 반영까지 최소 3일~익월 말이 걸릴 수 있어요"
			));

		mockMvc.perform(get("/api/v1/greenlife/items/1")
				.param("month", "2026-08")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.itemCode").value("E_RECEIPT"))
			.andExpect(jsonPath("$.data.validCount").value(24))
			.andExpect(jsonPath("$.data.pendingAmount").value(240))
			.andExpect(jsonPath("$.data.practiceSteps.length()").value(3))
			.andExpect(jsonPath("$.data.history[0].rewardStatus").value("PENDING"));
	}
}
