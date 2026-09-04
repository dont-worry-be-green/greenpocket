package com.greenpocket.eco.service;

import static com.greenpocket.global.type.UtilityType.ELECTRICITY;
import static com.greenpocket.global.type.UtilityType.GAS;
import static com.greenpocket.global.type.UtilityType.WATER;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.eco.dto.EcoGoalFormResponse;
import com.greenpocket.eco.dto.EcoGoalPreviewResponse;
import com.greenpocket.eco.dto.EcoGoalRequest;
import com.greenpocket.eco.dto.EcoGoalResponse;
import com.greenpocket.eco.dto.EcoGoalSaveResponse;
import com.greenpocket.eco.entity.RoundStatus;
import com.greenpocket.eco.entity.TargetTier;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.eco.exception.EcoErrorCode;
import com.greenpocket.eco.repository.EcoGoalRepository;
import com.greenpocket.eco.repository.EcoGoalRepository.GoalRoundSnapshot;
import com.greenpocket.eco.repository.EcoGoalRepository.GoalUtilitySnapshot;
import com.greenpocket.eco.repository.EcoGoalRepository.MissionSnapshot;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.global.type.UtilityType;

@Service
@RequiredArgsConstructor
public class EcoGoalService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private static final BigDecimal SIX = new BigDecimal("6");
	private static final BigDecimal ZERO_RATE = new BigDecimal("0.000");
	private static final String REGISTER_GUIDE_URL = "https://ecomileage.seoul.go.kr";

	private final EcoGoalRepository ecoGoalRepository;

	@Transactional(readOnly = true)
	public EcoGoalFormResponse getGoalForm(Long userId, Long roundId) {
		GoalRoundSnapshot round = findRound(userId, roundId);
		Map<UtilityType, GoalUtilitySnapshot> utilities = utilityMap(roundId);
		List<MissionSnapshot> missions = ecoGoalRepository.findActiveMissions();
		Set<Long> selectedMissionIds = ecoGoalRepository.findSavedMissions(roundId).stream()
			.map(EcoGoalRepository.SavedMissionSnapshot::missionId)
			.collect(Collectors.toSet());

		List<EcoGoalFormResponse.Segment> segments = List.of(UtilityType.values()).stream()
			.map(utilityType -> goalSegment(
				utilityType,
				utilities.get(utilityType),
				missions,
				selectedMissionIds
			))
			.toList();

		return new EcoGoalFormResponse(
			round.id(),
			YearMonth.from(round.periodStart()).toString(),
			YearMonth.from(round.periodEnd()).toString(),
			List.of(TargetTier.values()).stream()
				.map(tier -> new EcoGoalFormResponse.TierOption(
					tier,
					tier.label(),
					tier.targetRate(),
					tier.mileage()
				))
				.toList(),
			segments
		);
	}

	@Transactional(readOnly = true)
	public EcoGoalPreviewResponse preview(Long userId, Long roundId, EcoGoalRequest request) {
		return calculate(userId, roundId, request).response();
	}

	@Transactional
	public EcoGoalSaveResponse save(Long userId, Long roundId, EcoGoalRequest request) {
		GoalCalculation calculation = calculate(userId, roundId, request);
		EcoGoalPreviewResponse response = calculation.response();

		ecoGoalRepository.clearUtilityTargets(roundId);
		for (EcoGoalPreviewResponse.UtilityTarget target : response.utilities()) {
			ecoGoalRepository.updateUtilityTarget(
				roundId,
				target.utilityType(),
				calculation.targetTiers().get(target.utilityType()),
				target.targetRate(),
				target.targetUsage(),
				target.expectedSaving()
			);
		}
		ecoGoalRepository.updateRoundGoal(
			roundId,
			response.combined().combinedRate(),
			response.combined().expectedMileage(),
			response.combined().totalExpectedSaving()
		);
		ecoGoalRepository.deleteSavedMissions(roundId);
		for (CalculatedMission mission : calculation.missions()) {
			ecoGoalRepository.saveMission(
				userId,
				roundId,
				mission.mission().id(),
				mission.computedRate() == null ? ZERO_RATE : mission.computedRate(),
				mission.counted(),
				mission.exclusionReason()
			);
		}

		LocalDateTime goalSetAt = ecoGoalRepository.findRound(userId, roundId)
			.map(GoalRoundSnapshot::goalSetAt)
			.orElse(null);
		return new EcoGoalSaveResponse(
			roundId,
			toOffsetDateTime(goalSetAt),
			RoundStatus.GOAL_SET,
			response.combined().combinedRate(),
			response.combined().expectedMileage(),
			response.combined().totalExpectedSaving(),
			calculation.missions().size(),
			"WF-06"
		);
	}

	@Transactional(readOnly = true)
	public EcoGoalResponse getGoal(Long userId, Long roundId) {
		GoalRoundSnapshot round = findRound(userId, roundId);
		if (round.goalSetAt() == null) {
			return new EcoGoalResponse(roundId, false, null, null, null, null, null, null, null);
		}

		List<EcoGoalResponse.UtilityGoal> utilities = ecoGoalRepository.findUtilities(roundId).stream()
			.filter(value -> value.targetTier() != null)
			.map(value -> new EcoGoalResponse.UtilityGoal(
				value.utilityType(),
				value.targetTier(),
				value.targetRate(),
				value.baselineUsage(),
				value.targetUsage(),
				usageUnit(value.utilityType()),
				value.expectedSavingAmount()
			))
			.toList();
		List<EcoGoalResponse.SavedMission> missions = ecoGoalRepository.findSavedMissions(roundId).stream()
			.map(value -> new EcoGoalResponse.SavedMission(
				value.missionId(),
				value.title(),
				value.utilityType(),
				value.computedRate(),
				value.counted(),
				value.exclusionReason()
			))
			.toList();

		return new EcoGoalResponse(
			roundId,
			true,
			toOffsetDateTime(round.goalSetAt()),
			round.combinedTargetRate(),
			tierForRate(round.combinedTargetRate()),
			round.expectedMileage(),
			round.expectedSavingAmount(),
			utilities,
			missions
		);
	}

	private GoalCalculation calculate(Long userId, Long roundId, EcoGoalRequest request) {
		GoalRoundSnapshot round = findRound(userId, roundId);
		Map<UtilityType, GoalUtilitySnapshot> utilities = utilityMap(roundId);
		Map<UtilityType, TargetTier> targetTiers = parseTargets(request, utilities);

		List<EcoGoalPreviewResponse.UtilityTarget> targetResults = targetTiers.entrySet().stream()
			.map(entry -> calculateUtilityTarget(utilities.get(entry.getKey()), entry.getValue()))
			.toList();
		Map<UtilityType, EcoGoalPreviewResponse.UtilityTarget> targetResultMap = targetResults.stream()
			.collect(Collectors.toMap(EcoGoalPreviewResponse.UtilityTarget::utilityType, Function.identity()));

		BigDecimal baselineCarbon = ZERO_RATE;
		BigDecimal targetCarbon = ZERO_RATE;
		long baselineTotalAmount = 0L;
		List<UtilityType> excludedUtilities = new ArrayList<>();
		for (UtilityType utilityType : UtilityType.values()) {
			GoalUtilitySnapshot utility = utilities.get(utilityType);
			if (utility == null || !utility.registered()) {
				excludedUtilities.add(utilityType);
				continue;
			}
			if (utility.baselineUsage() == null || utility.carbonFactorG() == null) {
				throw invalidRequest("targets", Map.of("utilityType", utilityType.name()));
			}
			BigDecimal baselineUtilityCarbon = utility.baselineUsage().multiply(utility.carbonFactorG());
			baselineCarbon = baselineCarbon.add(baselineUtilityCarbon);
			EcoGoalPreviewResponse.UtilityTarget selectedTarget = targetResultMap.get(utilityType);
			BigDecimal targetUsage = selectedTarget == null
				? utility.baselineUsage()
				: selectedTarget.targetUsage();
			targetCarbon = targetCarbon.add(targetUsage.multiply(utility.carbonFactorG()));
			baselineTotalAmount += utility.baselineAmount() == null ? 0L : utility.baselineAmount();
		}

		baselineCarbon = scale(baselineCarbon);
		targetCarbon = scale(targetCarbon);
		BigDecimal combinedRate = baselineCarbon.signum() == 0
			? ZERO_RATE
			: scale(baselineCarbon.subtract(targetCarbon)
				.divide(baselineCarbon, 9, RoundingMode.HALF_UP)
				.multiply(ONE_HUNDRED));
		TargetTier combinedTier = tierForRate(combinedRate);
		long totalExpectedSaving = targetResults.stream()
			.mapToLong(EcoGoalPreviewResponse.UtilityTarget::expectedSaving)
			.sum();
		List<CalculatedMission> calculatedMissions = calculateMissions(
			request == null ? null : request.selectedMissionIds(),
			utilities
		);
		BigDecimal combinedMissionRate = scale(calculatedMissions.stream()
			.filter(CalculatedMission::counted)
			.map(CalculatedMission::computedRate)
			.filter(java.util.Objects::nonNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add));
		BigDecimal shortfallPoint = scale(combinedRate.subtract(combinedMissionRate).max(BigDecimal.ZERO));

		EcoGoalPreviewResponse response = new EcoGoalPreviewResponse(
			targetResults,
			new EcoGoalPreviewResponse.Combined(
				baselineCarbon,
				targetCarbon,
				combinedRate,
				combinedTier,
				combinedTier == null ? null : combinedTier.label(),
				combinedTier == null ? 0L : combinedTier.mileage(),
				totalExpectedSaving,
				baselineTotalAmount,
				excludedUtilities,
				nextTier(combinedRate)
			),
			new EcoGoalPreviewResponse.MissionSummary(
				combinedMissionRate,
				shortfallPoint,
				combinedMissionRate.compareTo(combinedRate) >= 0,
				calculatedMissions.stream()
					.map(value -> new EcoGoalPreviewResponse.MissionItem(
						value.mission().id(),
						value.computedRate(),
						value.counted(),
						value.exclusionReason()
					))
					.toList()
			),
			carbonFactors(utilities)
		);
		return new GoalCalculation(round, targetTiers, calculatedMissions, response);
	}

	private Map<UtilityType, TargetTier> parseTargets(
		EcoGoalRequest request,
		Map<UtilityType, GoalUtilitySnapshot> utilities
	) {
		if (request == null || request.targets() == null || request.targets().isEmpty()) {
			throw new BusinessException(EcoErrorCode.ECO_TIER_INVALID, "targets", null);
		}

		Map<UtilityType, TargetTier> targets = new LinkedHashMap<>();
		for (EcoGoalRequest.Target target : request.targets()) {
			if (target == null || target.utilityType() == null || target.tier() == null) {
				throw new BusinessException(EcoErrorCode.ECO_TIER_INVALID, "targets", null);
			}
			TargetTier tier;
			try {
				tier = TargetTier.valueOf(target.tier());
			}
			catch (IllegalArgumentException exception) {
				throw new BusinessException(EcoErrorCode.ECO_TIER_INVALID, "targets", null);
			}
			if (targets.putIfAbsent(target.utilityType(), tier) != null) {
				throw new BusinessException(EcoErrorCode.ECO_TIER_INVALID, "targets", null);
			}
			GoalUtilitySnapshot utility = utilities.get(target.utilityType());
			if (utility == null || !utility.registered()) {
				throw new BusinessException(
					EcoErrorCode.ECO_UTILITY_NOT_REGISTERED,
					"targets",
					Map.of("utilityType", target.utilityType().name())
				);
			}
			if (utility.baselineUsage() == null || utility.baselineAmount() == null) {
				throw invalidRequest("targets", Map.of("utilityType", target.utilityType().name()));
			}
		}
		return targets;
	}

	private EcoGoalPreviewResponse.UtilityTarget calculateUtilityTarget(
		GoalUtilitySnapshot utility,
		TargetTier tier
	) {
		BigDecimal rateRatio = tier.targetRate().divide(ONE_HUNDRED, 9, RoundingMode.HALF_UP);
		BigDecimal targetUsage = scale(utility.baselineUsage().multiply(BigDecimal.ONE.subtract(rateRatio)));
		Long expectedSaving = utility.baselineAmount() == null ? 0L : BigDecimal.valueOf(utility.baselineAmount())
			.multiply(rateRatio)
			.setScale(0, RoundingMode.HALF_UP)
			.longValue();
		return new EcoGoalPreviewResponse.UtilityTarget(
			utility.utilityType(),
			tier.targetRate(),
			utility.baselineUsage(),
			targetUsage,
			usageUnit(utility.utilityType()),
			utility.baselineAmount(),
			expectedSaving,
			utility.utilityType() == ELECTRICITY ? 0 : 1
		);
	}

	private List<CalculatedMission> calculateMissions(
		List<Long> selectedMissionIds,
		Map<UtilityType, GoalUtilitySnapshot> utilities
	) {
		if (selectedMissionIds == null || selectedMissionIds.isEmpty()) {
			return List.of();
		}
		Set<Long> distinctIds = new LinkedHashSet<>(selectedMissionIds);
		Map<Long, MissionSnapshot> missionsById = ecoGoalRepository.findActiveMissions().stream()
			.collect(Collectors.toMap(MissionSnapshot::id, Function.identity()));
		List<Long> invalidIds = distinctIds.stream()
			.filter(id -> !missionsById.containsKey(id))
			.toList();
		if (!invalidIds.isEmpty()) {
			throw invalidRequest("selectedMissionIds", Map.of("invalidMissionIds", invalidIds));
		}

		List<CalculatedMission> initial = distinctIds.stream()
			.map(missionsById::get)
			.map(mission -> new CalculatedMission(
				mission,
				computedMissionRate(mission, utilities.get(mission.utilityType())),
				false,
				null
			))
			.toList();
		Map<String, CalculatedMission> largestByGroup = new HashMap<>();
		for (CalculatedMission mission : initial) {
			if (mission.computedRate() == null) {
				continue;
			}
			largestByGroup.merge(
				mission.mission().deviceGroup(),
				mission,
				(left, right) -> left.computedRate().compareTo(right.computedRate()) >= 0 ? left : right
			);
		}

		return initial.stream()
			.map(mission -> {
				CalculatedMission largest = largestByGroup.get(mission.mission().deviceGroup());
				boolean counted = largest != null && largest.mission().id().equals(mission.mission().id());
				String reason = mission.computedRate() != null && !counted
					? mission.mission().deviceGroup() + " 겹침 · 합계 제외"
					: null;
				return new CalculatedMission(mission.mission(), mission.computedRate(), counted, reason);
			})
			.toList();
	}

	private EcoGoalFormResponse.Segment goalSegment(
		UtilityType utilityType,
		GoalUtilitySnapshot utility,
		List<MissionSnapshot> missions,
		Set<Long> selectedMissionIds
	) {
		boolean registered = utility != null && utility.registered();
		BigDecimal baselineUsage = utility == null ? null : utility.baselineUsage();
		BigDecimal monthlyBaselineUsage = baselineUsage == null ? null : monthlyBaselineUsage(baselineUsage);
		List<EcoGoalFormResponse.Mission> items = missions.stream()
			.filter(mission -> mission.utilityType() == utilityType)
			.map(mission -> goalMission(
				mission,
				utility,
				selectedMissionIds.contains(mission.id())
			))
			.toList();
		return new EcoGoalFormResponse.Segment(
			utilityType,
			registered,
			utility == null ? null : utility.unregisteredReason(),
			utility == null ? null : utility.baselineAmount(),
			baselineUsage,
			monthlyBaselineUsage,
			usageUnit(utilityType),
			missionRateCap(utilityType),
			utility == null ? null : utility.targetTier(),
			registered ? null : REGISTER_GUIDE_URL,
			!registered,
			items
		);
	}

	private EcoGoalFormResponse.Mission goalMission(
		MissionSnapshot mission,
		GoalUtilitySnapshot utility,
		boolean selected
	) {
		BigDecimal rawRate = rawMissionRate(mission, utility);
		BigDecimal computedRate = applyMissionCap(mission, rawRate);
		return new EcoGoalFormResponse.Mission(
			mission.id(),
			mission.missionCode(),
			mission.title(),
			mission.description(),
			mission.difficulty(),
			mission.evidenceAmount(),
			mission.evidenceUnit(),
			mission.evidenceText(),
			mission.calculationBasis(),
			mission.sourceOrg(),
			mission.deviceGroup(),
			mission.seasonTags(),
			computedRate,
			isMissionCapped(mission, rawRate),
			selected
		);
	}

	private BigDecimal computedMissionRate(MissionSnapshot mission, GoalUtilitySnapshot utility) {
		return applyMissionCap(mission, rawMissionRate(mission, utility));
	}

	private BigDecimal rawMissionRate(MissionSnapshot mission, GoalUtilitySnapshot utility) {
		if (utility == null || utility.baselineUsage() == null || utility.baselineUsage().signum() == 0) {
			return null;
		}
		return mission.evidenceAmount()
			.divide(monthlyBaselineUsage(utility.baselineUsage()), 9, RoundingMode.HALF_UP)
			.multiply(ONE_HUNDRED);
	}

	private BigDecimal applyMissionCap(MissionSnapshot mission, BigDecimal rawRate) {
		if (rawRate == null) {
			return null;
		}
		BigDecimal cap = missionCap(mission);
		BigDecimal cappedRate = cap == null ? rawRate : rawRate.min(cap);
		return cappedRate.setScale(0, RoundingMode.HALF_UP).setScale(3);
	}

	private boolean isMissionCapped(MissionSnapshot mission, BigDecimal rawRate) {
		BigDecimal cap = missionCap(mission);
		return rawRate != null && cap != null && rawRate.compareTo(cap) > 0;
	}

	private BigDecimal missionCap(MissionSnapshot mission) {
		return mission.rateCap() == null ? missionRateCap(mission.utilityType()) : mission.rateCap();
	}

	private List<EcoGoalPreviewResponse.CarbonFactor> carbonFactors(
		Map<UtilityType, GoalUtilitySnapshot> utilities
	) {
		return List.of(ELECTRICITY, WATER, GAS).stream()
			.map(utilities::get)
			.filter(java.util.Objects::nonNull)
			.map(value -> new EcoGoalPreviewResponse.CarbonFactor(
				value.utilityType(),
				value.carbonFactorG(),
				usageUnit(value.utilityType())
			))
			.toList();
	}

	private EcoGoalPreviewResponse.NextTier nextTier(BigDecimal rate) {
		for (TargetTier tier : TargetTier.values()) {
			if (rate.compareTo(tier.targetRate()) < 0) {
				return new EcoGoalPreviewResponse.NextTier(
					tier,
					scale(tier.targetRate().subtract(rate)),
					tier.mileage()
				);
			}
		}
		return null;
	}

	private TargetTier tierForRate(BigDecimal rate) {
		if (rate == null || rate.compareTo(TargetTier.TIER_5.targetRate()) < 0) {
			return null;
		}
		if (rate.compareTo(TargetTier.TIER_15.targetRate()) >= 0) {
			return TargetTier.TIER_15;
		}
		if (rate.compareTo(TargetTier.TIER_10.targetRate()) >= 0) {
			return TargetTier.TIER_10;
		}
		return TargetTier.TIER_5;
	}

	private GoalRoundSnapshot findRound(Long userId, Long roundId) {
		return ecoGoalRepository.findRound(userId, roundId)
			.orElseThrow(() -> new BusinessException(EcoErrorCode.ECO_ROUND_NOT_FOUND));
	}

	private Map<UtilityType, GoalUtilitySnapshot> utilityMap(Long roundId) {
		return ecoGoalRepository.findUtilities(roundId).stream()
			.collect(Collectors.toMap(
				GoalUtilitySnapshot::utilityType,
				Function.identity(),
				(left, right) -> left,
				() -> new EnumMap<>(UtilityType.class)
			));
	}

	private BigDecimal monthlyBaselineUsage(BigDecimal baselineUsage) {
		return scale(baselineUsage.divide(SIX, 9, RoundingMode.HALF_UP));
	}

	private BigDecimal missionRateCap(UtilityType utilityType) {
		return switch (utilityType) {
			case ELECTRICITY -> new BigDecimal("30.000");
			case WATER -> new BigDecimal("20.000");
			case GAS -> null;
		};
	}

	private UsageUnit usageUnit(UtilityType utilityType) {
		return utilityType == ELECTRICITY ? UsageUnit.kWh : UsageUnit.m3;
	}

	private BigDecimal scale(BigDecimal value) {
		return value.setScale(3, RoundingMode.HALF_UP);
	}

	private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
		return value == null ? null : value.atZone(KOREA_ZONE_ID).toOffsetDateTime();
	}

	private BusinessException invalidRequest(String field, Map<String, Object> details) {
		return new BusinessException(CommonErrorCode.INVALID_REQUEST, field, details);
	}

	private record CalculatedMission(
		MissionSnapshot mission,
		BigDecimal computedRate,
		boolean counted,
		String exclusionReason
	) {
	}

	private record GoalCalculation(
		GoalRoundSnapshot round,
		Map<UtilityType, TargetTier> targetTiers,
		List<CalculatedMission> missions,
		EcoGoalPreviewResponse response
	) {
	}
}
