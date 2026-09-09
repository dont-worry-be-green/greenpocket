package com.greenpocket.policy.controller;

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
import com.greenpocket.global.exception.GlobalExceptionHandler;
import com.greenpocket.policy.dto.PolicyListResponse;
import com.greenpocket.policy.service.PolicyQueryService;
import com.greenpocket.profile.entity.PolicyInterestCategory;

class PolicyControllerTest {

	private static final Long USER_ID = 1L;

	private PolicyQueryService service;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		service = mock(PolicyQueryService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new PolicyController(service))
			.setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void routesRecommendationAndCatalogRequests() throws Exception {
		PolicyListResponse response = emptyResponse();
		when(service.getRecommendations(USER_ID)).thenReturn(response);
		when(service.getAll(USER_ID, "청년", PolicyInterestCategory.JOB, "11620", 0, 20))
			.thenReturn(response);

		mockMvc.perform(get("/api/v1/policies/recommendations")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isEmpty());

		mockMvc.perform(get("/api/v1/policies")
				.param("keyword", "청년")
				.param("category", "JOB")
				.param("regionCode", "11620")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.page").value(0));
	}

	@Test
	void rejectsInvalidPageSizeAtControllerBoundary() throws Exception {
		mockMvc.perform(get("/api/v1/policies")
				.param("size", "101")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
	}

	private PolicyListResponse emptyResponse() {
		return new PolicyListResponse(List.of(), 0, 20, 0, 0, false, null, null);
	}
}
