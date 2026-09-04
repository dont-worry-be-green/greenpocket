package com.greenpocket.eco.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
		Set<Long> recommendedIds = recommendedMissionIds(segment, currentRate, reportValues.requiredRate());
		BigDecimal recommendedRate = segment.missions().stream()
			.filter(mission -> recommendedIds.contains(mission.missionId()))
			.map(EcoGoalFormResponse.Mission::computedRate)
			.filter(java.util.Objects::nonNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal withRecommendedRate = scale(currentRate.add(recommendedRate));
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
			tierDowngrade(consecutiveMisses, segment.selectedTier())
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

	private Set<Long> recommendedMissionIds(
		EcoGoalFormResponse.Segment segment,
		BigDecimal currentRate,
		BigDecimal requiredRate
	) {
		Set<Long> recommendedIds = new LinkedHashSet<>();
		if (requiredRate == null || currentRate.compareTo(requiredRate) >= 0) {
			return recommendedIds;
		}
		Set<String> usedDeviceGroups = new LinkedHashSet<>();
		segment.missions().stream()
			.filter(EcoGoalFormResponse.Mission::selected)
			.map(EcoGoalFormResponse.Mission::deviceGroup)
			.filter(java.util.Objects::nonNull)
			.forEach(usedDeviceGroups::add);

		BigDecimal previewRate = currentRate;
		for (EcoGoalFormResponse.Mission mission : segment.missions()) {
			if (mission.selected() || mission.computedRate() == null || mission.computedRate().signum() <= 0) {
				continue;
			}
			String deviceGroup = mission.deviceGroup();
			if (deviceGroup != null && usedDeviceGroups.contains(deviceGroup)) {
				continue;
			}
			recommendedIds.add(mission.missionId());
			if (deviceGroup != null) {
				usedDeviceGroups.add(deviceGroup);
			}
			previewRate = previewRate.add(mission.computedRate());
			if (previewRate.compareTo(requiredRate) >= 0) {
				break;
			}
		}
		return recommendedIds;
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
		TargetTier selectedTier
	) {
		boolean suggest = consecutiveMisses >= 2;
		String message;
		if (suggest) {
			message = "%d개월 연속 목표에 못 미쳤어요. 목표 구간 조정을 검토해 보세요".formatted(consecutiveMisses);
		}
		else if (consecutiveMisses == 1 && selectedTier != null) {
			message = "한 달 미끄러진 것만으로 %s 구간을 포기하기엔 일러요".formatted(selectedTier.label());
		}
		else {
			message = "현재 목표 구간을 유지해도 좋아요";
		}
		return new EcoMissionAdjustResponse.TierDowngrade(suggest, consecutiveMisses, message);
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
}
