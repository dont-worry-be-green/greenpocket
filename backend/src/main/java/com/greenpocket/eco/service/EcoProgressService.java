package com.greenpocket.eco.service;

import static com.greenpocket.global.type.UtilityType.ELECTRICITY;
import static com.greenpocket.global.type.UtilityType.GAS;
import static com.greenpocket.global.type.UtilityType.WATER;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.eco.dto.EcoHomeResponse;
import com.greenpocket.eco.dto.EcoMonthlyReportResponse;
import com.greenpocket.eco.entity.ApplicationStatus;
import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.eco.entity.TargetTier;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.eco.entity.WhatIfScreen;
import com.greenpocket.eco.exception.EcoErrorCode;
import com.greenpocket.eco.repository.EcoProgressRepository;
import com.greenpocket.eco.repository.EcoProgressRepository.MissionProgressSnapshot;
import com.greenpocket.eco.repository.EcoProgressRepository.MonthlyUtilitySnapshot;
import com.greenpocket.eco.repository.EcoProgressRepository.ProgressRoundSnapshot;
import com.greenpocket.eco.repository.EcoProgressRepository.ResultRoundSnapshot;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.global.type.UtilityType;

@Service
@Transactional(readOnly = true)
public class EcoProgressService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private static final BigDecimal ZERO_RATE = new BigDecimal("0.000");
	private static final String ECO_EXTERNAL_URL = "https://ecomileage.seoul.go.kr";

	private final EcoProgressRepository ecoProgressRepository;
	private final Clock clock;

	@Autowired
	public EcoProgressService(EcoProgressRepository ecoProgressRepository) {
		this(ecoProgressRepository, Clock.system(KOREA_ZONE_ID));
	}

	EcoProgressService(EcoProgressRepository ecoProgressRepository, Clock clock) {
		this.ecoProgressRepository = ecoProgressRepository;
		this.clock = clock;
	}

	public EcoHomeResponse getHome(Long userId) {
		EcoLinkStatus linkStatus = ecoProgressRepository.findLinkStatus(userId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED_DEMO_KEY));
		if (linkStatus == EcoLinkStatus.UNLINKED || linkStatus == EcoLinkStatus.FAILED) {
			return emptyHome(WhatIfScreen.WF_01_UNLINKED);
		}
		if (linkStatus == EcoLinkStatus.LINKING) {
			return emptyHome(WhatIfScreen.WF_02_LINKING);
		}

		ProgressRoundSnapshot round = ecoProgressRepository.findCurrentRound(userId)
			.orElseThrow(() -> new BusinessException(EcoErrorCode.ECO_ROUND_NOT_FOUND));
		boolean goalSet = round.goalSetAt() != null;
		Optional<ResultRoundSnapshot> unviewedResult = ecoProgressRepository.findUnviewedResult(userId);
		WhatIfScreen screen = unviewedResult.isPresent()
			? WhatIfScreen.WF_09_RESULT_READY
			: goalSet ? WhatIfScreen.WF_06_IN_PROGRESS : WhatIfScreen.WF_03_NO_GOAL;

		YearMonth periodStart = YearMonth.from(round.periodStart());
		YearMonth periodEnd = YearMonth.from(round.periodEnd());
		List<YearMonth> homeRemainingMonths = homeRemainingMonths(periodStart, periodEnd);
		EcoHomeResponse.Header header = new EcoHomeResponse.Header(
			periodStart.toString(),
			periodEnd.toString(),
			homeRemainingMonths.size(),
			monthLabels(homeRemainingMonths)
		);

		List<MonthCalculation> calculations = calculationsUntilLatestBill(userId, round);
		BigDecimal cumulativeRate = cumulativeRate(calculations);
		List<String> coveredMonths = calculations.stream()
			.map(value -> value.month().toString())
			.toList();
		TargetTier currentTier = tierForRate(cumulativeRate);
		TargetTier targetTier = tierForRate(round.combinedTargetRate());
		NextTier nextTier = nextTier(cumulativeRate);

		EcoHomeResponse.Progress progress = goalSet ? new EcoHomeResponse.Progress(
			cumulativeRate,
			coveredMonths,
			currentTier,
			targetTier,
			tierProgress(currentTier, targetTier),
			nextTier == null ? null : nextTier.gapPoint(),
			nextTier == null ? null : nextTier.tier().mileage()
		) : null;
		EcoHomeResponse.LatestReport latestReport = latestReport(calculations, round.combinedTargetRate());
		LocalDate today = LocalDate.now(clock);
		MissionProgressSnapshot missionProgress = goalSet
			? ecoProgressRepository.findMissionProgress(userId, round.id(), today, season(today))
			: new MissionProgressSnapshot(0, 0);

		return new EcoHomeResponse(
			screen,
			round.id(),
			header,
			progress,
			latestReport,
			new EcoHomeResponse.Application(
				round.applicationStatus(),
				round.applicationStatus() != ApplicationStatus.APPLIED,
				ECO_EXTERNAL_URL
			),
			new EcoHomeResponse.Goal(
				goalSet,
				goalSet ? round.combinedTargetRate() : null,
				goalSet ? targetTier : null,
				goalSet ? round.expectedMileage() : null
			),
			new EcoHomeResponse.TodayMissions(
				missionProgress.completedCount(),
				missionProgress.totalCount()
			),
			unviewedResult.map(this::resultModal).orElse(null),
			links()
		);
	}

	public EcoMonthlyReportResponse getMonthlyReport(Long userId, String monthValue) {
		ecoProgressRepository.findLinkStatus(userId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED_DEMO_KEY));
		ProgressRoundSnapshot currentRound = ecoProgressRepository.findCurrentRound(userId)
			.orElseThrow(() -> new BusinessException(EcoErrorCode.ECO_ROUND_NOT_FOUND));
		YearMonth requestedMonth = parseMonth(monthValue);
		ProgressRoundSnapshot round = requestedMonth == null
			? currentRound
			: ecoProgressRepository.findRoundForMonth(userId, requestedMonth.atDay(1))
				.orElseThrow(() -> new BusinessException(EcoErrorCode.ECO_ROUND_NOT_FOUND));

		Optional<LocalDate> reportDate = requestedMonth == null
			? ecoProgressRepository.findLatestBillMonth(userId, round.periodStart(), round.periodEnd())
			: Optional.of(requestedMonth.atDay(1));
		if (reportDate.isEmpty()) {
			return emptyMonthlyReport(null, round.id());
		}

		YearMonth reportMonth = YearMonth.from(reportDate.get());
		List<MonthlyUtilitySnapshot> rows = ecoProgressRepository.findMonthlyUtilities(
			userId,
			round.id(),
			round.periodStart(),
			reportDate.get()
		);
		List<MonthCalculation> calculations = monthCalculations(rows);
		MonthCalculation report = calculations.stream()
			.filter(value -> value.month().equals(reportMonth))
			.findFirst()
			.orElse(null);
		if (report == null) {
			return emptyMonthlyReport(reportMonth.toString(), round.id());
		}

		BigDecimal targetRate = rateOrZero(round.combinedTargetRate());
		BigDecimal cumulativeRate = cumulativeRate(calculations);
		List<String> cumulativeMonths = calculations.stream()
			.map(value -> value.month().toString())
			.toList();
		List<EcoMonthlyReportResponse.UtilityResult> byUtility = utilityResults(report, targetRate);
		UtilityType largestCarbonUtility = byUtility.stream()
			.max(Comparator.comparing(EcoMonthlyReportResponse.UtilityResult::carbonSharePercent))
			.map(EcoMonthlyReportResponse.UtilityResult::utilityType)
			.orElse(null);
		int remainingMonths = remainingMonths(reportMonth, YearMonth.from(round.periodEnd()));
		BigDecimal requiredRate = requiredRate(
			targetRate,
			calculations,
			remainingMonths,
			periodLength(round)
		);
		BigDecimal selectedMissionRate = scale(ecoProgressRepository.findSelectedMissionRate(userId, round.id()));

		return new EcoMonthlyReportResponse(
			reportMonth.toString(),
			round.id(),
			toOffsetDateTime(report.billRegisteredAt()),
			baselineDescription(reportMonth),
			new EcoMonthlyReportResponse.Result(
				report.rate(),
				targetRate,
				report.rate().compareTo(targetRate) >= 0,
				cumulativeRate,
				cumulativeMonths
			),
			new EcoMonthlyReportResponse.Cause(
				byUtility,
				largestCarbonUtility,
				carbonFactors(report.rows())
			),
			new EcoMonthlyReportResponse.Prescription(
				remainingMonths,
				monthLabels(monthsAfter(reportMonth, YearMonth.from(round.periodEnd()))),
				requiredRate,
				isAchievable(requiredRate, selectedMissionRate),
				requiredByUtility(report, largestCarbonUtility, requiredRate),
				selectedMissionRate,
				largestCarbonUtility
			),
			calculations.stream()
				.map(value -> new EcoMonthlyReportResponse.MonthlyRate(
					value.month().toString(),
					value.rate(),
					value.rate().compareTo(targetRate) >= 0
				))
				.toList(),
			null
		);
	}

	private List<MonthCalculation> calculationsUntilLatestBill(Long userId, ProgressRoundSnapshot round) {
		Optional<LocalDate> latestBillMonth = ecoProgressRepository.findLatestBillMonth(
			userId,
			round.periodStart(),
			round.periodEnd()
		);
		if (latestBillMonth.isEmpty()) {
			return List.of();
		}
		return monthCalculations(ecoProgressRepository.findMonthlyUtilities(
			userId,
			round.id(),
			round.periodStart(),
			latestBillMonth.get()
		));
	}

	private List<MonthCalculation> monthCalculations(List<MonthlyUtilitySnapshot> rows) {
		Map<YearMonth, List<MonthlyUtilitySnapshot>> byMonth = new LinkedHashMap<>();
		for (MonthlyUtilitySnapshot row : rows) {
			byMonth.computeIfAbsent(YearMonth.from(row.billingMonth()), ignored -> new ArrayList<>()).add(row);
		}
		return byMonth.entrySet().stream()
			.map(entry -> calculateMonth(entry.getKey(), entry.getValue()))
			.filter(value -> value.baselineCarbon().signum() > 0)
			.toList();
	}

	private MonthCalculation calculateMonth(YearMonth month, List<MonthlyUtilitySnapshot> rows) {
		BigDecimal baselineCarbon = BigDecimal.ZERO;
		BigDecimal actualCarbon = BigDecimal.ZERO;
		for (MonthlyUtilitySnapshot row : rows) {
			baselineCarbon = baselineCarbon.add(row.baselineUsage().multiply(row.carbonFactorG()));
			actualCarbon = actualCarbon.add(row.actualUsage().multiply(row.carbonFactorG()));
		}
		BigDecimal rate = baselineCarbon.signum() == 0
			? ZERO_RATE
			: percentage(baselineCarbon.subtract(actualCarbon), baselineCarbon);
		java.time.LocalDateTime latestRegistration = rows.stream()
			.map(MonthlyUtilitySnapshot::billRegisteredAt)
			.filter(java.util.Objects::nonNull)
			.max(Comparator.naturalOrder())
			.orElse(null);
		return new MonthCalculation(
			month,
			scale(baselineCarbon),
			scale(actualCarbon),
			rate,
			latestRegistration,
			List.copyOf(rows)
		);
	}

	private BigDecimal cumulativeRate(List<MonthCalculation> calculations) {
		BigDecimal baseline = calculations.stream()
			.map(MonthCalculation::baselineCarbon)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		if (baseline.signum() == 0) {
			return ZERO_RATE;
		}
		BigDecimal actual = calculations.stream()
			.map(MonthCalculation::actualCarbon)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return percentage(baseline.subtract(actual), baseline);
	}

	private List<EcoMonthlyReportResponse.UtilityResult> utilityResults(
		MonthCalculation report,
		BigDecimal combinedTargetRate
	) {
		BigDecimal totalBaselineCarbon = report.baselineCarbon();
		return report.rows().stream()
			.map(row -> {
				BigDecimal rate = percentage(row.baselineUsage().subtract(row.actualUsage()), row.baselineUsage());
				BigDecimal utilityTarget = row.targetRate() == null ? combinedTargetRate : row.targetRate();
				boolean achieved = rate.compareTo(utilityTarget) >= 0;
				BigDecimal share = totalBaselineCarbon.signum() == 0
					? BigDecimal.ZERO.setScale(1)
					: row.baselineUsage().multiply(row.carbonFactorG())
						.divide(totalBaselineCarbon, 9, RoundingMode.HALF_UP)
						.multiply(ONE_HUNDRED)
						.setScale(1, RoundingMode.HALF_UP);
				return new EcoMonthlyReportResponse.UtilityResult(
					row.utilityType(),
					row.baselineUsage(),
					row.actualUsage(),
					row.usageUnit(),
					rate,
					achieved,
					share,
					!achieved
				);
			})
			.toList();
	}

	private List<EcoMonthlyReportResponse.CarbonFactor> carbonFactors(List<MonthlyUtilitySnapshot> rows) {
		Map<UtilityType, MonthlyUtilitySnapshot> byUtility = new EnumMap<>(UtilityType.class);
		rows.forEach(row -> byUtility.put(row.utilityType(), row));
		return List.of(ELECTRICITY, WATER, GAS).stream()
			.map(byUtility::get)
			.filter(java.util.Objects::nonNull)
			.map(row -> new EcoMonthlyReportResponse.CarbonFactor(
				row.utilityType(),
				row.carbonFactorG(),
				row.usageUnit()
			))
			.toList();
	}

	private List<EcoMonthlyReportResponse.RequiredUtility> requiredByUtility(
		MonthCalculation report,
		UtilityType largestUtility,
		BigDecimal requiredRate
	) {
		if (largestUtility == null || requiredRate == null || report.baselineCarbon().signum() == 0) {
			return List.of();
		}
		MonthlyUtilitySnapshot largest = report.rows().stream()
			.filter(row -> row.utilityType() == largestUtility)
			.findFirst()
			.orElse(null);
		if (largest == null) {
			return List.of();
		}

		BigDecimal largestCarbon = largest.baselineUsage().multiply(largest.carbonFactorG());
		BigDecimal otherContribution = BigDecimal.ZERO;
		List<String> assumptions = new ArrayList<>();
		for (MonthlyUtilitySnapshot row : report.rows()) {
			if (row.utilityType() == largestUtility || row.baselineUsage().signum() == 0) {
				continue;
			}
			BigDecimal rate = percentage(row.baselineUsage().subtract(row.actualUsage()), row.baselineUsage());
			BigDecimal carbon = row.baselineUsage().multiply(row.carbonFactorG());
			otherContribution = otherContribution.add(carbon.multiply(rate).divide(ONE_HUNDRED));
			assumptions.add("%s %s%%".formatted(utilityLabel(row.utilityType()), displayRate(rate)));
		}
		if (largestCarbon.signum() == 0) {
			return List.of();
		}
		BigDecimal requiredLargestCarbonSaving = report.baselineCarbon()
			.multiply(requiredRate)
			.divide(ONE_HUNDRED)
			.subtract(otherContribution);
		BigDecimal utilityRequiredRate = percentage(requiredLargestCarbonSaving, largestCarbon);
		String assumption = assumptions.isEmpty()
			? "다른 요금의 감축률을 지금처럼 유지할 때"
			: String.join(", ", assumptions) + " 감축을 지금처럼 유지할 때";
		return List.of(new EcoMonthlyReportResponse.RequiredUtility(
			largestUtility,
			utilityRequiredRate,
			assumption
		));
	}

	private BigDecimal requiredRate(
		BigDecimal targetRate,
		List<MonthCalculation> calculations,
		int remainingMonths,
		int periodLength
	) {
		if (remainingMonths == 0) {
			return null;
		}
		BigDecimal rateSum = calculations.stream()
			.map(MonthCalculation::rate)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return scale(targetRate.multiply(BigDecimal.valueOf(periodLength))
			.subtract(rateSum)
			.divide(BigDecimal.valueOf(remainingMonths), 9, RoundingMode.HALF_UP));
	}

	private EcoHomeResponse.LatestReport latestReport(
		List<MonthCalculation> calculations,
		BigDecimal targetRateValue
	) {
		if (calculations.isEmpty()) {
			return new EcoHomeResponse.LatestReport(false, null, null, null, null, null);
		}
		MonthCalculation latest = calculations.getLast();
		BigDecimal targetRate = rateOrZero(targetRateValue);
		return new EcoHomeResponse.LatestReport(
			true,
			latest.month().toString(),
			toOffsetDateTime(latest.billRegisteredAt()),
			latest.rate(),
			targetRate,
			latest.rate().compareTo(targetRate) >= 0
		);
	}

	private EcoHomeResponse.ResultModal resultModal(ResultRoundSnapshot result) {
		return new EcoHomeResponse.ResultModal(
			result.id(),
			YearMonth.from(result.periodStart()).toString(),
			YearMonth.from(result.periodEnd()).toString(),
			result.finalRate(),
			tierForRate(result.finalRate()),
			result.confirmedMileage(),
			toOffsetDateTime(result.confirmedAt())
		);
	}

	private List<EcoHomeResponse.TierProgress> tierProgress(TargetTier current, TargetTier target) {
		return List.of(TargetTier.values()).stream()
			.map(tier -> new EcoHomeResponse.TierProgress(
				tier,
				tier.mileage(),
				tier == current ? "CURRENT" : tier == target ? "TARGET" : "NONE"
			))
			.toList();
	}

	private NextTier nextTier(BigDecimal rate) {
		for (TargetTier tier : TargetTier.values()) {
			if (rate.compareTo(tier.targetRate()) < 0) {
				return new NextTier(tier, scale(tier.targetRate().subtract(rate)));
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

	private YearMonth parseMonth(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		if (!YEAR_MONTH_PATTERN.matcher(value).matches()) {
			throw invalidMonth();
		}
		try {
			return YearMonth.parse(value);
		}
		catch (java.time.DateTimeException exception) {
			throw invalidMonth();
		}
	}

	private BusinessException invalidMonth() {
		return new BusinessException(
			CommonErrorCode.INVALID_REQUEST,
			"month",
			Map.of("expectedFormat", "YYYY-MM")
		);
	}

	private List<YearMonth> homeRemainingMonths(YearMonth periodStart, YearMonth periodEnd) {
		YearMonth current = YearMonth.now(clock);
		YearMonth first = current.isBefore(periodStart) ? periodStart : current;
		return monthsBetween(first, periodEnd);
	}

	private List<YearMonth> monthsAfter(YearMonth month, YearMonth periodEnd) {
		return monthsBetween(month.plusMonths(1), periodEnd);
	}

	private List<YearMonth> monthsBetween(YearMonth first, YearMonth last) {
		if (first.isAfter(last)) {
			return List.of();
		}
		long count = ChronoUnit.MONTHS.between(first, last) + 1;
		return java.util.stream.LongStream.range(0, count)
			.mapToObj(first::plusMonths)
			.toList();
	}

	private int remainingMonths(YearMonth reportMonth, YearMonth periodEnd) {
		return monthsAfter(reportMonth, periodEnd).size();
	}

	private int periodLength(ProgressRoundSnapshot round) {
		return (int)ChronoUnit.MONTHS.between(
			YearMonth.from(round.periodStart()),
			YearMonth.from(round.periodEnd())
		) + 1;
	}

	private List<Integer> monthLabels(List<YearMonth> months) {
		return months.stream().map(YearMonth::getMonthValue).toList();
	}

	private String baselineDescription(YearMonth reportMonth) {
		return "%d·%d년 %d월 평균".formatted(
			reportMonth.getYear() - 2,
			reportMonth.getYear() - 1,
			reportMonth.getMonthValue()
		);
	}

	private String utilityLabel(UtilityType utilityType) {
		return switch (utilityType) {
			case ELECTRICITY -> "전기";
			case GAS -> "도시가스";
			case WATER -> "수도";
		};
	}

	private String season(LocalDate date) {
		return switch (date.getMonthValue()) {
			case 3, 4, 5 -> "SPRING";
			case 6, 7, 8 -> "SUMMER";
			case 9, 10, 11 -> "AUTUMN";
			default -> "WINTER";
		};
	}

	private String displayRate(BigDecimal rate) {
		return rate.setScale(0, RoundingMode.HALF_UP).toPlainString();
	}

	private boolean isAchievable(BigDecimal requiredRate, BigDecimal selectedMissionRate) {
		return requiredRate == null
			|| requiredRate.signum() <= 0
			|| selectedMissionRate.compareTo(requiredRate) >= 0;
	}

	private BigDecimal percentage(BigDecimal difference, BigDecimal baseline) {
		if (baseline == null || baseline.signum() == 0) {
			return ZERO_RATE;
		}
		return scale(difference.divide(baseline, 9, RoundingMode.HALF_UP).multiply(ONE_HUNDRED));
	}

	private BigDecimal rateOrZero(BigDecimal value) {
		return value == null ? ZERO_RATE : scale(value);
	}

	private BigDecimal scale(BigDecimal value) {
		return value == null ? null : value.setScale(3, RoundingMode.HALF_UP);
	}

	private OffsetDateTime toOffsetDateTime(java.time.LocalDateTime value) {
		return value == null ? null : value.atZone(KOREA_ZONE_ID).toOffsetDateTime();
	}

	private EcoHomeResponse emptyHome(WhatIfScreen screen) {
		return new EcoHomeResponse(screen, null, null, null, null, null, null, null, null, links());
	}

	private EcoMonthlyReportResponse emptyMonthlyReport(String month, Long roundId) {
		return new EcoMonthlyReportResponse(
			month,
			roundId,
			null,
			month == null ? null : baselineDescription(YearMonth.parse(month)),
			null,
			null,
			null,
			List.of(),
			"NO_BILL"
		);
	}

	private EcoHomeResponse.Links links() {
		return new EcoHomeResponse.Links(true, true, true);
	}

	private record MonthCalculation(
		YearMonth month,
		BigDecimal baselineCarbon,
		BigDecimal actualCarbon,
		BigDecimal rate,
		java.time.LocalDateTime billRegisteredAt,
		List<MonthlyUtilitySnapshot> rows
	) {
	}

	private record NextTier(TargetTier tier, BigDecimal gapPoint) {
	}

}
