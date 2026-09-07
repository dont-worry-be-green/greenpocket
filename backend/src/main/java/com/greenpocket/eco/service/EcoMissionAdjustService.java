package com.greenpocket.eco.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.eco.dto.EcoGoalFormResponse;
import com.greenpocket.eco.dto.EcoGoalResponse;
import com.greenpocket.eco.dto.EcoMissionAdjustResponse;
import com.greenpocket.eco.dto.EcoMonthlyReportResponse;
import com.greenpocket.eco.entity.TargetTier;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.global.type.UtilityType;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EcoMissionAdjustService {

	private static final BigDecimal ZERO_RATE = new BigDecimal("0.000");
	private static final BigDecimal RECOVERY_BURDEN_MULTIPLIER = new BigDecimal("1.500");

	private final EcoGoalService ecoGoalService;
	private final EcoProgressService ecoProgressService;

	public EcoMissionAdjustResponse getMissionAdjust(
		Long userId,
		Long roundId,
		String utilityValue,
		String month
	) {
		UtilityType utilityType = parseUtility(utilityValue);
		EcoGoalFormResponse goalForm = ecoGoalService.getGoalForm(userId, roundId);
		EcoGoalResponse goal = ecoGoalService.getGoal(userId, roundId);
		EcoMonthlyReportResponse report = ecoProgressService.getMonthlyReport(userId, month);
		if (!roundId.equals(report.roundId())) {
			throw new BusinessException(
				CommonErrorCode.INVALID_REQUEST,
				"month",
				Map.of("roundId", roundId, "reportRoundId", report.roundId())
			);
		}

		EcoGoalFormResponse.Segment segment = goalForm.segments().stream()
			.filter(value -> value.utilityType() == utilityType)
			.findFirst()
			.orElseThrow(() -> invalidUtility(utilityValue));
		ReportValues reportValues = reportValues(report, utilityType);
		BigDecimal currentRate = currentMissionRate(goal, utilityType);
		RecommendationPlan recommendationPlan = recommendationPlan(
			segment,
			currentRate,
			reportValues.requiredRate()
		);
		Set<Long> recommendedIds = recommendationPlan.missionIds();
		BigDecimal withRecommendedRate = recommendationPlan.previewRate();
		int consecutiveMisses = consecutiveMisses(report.monthlyRates());

		return new EcoMissionAdjustResponse(
			roundId,
			utilityType,
			report.reportMonth(),
			reportValues.requiredRate(),
			reportValues.assumption(),
			reportValues.carbonSharePercent(),
			new EcoMissionAdjustResponse.Comparison(currentRate, reportValues.actualRate()),
			(int)segment.missions().stream().filter(EcoGoalFormResponse.Mission::selected).count(),
			segment.missions().stream()
				.map(mission -> new EcoMissionAdjustResponse.Mission(
					mission.missionId(),
					mission.title(),
					mission.computedRate(),
					mission.difficulty(),
					mission.deviceGroup(),
					mission.evidenceText(),
					mission.calculationBasis(),
					mission.sourceOrg(),
					mission.selected(),
					recommendedIds.contains(mission.missionId()),
					mission.capped()
				))
				.toList(),
			new EcoMissionAdjustResponse.Preview(
				currentRate,
				withRecommendedRate,
				reportValues.requiredRate() != null
					&& withRecommendedRate.compareTo(reportValues.requiredRate()) >= 0
			),
			tierDowngrade(
				consecutiveMisses,
				segment.selectedTier(),
				reportValues.requiredRate(),
				withRecommendedRate
			)
		);
	}

	private ReportValues reportValues(EcoMonthlyReportResponse report, UtilityType utilityType) {
		if (report.cause() == null || report.prescription() == null) {
			return new ReportValues(null, null, null, null);
		}
		EcoMonthlyReportResponse.UtilityResult utilityResult = report.cause().byUtility().stream()
			.filter(value -> value.utilityType() == utilityType)
			.findFirst()
			.orElse(null);
		EcoMonthlyReportResponse.RequiredUtility requiredUtility = report.prescription().requiredByUtility().stream()
			.filter(value -> value.utilityType() == utilityType)
			.findFirst()
			.orElse(null);
		return new ReportValues(
			requiredUtility == null ? null : requiredUtility.requiredRate(),
			requiredUtility == null ? null : requiredUtility.assumption(),
			utilityResult == null ? null : utilityResult.carbonSharePercent(),
			utilityResult == null ? null : utilityResult.rate()
		);
	}

	private BigDecimal currentMissionRate(EcoGoalResponse goal, UtilityType utilityType) {
		if (goal.missions() == null) {
			return ZERO_RATE;
		}
		return scale(goal.missions().stream()
			.filter(mission -> mission.utilityType() == utilityType && mission.counted())
			.map(EcoGoalResponse.SavedMission::computedRate)
			.filter(java.util.Objects::nonNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add));
	}

	private RecommendationPlan recommendationPlan(
		EcoGoalFormResponse.Segment segment,
		BigDecimal currentRate,
		BigDecimal requiredRate
	) {
		Set<Long> recommendedIds = new LinkedHashSet<>();
		if (requiredRate == null || currentRate.compareTo(requiredRate) >= 0) {
			return new RecommendationPlan(recommendedIds, currentRate);
		}

		BigDecimal previewRate = currentRate;
		Map<String, BigDecimal> selectedGroupRates = selectedGroupRates(segment.missions());

		for (Map.Entry<String, BigDecimal> selectedGroup : selectedGroupRates.entrySet()) {
			BigDecimal remainingRate = requiredRate.subtract(previewRate);
			EcoGoalFormResponse.Mission replacement = chooseCandidate(
				segment.missions().stream()
					.filter(mission -> !mission.selected())
					.filter(mission -> selectedGroup.getKey().equals(mission.deviceGroup()))
					.filter(this::hasPositiveRate)
					.filter(mission -> mission.computedRate().compareTo(selectedGroup.getValue()) > 0)
					.toList(),
				remainingRate.add(selectedGroup.getValue())
			);
			if (replacement == null) {
				continue;
			}
			recommendedIds.add(replacement.missionId());
			previewRate = previewRate.add(replacement.computedRate().subtract(selectedGroup.getValue()));
			selectedGroup.setValue(replacement.computedRate());
			if (previewRate.compareTo(requiredRate) >= 0) {
				return new RecommendationPlan(recommendedIds, scale(previewRate));
			}
		}

		Map<String, List<EcoGoalFormResponse.Mission>> unusedGroups = new LinkedHashMap<>();
		for (EcoGoalFormResponse.Mission mission : segment.missions()) {
			if (mission.selected() || !hasPositiveRate(mission) || mission.deviceGroup() == null
				|| selectedGroupRates.containsKey(mission.deviceGroup())) {
				continue;
			}
			unusedGroups.computeIfAbsent(mission.deviceGroup(), ignored -> new ArrayList<>()).add(mission);
		}
		for (List<EcoGoalFormResponse.Mission> candidates : unusedGroups.values()) {
			EcoGoalFormResponse.Mission addition = chooseCandidate(
				candidates,
				requiredRate.subtract(previewRate)
			);
			recommendedIds.add(addition.missionId());
			previewRate = previewRate.add(addition.computedRate());
			if (previewRate.compareTo(requiredRate) >= 0) {
				break;
			}
		}
		return new RecommendationPlan(recommendedIds, scale(previewRate));
	}

	private Map<String, BigDecimal> selectedGroupRates(List<EcoGoalFormResponse.Mission> missions) {
		Map<String, BigDecimal> rates = new LinkedHashMap<>();
		for (EcoGoalFormResponse.Mission mission : missions) {
			if (!mission.selected() || !hasPositiveRate(mission) || mission.deviceGroup() == null) {
				continue;
			}
			rates.merge(mission.deviceGroup(), mission.computedRate(), BigDecimal::max);
		}
		return rates;
	}

	private EcoGoalFormResponse.Mission chooseCandidate(
		List<EcoGoalFormResponse.Mission> candidates,
		BigDecimal targetRate
	) {
		if (candidates.isEmpty()) {
			return null;
		}
		return candidates.stream()
			.filter(mission -> mission.computedRate().compareTo(targetRate) >= 0)
			.min(Comparator.comparing(EcoGoalFormResponse.Mission::computedRate))
			.orElseGet(() -> candidates.stream()
				.max(Comparator.comparing(EcoGoalFormResponse.Mission::computedRate))
				.orElseThrow());
	}

	private boolean hasPositiveRate(EcoGoalFormResponse.Mission mission) {
		return mission.computedRate() != null && mission.computedRate().signum() > 0;
	}

	private int consecutiveMisses(List<EcoMonthlyReportResponse.MonthlyRate> monthlyRates) {
		int misses = 0;
		for (int index = monthlyRates.size() - 1; index >= 0; index--) {
			if (monthlyRates.get(index).achieved()) {
				break;
			}
			misses++;
		}
		return misses;
	}

	private EcoMissionAdjustResponse.TierDowngrade tierDowngrade(
		int consecutiveMisses,
		TargetTier selectedTier,
		BigDecimal requiredRate,
		BigDecimal withRecommendedRate
	) {
		if (selectedTier == null || requiredRate == null) {
			return new EcoMissionAdjustResponse.TierDowngrade(
				false,
				consecutiveMisses,
				"목표 조정에 필요한 월별 데이터가 아직 부족해요"
			);
		}

		TargetTier suggestedTier = lowerTier(selectedTier);
		BigDecimal burdenThreshold = selectedTier.targetRate().multiply(RECOVERY_BURDEN_MULTIPLIER);
		boolean recoveryBurdenHigh = requiredRate.compareTo(burdenThreshold) >= 0;
		boolean missionPlanInsufficient = withRecommendedRate.compareTo(requiredRate) < 0;
		boolean suggest = suggestedTier != null && (recoveryBurdenHigh || missionPlanInsufficient);
		String message;
		if (suggest && missionPlanInsufficient) {
			message = "추천 가능한 실천을 모두 반영해도 필요한 %s%%에 미치지 못해요. %s 구간으로 조정을 검토해 보세요"
				.formatted(rateText(requiredRate), suggestedTier.label());
		}
		else if (suggest) {
			message = "남은 기간에는 매달 %s%% 감축이 필요해 처음 목표보다 실천 부담이 커졌어요. %s 구간으로 조정을 검토해 보세요"
				.formatted(rateText(requiredRate), suggestedTier.label());
		}
		else if (suggestedTier == null && (recoveryBurdenHigh || missionPlanInsufficient)) {
			message = "현재는 가장 낮은 %s 구간이에요. 목표 하향 대신 실천 강도를 조정해 보세요"
				.formatted(selectedTier.label());
		}
		else if (consecutiveMisses > 0) {
			message = "%d개월 연속 목표에 못 미쳤지만 필요한 월 감축률 %s%%는 회복 가능한 범위예요. 현재 %s 구간을 유지해 보세요"
				.formatted(consecutiveMisses, rateText(requiredRate), selectedTier.label());
		}
		else {
			message = "필요한 월 감축률 %s%%는 현재 목표에서 회복 가능한 범위예요"
				.formatted(rateText(requiredRate));
		}
		return new EcoMissionAdjustResponse.TierDowngrade(suggest, consecutiveMisses, message);
	}

	private TargetTier lowerTier(TargetTier selectedTier) {
		return switch (selectedTier) {
			case TIER_15 -> TargetTier.TIER_10;
			case TIER_10 -> TargetTier.TIER_5;
			case TIER_5 -> null;
		};
	}

	private String rateText(BigDecimal rate) {
		return scale(rate).stripTrailingZeros().toPlainString();
	}

	private UtilityType parseUtility(String value) {
		if (value == null || value.isBlank()) {
			throw invalidUtility(value);
		}
		try {
			return UtilityType.valueOf(value.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException exception) {
			throw invalidUtility(value);
		}
	}

	private BusinessException invalidUtility(String value) {
		return new BusinessException(
			CommonErrorCode.INVALID_REQUEST,
			"utility",
			Map.of(
				"value", value == null ? "" : value,
				"allowedValues", List.of(UtilityType.values()).stream().map(Enum::name).toList()
			)
		);
	}

	private BigDecimal scale(BigDecimal value) {
		return value.setScale(3, RoundingMode.HALF_UP);
	}

	private record ReportValues(
		BigDecimal requiredRate,
		String assumption,
		BigDecimal carbonSharePercent,
		BigDecimal actualRate
	) {
	}

	private record RecommendationPlan(Set<Long> missionIds, BigDecimal previewRate) {
	}
}
