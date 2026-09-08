package com.greenpocket.policy.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.greenpocket.global.auth.CurrentUserIdArgumentResolver;
import com.greenpocket.global.auth.DemoKeyAuthenticationInterceptor;
import com.greenpocket.global.exception.GlobalExceptionHandler;
import com.greenpocket.policy.dto.PolicyListResponse;
import com.greenpocket.policy.dto.PolicyPreviewRequest;
import com.greenpocket.policy.service.PolicyQueryService;
import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.HouseholdStatus;
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
		PolicyListResponse response = emptyResponse(false);
		when(service.getRecommendations(USER_ID, 0, 20)).thenReturn(response);
		when(service.getAll(USER_ID, "청년", PolicyInterestCategory.JOB, "11620", null, 0, 20))
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
	void routesPreviewWithoutPersistingRequest() throws Exception {
		PolicyPreviewRequest request = new PolicyPreviewRequest(
			CurrentStatus.UNEMPLOYED, AnnualIncomeBand.NO_INCOME, HouseholdStatus.ONE_PERSON, 0, 20
		);
		when(service.preview(USER_ID, request)).thenReturn(emptyResponse(true));

		mockMvc.perform(post("/api/v1/policies/recommendations/preview")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "currentStatus":"UNEMPLOYED", "annualIncomeBand":"NO_INCOME",
					  "householdStatus":"ONE_PERSON",
					  "page":0, "size":20
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.preview").value(true));
	}

	@Test
	void rejectsInvalidPageSizeAtControllerBoundary() throws Exception {
		mockMvc.perform(get("/api/v1/policies")
				.param("size", "101")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
	}

	@Test
	void documentsPreviewAuthenticationAndProfileErrors() throws Exception {
		Method method = PolicyController.class.getDeclaredMethod(
			"preview", Long.class, PolicyPreviewRequest.class
		);

		assertThat(Arrays.stream(method.getAnnotation(ApiResponses.class).value())
			.map(response -> response.responseCode()))
			.contains("200", "400", "401", "409", "503");
	}

	private PolicyListResponse emptyResponse(boolean preview) {
		return new PolicyListResponse(List.of(), 0, 20, 0, 0, false, null, null, preview);
	}
}
