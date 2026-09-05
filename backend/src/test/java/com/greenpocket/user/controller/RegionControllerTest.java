package com.greenpocket.user.controller;

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

import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.GlobalExceptionHandler;
import com.greenpocket.profile.exception.ProfileErrorCode;
import com.greenpocket.user.dto.RegionListResponse;
import com.greenpocket.user.service.RegionService;

class RegionControllerTest {

	private RegionService service;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		service = mock(RegionService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new RegionController(service))
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void returnsSeoulDistricts() throws Exception {
		when(service.findRegions("11")).thenReturn(new RegionListResponse(
			RegionListResponse.Level.SIGUNGU,
			List.of(new RegionListResponse.Item("11620", "관악구", "11", true))
		));

		mockMvc.perform(get("/api/v1/meta/regions").param("sidoCode", "11"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.level").value("SIGUNGU"))
			.andExpect(jsonPath("$.data.items[0].code").value("11620"))
			.andExpect(jsonPath("$.data.items[0].hasRegionAverage").value(true));
	}

	@Test
	void returnsRegionNotFound() throws Exception {
		when(service.findRegions("26"))
			.thenThrow(new BusinessException(ProfileErrorCode.REGION_NOT_FOUND, "sidoCode", null));

		mockMvc.perform(get("/api/v1/meta/regions").param("sidoCode", "26"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("REGION_NOT_FOUND"))
			.andExpect(jsonPath("$.error.field").value("sidoCode"));
	}
}
