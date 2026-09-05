package com.greenpocket.user.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.global.exception.GlobalExceptionHandler;
import com.greenpocket.user.dto.DemoResetRequest;
import com.greenpocket.user.dto.DemoResetResponse;
import com.greenpocket.user.service.DemoResetService;

class DemoControllerTest {

	private static final String DEMO_KEY = "84cc0ab0-4fba-477d-8434-fcee3be057ab";

	private DemoResetService service;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		service = mock(DemoResetService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new DemoController(service))
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void returnsResetResult() throws Exception {
		DemoResetRequest request = new DemoResetRequest(DEMO_KEY);
		when(service.reset(request)).thenReturn(new DemoResetResponse(
			OffsetDateTime.parse("2026-09-06T10:00:00+09:00"), "ONB-01"
		));

		mockMvc.perform(post("/api/v1/demo/reset")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"demoKey\":\"" + DEMO_KEY + "\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.nextScreen").value("ONB-01"));
	}

	@Test
	void returnsInvalidRequestForMalformedKey() throws Exception {
		DemoResetRequest request = new DemoResetRequest("not-a-uuid");
		when(service.reset(request)).thenThrow(new BusinessException(CommonErrorCode.INVALID_REQUEST, "demoKey", null));

		mockMvc.perform(post("/api/v1/demo/reset")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"demoKey\":\"not-a-uuid\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.error.field").value("demoKey"));
	}
}
