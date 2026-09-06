package com.greenpocket.user.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.user.dto.DemoResetRequest;
import com.greenpocket.user.dto.DemoResetResponse;
import com.greenpocket.user.repository.UserRepository;

@Service
public class DemoResetService {

	private static final String ONBOARDING_SCREEN = "ONB-01";
	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

	private final UserRepository userRepository;
	private final Clock clock;

	@Autowired
	public DemoResetService(UserRepository userRepository) {
		this(userRepository, Clock.system(KOREA_ZONE_ID));
	}

	DemoResetService(UserRepository userRepository, Clock clock) {
		this.userRepository = userRepository;
		this.clock = clock;
	}

	@Transactional
	public DemoResetResponse reset(DemoResetRequest request) {
		String demoKey = request == null ? null : request.demoKey();
		validateDemoKey(demoKey);
		userRepository.deleteByDemoKey(demoKey);
		return new DemoResetResponse(OffsetDateTime.now(clock), ONBOARDING_SCREEN);
	}

	private void validateDemoKey(String demoKey) {
		try {
			UUID uuid = UUID.fromString(demoKey);
			if (uuid.version() != 4 || !uuid.toString().equalsIgnoreCase(demoKey)) {
				throw new IllegalArgumentException();
			}
		}
		catch (RuntimeException exception) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "demoKey", null);
		}
	}
}
