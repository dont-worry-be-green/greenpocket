package com.greenpocket.profile.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.greenpocket.profile.dto.ProfileSaveRequest;
import com.greenpocket.profile.dto.ProfileSaveResponse;
import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.AreaBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.HouseholdStatus;
import com.greenpocket.profile.entity.HousingType;
import com.greenpocket.profile.entity.PolicyInterestCategory;
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
			LocalDate.of(1998, 3, 15), HousingType.APARTMENT, AreaBand.OVER_20,
			CurrentStatus.EMPLOYED, AnnualIncomeBand.FROM_24M_TO_36M,
			HouseholdStatus.ONE_PERSON, List.of(PolicyInterestCategory.JOB)
		);
		when(service.save(USER_ID, request)).thenReturn(new ProfileSaveResponse(
			true, true, "아파트 · 20평 이상", "WF-06", false
		));

		mockMvc.perform(post("/api/v1/profile")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(profileJson()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.onboardingCompleted").value(true))
			.andExpect(jsonPath("$.data.policyProfileCompleted").value(true))
			.andExpect(jsonPath("$.data.policyRegionLinked").value(false));
	}

	@Test
	void findsProfileWithEcoAddress() throws Exception {
		when(service.find(USER_ID)).thenReturn(new ProfileResponse(
			"김그린", LocalDate.of(1998, 3, 15), HousingType.APARTMENT, AreaBand.OVER_20,
			CurrentStatus.EMPLOYED, AnnualIncomeBand.FROM_24M_TO_36M, HouseholdStatus.ONE_PERSON,
			List.of(PolicyInterestCategory.JOB),
			new ProfileResponse.EcoAddress("서울특별시 관악구", "11", "11620", "2026-03"),
			"아파트 · 20평 이상", true, true
		));

		mockMvc.perform(get("/api/v1/profile")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.birthDate").value("1998-03-15"))
			.andExpect(jsonPath("$.data.ecoAddress.sigunguCode").value("11620"));

		mockMvc.perform(get("/api/v1/profile"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
	}

	@Test
	void getsAndSavesPolicyPreferences() throws Exception {
		PolicyPreferencesRequest request = new PolicyPreferencesRequest(
			LocalDate.of(1998, 3, 15), HousingType.APARTMENT, AreaBand.OVER_20,
			CurrentStatus.EMPLOYED, AnnualIncomeBand.FROM_24M_TO_36M,
			HouseholdStatus.ONE_PERSON, List.of(PolicyInterestCategory.JOB)
		);
		when(service.findPolicyPreferences(USER_ID)).thenReturn(new PolicyPreferencesResponse(
			request.birthDate(), request.housingType(), request.areaBand(), request.currentStatus(),
			request.annualIncomeBand(), request.householdStatus(), request.interestCategories(), null, false
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
			  "birthDate":"1998-03-15",
			  "housingType":"APARTMENT",
			  "areaBand":"OVER_20",
			  "currentStatus":"EMPLOYED",
			  "annualIncomeBand":"FROM_24M_TO_36M",
			  "householdStatus":"ONE_PERSON",
			  "interestCategories":["JOB"]
			}
			""";
	}
}
