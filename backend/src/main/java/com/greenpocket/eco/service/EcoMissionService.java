package com.greenpocket.eco.service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.eco.dto.EcoMissionLogRequest;
import com.greenpocket.eco.dto.EcoMissionLogResponse;
import com.greenpocket.eco.dto.EcoTodayMissionsResponse;
import com.greenpocket.eco.exception.EcoErrorCode;
import com.greenpocket.eco.repository.EcoMissionRepository;
import com.greenpocket.eco.repository.EcoMissionRepository.TodayMissionSnapshot;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;

@Service
public class EcoMissionService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])");
	private static final String SEASON_FILTERED_EMPTY = "SEASON_FILTERED_EMPTY";

	private final EcoMissionRepository ecoMissionRepository;
	private final Clock clock;

	@Autowired
	public EcoMissionService(EcoMissionRepository ecoMissionRepository) {
		this(ecoMissionRepository, Clock.system(KOREA_ZONE_ID));
	}

	EcoMissionService(EcoMissionRepository ecoMissionRepository, Clock clock) {
		this.ecoMissionRepository = ecoMissionRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public EcoTodayMissionsResponse getTodayMissions(Long userId, Long roundId, String dateValue) {
		validateRound(userId, roundId);
		LocalDate date = dateValue == null ? LocalDate.now(clock) : parseDate(dateValue);
		String season = season(date);
		List<TodayMissionSnapshot> missions = ecoMissionRepository.findTodayMissions(
			userId,
			roundId,
			date,
			season
		);
		int completedCount = (int)missions.stream().filter(TodayMissionSnapshot::completed).count();

		return new EcoTodayMissionsResponse(
			date.toString(),
			season,
			completedCount,
			missions.size(),
			missions.stream().map(this::toResponse).toList(),
			missions.isEmpty() ? SEASON_FILTERED_EMPTY : null
		);
	}

	@Transactional
	public EcoMissionLogResponse saveMissionLog(
		Long userId,
		Long roundId,
		String dateValue,
		EcoMissionLogRequest request
	) {
		validateRound(userId, roundId);
		LocalDate date = parseDate(dateValue);
		List<TodayMissionSnapshot> missions = ecoMissionRepository.findTodayMissions(
			userId,
			roundId,
			date,
			season(date)
		);
		List<Long> completedMissionIds = distinctCompletedMissionIds(request);
		Set<Long> availableMissionIds = missions.stream()
			.map(TodayMissionSnapshot::missionId)
			.collect(java.util.stream.Collectors.toSet());
		List<Long> invalidMissionIds = completedMissionIds.stream()
			.filter(missionId -> !availableMissionIds.contains(missionId))
			.toList();
		if (!invalidMissionIds.isEmpty()) {
			throw invalidRequest("completedMissionIds", Map.of("invalidMissionIds", invalidMissionIds));
		}

		ecoMissionRepository.saveDailyLog(userId, roundId, date, completedMissionIds);
		return new EcoMissionLogResponse(date.toString(), completedMissionIds.size(), missions.size());
	}

	private void validateRound(Long userId, Long roundId) {
		ecoMissionRepository.findOwnedRoundId(userId, roundId)
			.orElseThrow(() -> new BusinessException(EcoErrorCode.ECO_ROUND_NOT_FOUND));
	}

	private LocalDate parseDate(String value) {
		if (value == null || !DATE_PATTERN.matcher(value).matches()) {
			throw invalidDate();
		}
		try {
			return LocalDate.parse(value);
		}
		catch (DateTimeException exception) {
			throw invalidDate();
		}
	}

	private List<Long> distinctCompletedMissionIds(EcoMissionLogRequest request) {
		if (request == null || request.completedMissionIds() == null
			|| request.completedMissionIds().stream().anyMatch(java.util.Objects::isNull)) {
			throw invalidRequest("completedMissionIds", Map.of("required", true));
		}
		return List.copyOf(new LinkedHashSet<>(request.completedMissionIds()));
	}

	private EcoTodayMissionsResponse.Mission toResponse(TodayMissionSnapshot mission) {
		return new EcoTodayMissionsResponse.Mission(
			mission.missionId(),
			mission.title(),
			mission.utilityType(),
			mission.difficulty(),
			mission.completed()
		);
	}

	private String season(LocalDate date) {
		return switch (date.getMonthValue()) {
			case 3, 4, 5 -> "SPRING";
			case 6, 7, 8 -> "SUMMER";
			case 9, 10, 11 -> "AUTUMN";
			default -> "WINTER";
		};
	}

	private BusinessException invalidDate() {
		return invalidRequest("date", Map.of("expectedFormat", "YYYY-MM-DD"));
	}

	private BusinessException invalidRequest(String field, Map<String, Object> details) {
		return new BusinessException(CommonErrorCode.INVALID_REQUEST, field, details);
	}
}
