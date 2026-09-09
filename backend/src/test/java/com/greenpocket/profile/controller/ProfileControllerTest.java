package com.greenpocket.profile.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.greenpocket.global.auth.CurrentUserIdArgumentResolver;
import com.greenpocket.global.auth.DemoKeyAuthenticationInterceptor;
import com.greenpocket.global.exception.GlobalExceptionHandler;
import com.greenpocket.profile.dto.PolicyPreferencesRequest;
import com.greenpocket.profile.dto.PolicyPreferencesResponse;
import com.greenpocket.profile.dto.PolicyPreferencesUpdateResponse;
import com.greenpocket.profile.dto.ProfileResponse;
import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.EducationStatus;
import com.greenpocket.profile.entity.PolicyInterestCategory;
import com.greenpocket.profile.service.ProfileService;
import com.greenpocket.user.entity.Gender;

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
	void findsProfileWithEcoAddress() throws Exception {
		when(service.find(USER_ID)).thenReturn(new ProfileResponse(
			"김그린", LocalDate.of(1998, 3, 15), Gender.FEMALE, "01091740339",
			CurrentStatus.EMPLOYED, AnnualIncomeBand.FROM_24M_TO_36M, EducationStatus.UNIVERSITY_GRADUATE,
			List.of(PolicyInterestCategory.JOB),
			new ProfileResponse.EcoAddress("서울특별시 관악구", "11", "11620", "2026-03"),
			true
		));

		mockMvc.perform(get("/api/v1/profile")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.birthDate").value("1998-03-15"))
			.andExpect(jsonPath("$.data.gender").value("FEMALE"))
			.andExpect(jsonPath("$.data.ecoAddress.sigunguCode").value("11620"));

		mockMvc.perform(get("/api/v1/profile"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
	}

	@Test
	void getsAndSavesPolicyPreferences() throws Exception {
		PolicyPreferencesRequest request = new PolicyPreferencesRequest(
			CurrentStatus.EMPLOYED, AnnualIncomeBand.FROM_24M_TO_36M,
			EducationStatus.UNIVERSITY_GRADUATE, List.of(PolicyInterestCategory.JOB)
		);
		when(service.findPolicyPreferences(USER_ID)).thenReturn(new PolicyPreferencesResponse(
			LocalDate.of(1998, 3, 15), request.currentStatus(), request.annualIncomeBand(),
			request.educationStatus(), request.interestCategories(), null, false, false, true
		));
		when(service.updatePolicyPreferences(USER_ID, request))
			.thenReturn(new PolicyPreferencesUpdateResponse(true, true));

		mockMvc.perform(get("/api/v1/profile/policy-preferences")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.regionEditable").value(false));

		mockMvc.perform(put("/api/v1/profile/policy-preferences")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(profileJson()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.recommendationsUpdated").value(true));
	}

	private String profileJson() {
		return """
			{
			  "currentStatus":"EMPLOYED",
			  "annualIncomeBand":"FROM_24M_TO_36M",
			  "educationStatus":"UNIVERSITY_GRADUATE",
			  "interestCategories":["JOB"]
			}
			""";
	}
}
