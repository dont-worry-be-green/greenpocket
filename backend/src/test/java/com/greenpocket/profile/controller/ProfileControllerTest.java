package com.greenpocket.profile.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.greenpocket.global.auth.CurrentUserIdArgumentResolver;
import com.greenpocket.global.auth.DemoKeyAuthenticationInterceptor;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.global.exception.GlobalExceptionHandler;
import com.greenpocket.profile.dto.ProfileResponse;
import com.greenpocket.profile.dto.ProfileSaveRequest;
import com.greenpocket.profile.dto.ProfileSaveResponse;
import com.greenpocket.profile.dto.ProfileUpdateRequest;
import com.greenpocket.profile.entity.AreaBand;
import com.greenpocket.profile.entity.HousingType;
import com.greenpocket.profile.service.ProfileService;

class ProfileControllerTest {

	private static final Long USER_ID = 1L;

	private ProfileService service;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		service = mock(ProfileService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new ProfileController(service))
			.setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void savesProfileForAuthenticatedUser() throws Exception {
		ProfileSaveRequest request = new ProfileSaveRequest(
			"11", "서울특별시", "11620", "관악구", HousingType.APARTMENT, AreaBand.OVER_20
		);
		when(service.save(USER_ID, request)).thenReturn(new ProfileSaveResponse(
			true, "서울 관악구 · 아파트 20평 이상", "WF-06", true
		));

		mockMvc.perform(post("/api/v1/profile")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(profileJson()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.onboardingCompleted").value(true))
			.andExpect(jsonPath("$.data.nextScreen").value("WF-06"));
	}

	@Test
	void findsProfileAndRejectsMissingAuthentication() throws Exception {
		when(service.find(USER_ID)).thenReturn(new ProfileResponse(
			"김그린", "11", "서울특별시", "11620", "관악구",
			HousingType.APARTMENT, AreaBand.OVER_20,
			"서울 관악구 · 아파트 20평 이상", true, true
		));

		mockMvc.perform(get("/api/v1/profile")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.sigunguName").value("관악구"));

		mockMvc.perform(get("/api/v1/profile"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED_DEMO_KEY"));
	}

	@Test
	void returnsConflictWhenBaselineChangeNeedsConfirmation() throws Exception {
		ProfileUpdateRequest request = new ProfileUpdateRequest(
			"김그린", "11", "서울특별시", "11710", "송파구",
			HousingType.APARTMENT, AreaBand.OVER_20, false
		);
		when(service.update(USER_ID, request)).thenThrow(new BusinessException(
			CommonErrorCode.CONFLICT,
			"지역 변경 확인이 필요해요.",
			"confirmBaselineChange",
			Map.of("affectedRoundId", 7L)
		));

		mockMvc.perform(put("/api/v1/profile")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"name":"김그린","sidoCode":"11","sidoName":"서울특별시","sigunguCode":"11710",
					 "sigunguName":"송파구","housingType":"APARTMENT","areaBand":"OVER_20",
					 "confirmBaselineChange":false}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("CONFLICT"))
			.andExpect(jsonPath("$.error.field").value("confirmBaselineChange"))
			.andExpect(jsonPath("$.error.details.affectedRoundId").value(7));
	}

	private String profileJson() {
		return """
			{"sidoCode":"11","sidoName":"서울특별시","sigunguCode":"11620","sigunguName":"관악구",
			 "housingType":"APARTMENT","areaBand":"OVER_20"}
			""";
	}
}
