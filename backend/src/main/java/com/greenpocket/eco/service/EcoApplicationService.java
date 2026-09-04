package com.greenpocket.eco.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.eco.dto.EcoApplicationResponse;
import com.greenpocket.eco.entity.ApplicationStatus;
import com.greenpocket.eco.exception.EcoErrorCode;
import com.greenpocket.eco.repository.EcoApplicationRepository;
import com.greenpocket.eco.repository.EcoApplicationRepository.ApplicationSnapshot;
import com.greenpocket.global.exception.BusinessException;

@Service
public class EcoApplicationService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

	private final EcoApplicationRepository ecoApplicationRepository;
	private final Clock clock;

	@Autowired
	public EcoApplicationService(EcoApplicationRepository ecoApplicationRepository) {
		this(ecoApplicationRepository, Clock.system(KOREA_ZONE_ID));
	}

	EcoApplicationService(EcoApplicationRepository ecoApplicationRepository, Clock clock) {
		this.ecoApplicationRepository = ecoApplicationRepository;
		this.clock = clock;
	}

	@Transactional
	public EcoApplicationResponse apply(Long userId, Long roundId) {
		ApplicationSnapshot application = findApplication(userId, roundId);
		if (application.applicationStatus() == ApplicationStatus.APPLIED) {
			return toResponse(application);
		}

		LocalDateTime appliedAt = LocalDateTime.ofInstant(clock.instant(), KOREA_ZONE_ID);
		if (ecoApplicationRepository.markApplied(userId, roundId, appliedAt)) {
			return toResponse(new ApplicationSnapshot(roundId, ApplicationStatus.APPLIED, appliedAt));
		}

		return toResponse(findApplication(userId, roundId));
	}

	private ApplicationSnapshot findApplication(Long userId, Long roundId) {
		return ecoApplicationRepository.findByUserIdAndRoundId(userId, roundId)
			.orElseThrow(() -> new BusinessException(EcoErrorCode.ECO_ROUND_NOT_FOUND));
	}

	private EcoApplicationResponse toResponse(ApplicationSnapshot application) {
		return new EcoApplicationResponse(
			application.roundId(),
			application.applicationStatus(),
			toOffsetDateTime(application.appliedAt()),
			application.applicationStatus() != ApplicationStatus.APPLIED
		);
	}

	private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
		return value == null ? null : value.atZone(KOREA_ZONE_ID).toOffsetDateTime();
	}
}
