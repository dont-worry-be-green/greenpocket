package com.greenpocket.mypage.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.greenpocket.global.auth.CurrentUserIdArgumentResolver;
import com.greenpocket.global.auth.DemoKeyAuthenticationInterceptor;
import com.greenpocket.mypage.dto.MypageResponse;
import com.greenpocket.mypage.dto.ReportListResponse;
import com.greenpocket.mypage.dto.ReportType;
import com.greenpocket.mypage.service.MypageService;
import com.greenpocket.mypage.service.ReportService;

class MypageControllerTest {

	private static final Long USER_ID = 42L;

	private MypageService mypageService;
	private ReportService reportService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mypageService = mock(MypageService.class);
		reportService = mock(ReportService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new MypageController(mypageService, reportService))
			.setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
			.build();
	}

	@Test
	void returnsMypageMain() throws Exception {
		when(mypageService.getMypage(USER_ID)).thenReturn(new MypageResponse(
			new MypageResponse.Profile(
				"김수현", "서울특별시", "관악구", "ONE_ROOM", "UNDER_10",
				"서울 관악구 · 원룸 · 10평 이하"
			),
			new MypageResponse.Links(
				new MypageResponse.ArchiveLink(14, "MY-03"),
				new MypageResponse.ArchiveLink(9, "MY-04")
			),
			null,
			null,
			"1005-1234-5678-90"
		));

		mockMvc.perform(get("/api/v1/mypage")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.profile.name").value("김수현"))
			.andExpect(jsonPath("$.data.links.billArchive.count").value(14))
			.andExpect(jsonPath("$.data.pocketAccountNo").value("1005-1234-5678-90"));
	}

	@Test
	void returnsFilteredReports() throws Exception {
		when(reportService.getReports(USER_ID, ReportType.ECO_RESULT, 2026, 0, 20))
			.thenReturn(new ReportListResponse(List.of(), 0, 20, 0, 0, false));

		mockMvc.perform(get("/api/v1/reports")
				.param("type", "ECO_RESULT")
				.param("year", "2026")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isEmpty())
			.andExpect(jsonPath("$.data.page").value(0))
			.andExpect(jsonPath("$.data.size").value(20));
	}
}
