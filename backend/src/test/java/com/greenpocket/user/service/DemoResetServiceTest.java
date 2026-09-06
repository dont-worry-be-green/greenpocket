package com.greenpocket.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.user.dto.DemoResetRequest;
import com.greenpocket.user.repository.UserRepository;

class DemoResetServiceTest {

	private static final String DEMO_KEY = "84cc0ab0-4fba-477d-8434-fcee3be057ab";

	@Test
	void deletesUserAndReturnsOnboardingScreen() {
		UserRepository repository = mock(UserRepository.class);
		Clock clock = Clock.fixed(Instant.parse("2026-09-06T01:00:00Z"), ZoneId.of("Asia/Seoul"));
		DemoResetService service = new DemoResetService(repository, clock);

		var response = service.reset(new DemoResetRequest(DEMO_KEY));

		verify(repository).deleteByDemoKey(DEMO_KEY);
		assertThat(response.resetAt().toString()).isEqualTo("2026-09-06T10:00+09:00");
		assertThat(response.nextScreen()).isEqualTo("ONB-01");
	}

	@Test
	void rejectsMalformedDemoKeyWithoutDeleting() {
		UserRepository repository = mock(UserRepository.class);
		DemoResetService service = new DemoResetService(repository, Clock.systemUTC());

		assertThatThrownBy(() -> service.reset(new DemoResetRequest("not-a-uuid")))
			.isInstanceOfSatisfying(BusinessException.class, exception -> {
				assertThat(exception.getErrorCode().code()).isEqualTo("INVALID_REQUEST");
				assertThat(exception.getField()).isEqualTo("demoKey");
			});
		verify(repository, never()).deleteByDemoKey("not-a-uuid");
	}
}
