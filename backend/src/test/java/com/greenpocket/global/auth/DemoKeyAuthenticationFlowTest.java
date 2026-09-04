package com.greenpocket.global.auth;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.global.exception.GlobalExceptionHandler;
import com.greenpocket.global.response.ApiResponse;

class DemoKeyAuthenticationFlowTest {

	private static final String DEMO_KEY = "9f2c1a7e-4b30-4c88-9a11-6d0e5b7c2f41";

	private DemoUserLookup demoUserLookup;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		demoUserLookup = mock(DemoUserLookup.class);
		DemoKeyAuthenticationInterceptor interceptor = new DemoKeyAuthenticationInterceptor(demoUserLookup);
		CurrentUserIdArgumentResolver argumentResolver = new CurrentUserIdArgumentResolver();

		mockMvc = MockMvcBuilders.standaloneSetup(new AuthenticationTestController())
			.addInterceptors(interceptor)
			.setCustomArgumentResolvers(argumentResolver)
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void registeredDemoKeyResolvesCurrentUserAndReturnsWrappedResponse() throws Exception {
		when(demoUserLookup.findUserIdByDemoKey(DEMO_KEY)).thenReturn(Optional.of(42L));

		mockMvc.perform(get("/api/v1/test-authentication")
				.header(DemoKeyAuthenticationInterceptor.DEMO_KEY_HEADER, DEMO_KEY))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.userId").value(42))
			.andExpect(jsonPath("$.error").doesNotExist())
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void missingDemoKeyReturnsUnauthenticatedResponse() throws Exception {
		mockMvc.perform(get("/api/v1/test-authentication"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED_DEMO_KEY"))
			.andExpect(jsonPath("$.error.message").value("데모 사용자를 찾을 수 없어요. 처음부터 시작해 주세요."));
	}

	@Test
	void malformedDemoKeyReturnsUnauthenticatedResponse() throws Exception {
		mockMvc.perform(get("/api/v1/test-authentication")
				.header(DemoKeyAuthenticationInterceptor.DEMO_KEY_HEADER, "not-a-uuid"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED_DEMO_KEY"));
	}

	@Test
	void unregisteredDemoKeyReturnsUnauthenticatedResponse() throws Exception {
		when(demoUserLookup.findUserIdByDemoKey(DEMO_KEY)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/test-authentication")
				.header(DemoKeyAuthenticationInterceptor.DEMO_KEY_HEADER, DEMO_KEY))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED_DEMO_KEY"));
	}

	@RestController
	private static class AuthenticationTestController {

		@GetMapping("/api/v1/test-authentication")
		ApiResponse<CurrentUserResponse> authenticate(@CurrentUserId Long userId) {
			return ApiResponse.success(new CurrentUserResponse(userId));
		}
	}

	private record CurrentUserResponse(Long userId) {
	}
}
