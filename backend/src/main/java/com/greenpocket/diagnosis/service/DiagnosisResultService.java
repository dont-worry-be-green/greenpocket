package com.greenpocket.diagnosis.service;

import static com.greenpocket.diagnosis.exception.DiagnosisErrorCode.DIAGNOSIS_MONTH_EMPTY;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.bill.service.BillDiagnosisQueryService;
import com.greenpocket.bill.service.BillDiagnosisQueryService.MonthlyRecord;
import com.greenpocket.diagnosis.dto.DiagnosisMonthsResponse;
import com.greenpocket.diagnosis.dto.DiagnosisResponse;
import com.greenpocket.diagnosis.service.SingleHouseholdBaselineCatalog.Baseline;
import com.greenpocket.eco.service.EcoCurrentRoundQueryService;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.type.UtilityType;
import com.greenpocket.user.service.UserRegionQueryService;
import com.greenpocket.user.service.UserRegionQueryService.UserDiagnosisProfile;

@Service
@Transactional(readOnly = true)
public class DiagnosisResultService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private static final List<UtilityType> UTILITY_ORDER = List.of(
		UtilityType.ELECTRICITY,
		UtilityType.GAS,
		UtilityType.WATER
	);

	private final BillDiagnosisQueryService billDiagnosisQueryService;
	private final UserRegionQueryService userRegionQueryService;
	private final EcoCurrentRoundQueryService ecoCurrentRoundQueryService;
	private final SingleHouseholdBaselineCatalog baselineCatalog;
	private final Clock clock;

	@Autowired
	public DiagnosisResultService(
		BillDiagnosisQueryService billDiagnosisQueryService,
		UserRegionQueryService userRegionQueryService,
		EcoCurrentRoundQueryService ecoCurrentRoundQueryService,
		SingleHouseholdBaselineCatalog baselineCatalog
	) {
		this(
			billDiagnosisQueryService,
			userRegionQueryService,
			ecoCurrentRoundQueryService,
			baselineCatalog,
			Clock.system(KOREA_ZONE_ID)
		);
	}

	DiagnosisResultService(
		BillDiagnosisQueryService billDiagnosisQueryService,
		UserRegionQueryService userRegionQueryService,
		EcoCurrentRoundQueryService ecoCurrentRoundQueryService,
		SingleHouseholdBaselineCatalog baselineCatalog,
		Clock clock
	) {
		this.billDiagnosisQueryService = billDiagnosisQueryService;
		this.userRegionQueryService = userRegionQueryService;
		this.ecoCurrentRoundQueryService = ecoCurrentRoundQueryService;
		this.baselineCatalog = baselineCatalog;
		this.clock = clock;
	}

	public DiagnosisMonthsResponse findMonths(Long userId) {
		List<MonthlyRecord> allBills = billDiagnosisQueryService.findAllBills(userId);
		List<DiagnosisMonthsResponse.MonthItem> months = groupByMonth(allBills).entrySet().stream()
			.map(entry -> new DiagnosisMonthsResponse.MonthItem(
				entry.getKey().toString(),
				true,
				orderedUtilities(entry.getValue()),
				entry.getValue().stream().mapToLong(MonthlyRecord::amount).sum()
			))
			.toList();
		String defaultMonth = months.isEmpty() ? null : months.getFirst().yearMonth();
		return new DiagnosisMonthsResponse(months, defaultMonth);
	}

	public DiagnosisResponse findDiagnosis(Long userId, YearMonth requestedMonth) {
		List<MonthlyRecord> allBills = billDiagnosisQueryService.findAllBills(userId);
		Map<YearMonth, List<MonthlyRecord>> billsByMonth = groupByMonth(allBills);

		if (billsByMonth.isEmpty()) {
			if (requestedMonth != null) {
				throw new BusinessException(DIAGNOSIS_MONTH_EMPTY);
			}
			return DiagnosisResponse.empty(findLatestUnregisteredMonth(billsByMonth).toString());
		}

		YearMonth targetMonth = requestedMonth == null
			? billsByMonth.keySet().iterator().next()
			: requestedMonth;
		List<MonthlyRecord> currentRecords = billsByMonth.get(targetMonth);
		if (currentRecords == null) {
			throw new BusinessException(DIAGNOSIS_MONTH_EMPTY);
		}

		List<MonthlyRecord> previousRecords = billDiagnosisQueryService
			.findPreviousYearBaseline(userId, targetMonth.minusYears(1));
		Optional<UserDiagnosisProfile> profile = userRegionQueryService.findDiagnosisProfile(userId);

		return new DiagnosisResponse(
			false,
			null,
			"AN-07",
			targetMonth.toString(),
			profile.map(UserDiagnosisProfile::profileSummary).orElse(""),
			createSummary(currentRecords, previousRecords),
			createLastYearComparison(currentRecords, previousRecords),
			createSingleHouseholdComparison(targetMonth, billsByMonth),
			createWhatIfLink(userId)
		);
	}

	private DiagnosisResponse.Summary createSummary(
		List<MonthlyRecord> currentRecords,
		List<MonthlyRecord> previousRecords
	) {
		long currentTotal = currentRecords.stream().mapToLong(MonthlyRecord::amount).sum();
		boolean hasPreviousYear = !previousRecords.isEmpty();
		Long previousTotal = hasPreviousYear
			? previousRecords.stream().mapToLong(MonthlyRecord::amount).sum()
			: null;
		List<DiagnosisResponse.SummaryItem> items = currentRecords.stream()
			.sorted((left, right) -> Integer.compare(
				UTILITY_ORDER.indexOf(left.utilityType()),
				UTILITY_ORDER.indexOf(right.utilityType())
			))
			.map(record -> new DiagnosisResponse.SummaryItem(
				record.utilityType(),
				record.amount(),
				record.usage(),
				record.usageUnit()
			))
			.toList();
		return new DiagnosisResponse.Summary(
			currentTotal,
			previousTotal,
			hasPreviousYear ? currentTotal - previousTotal : null,
			hasPreviousYear,
			items
		);
	}

	private DiagnosisResponse.LastYearComparison createLastYearComparison(
		List<MonthlyRecord> currentRecords,
		List<MonthlyRecord> previousRecords
	) {
		if (previousRecords.isEmpty()) {
			return new DiagnosisResponse.LastYearComparison(false, "NO_BASELINE", null, List.of());
		}

		Map<UtilityType, MonthlyRecord> currentByUtility = byUtility(currentRecords);
		Map<UtilityType, MonthlyRecord> previousByUtility = byUtility(previousRecords);
		long totalDiff = currentRecords.stream().mapToLong(MonthlyRecord::amount).sum()
			- previousRecords.stream().mapToLong(MonthlyRecord::amount).sum();
		List<DiagnosisResponse.LastYearItem> items = UTILITY_ORDER.stream()
			.filter(utility -> currentByUtility.containsKey(utility) || previousByUtility.containsKey(utility))
			.map(utility -> {
				Long current = amount(currentByUtility.get(utility));
				Long previous = amount(previousByUtility.get(utility));
				Long diff = current == null || previous == null ? null : current - previous;
				return new DiagnosisResponse.LastYearItem(utility, previous, current, diff);
			})
			.toList();
		return new DiagnosisResponse.LastYearComparison(true, null, totalDiff, items);
	}

	private DiagnosisResponse.SingleHouseholdComparison createSingleHouseholdComparison(
		YearMonth targetMonth,
		Map<YearMonth, List<MonthlyRecord>> billsByMonth
	) {
		List<MonthlyRecord> currentRecords = billsByMonth.getOrDefault(targetMonth, List.of());
		Map<UtilityType, MonthlyRecord> currentByUtility = byUtility(currentRecords);
		List<DiagnosisResponse.SingleHouseholdTab> tabs = UTILITY_ORDER.stream()
			.map(utilityType -> createSingleHouseholdTab(
				targetMonth,
				currentByUtility.get(utilityType),
				utilityType,
				billsByMonth
			))
			.toList();
		return new DiagnosisResponse.SingleHouseholdComparison(
			"1인 가구 평균 사용량",
			tabs
		);
	}

	private DiagnosisResponse.SingleHouseholdTab createSingleHouseholdTab(
		YearMonth targetMonth,
		MonthlyRecord currentRecord,
		UtilityType utilityType,
		Map<YearMonth, List<MonthlyRecord>> billsByMonth
	) {
		Optional<Baseline> baseline = baselineCatalog.find(targetMonth, utilityType);
		BigDecimal myUsage = currentRecord == null ? null : currentRecord.usage();
		if (baseline.isEmpty()) {
			return new DiagnosisResponse.SingleHouseholdTab(
				utilityType,
				false,
				"NO_BASELINE",
				myUsage,
				null,
				null,
				null,
				currentRecord == null ? null : currentRecord.usageUnit(),
				null,
				null,
				null,
				null,
				null,
				List.of()
			);
		}
		Baseline value = baseline.get();
		BigDecimal difference = myUsage == null ? null : myUsage.subtract(value.averageUsage());
		BigDecimal differenceRate = difference == null || value.averageUsage().signum() == 0
			? null
			: difference.multiply(ONE_HUNDRED)
				.divide(value.averageUsage(), 3, RoundingMode.HALF_UP);
		return new DiagnosisResponse.SingleHouseholdTab(
			utilityType,
			true,
			null,
			myUsage,
			value.averageUsage(),
			difference,
			differenceRate,
			value.usageUnit(),
			value.comparisonLabel(),
			value.sourceName(),
			value.referencePeriod(),
			value.calculationBasis(),
			value.note(),
			createSingleHouseholdSeries(targetMonth, utilityType, billsByMonth)
		);
	}

	private List<DiagnosisResponse.SingleHouseholdSeriesPoint> createSingleHouseholdSeries(
		YearMonth targetMonth,
		UtilityType utilityType,
		Map<YearMonth, List<MonthlyRecord>> billsByMonth
	) {
		return IntStream.rangeClosed(0, 5)
			.mapToObj(offset -> targetMonth.minusMonths(5L - offset))
			.map(yearMonth -> baselineCatalog.find(yearMonth, utilityType)
				.map(baseline -> new DiagnosisResponse.SingleHouseholdSeriesPoint(
					yearMonth.toString(),
					findUsage(billsByMonth.getOrDefault(yearMonth, List.of()), utilityType),
					baseline.averageUsage()
				))
				.orElse(null))
			.filter(java.util.Objects::nonNull)
			.toList();
	}

	private static BigDecimal findUsage(List<MonthlyRecord> records, UtilityType utilityType) {
		return records.stream()
			.filter(record -> record.utilityType() == utilityType)
			.map(MonthlyRecord::usage)
			.findFirst()
			.orElse(null);
	}

	private DiagnosisResponse.WhatIfLink createWhatIfLink(Long userId) {
		return ecoCurrentRoundQueryService.findCurrentRoundLink(userId)
			.map(round -> new DiagnosisResponse.WhatIfLink(round.roundId(), round.goalSet()))
			.orElseGet(() -> new DiagnosisResponse.WhatIfLink(null, false));
	}

	private YearMonth findLatestUnregisteredMonth(Map<YearMonth, List<MonthlyRecord>> registered) {
		YearMonth candidate = YearMonth.now(clock).minusMonths(1);
		while (registered.containsKey(candidate)) {
			candidate = candidate.minusMonths(1);
		}
		return candidate;
	}

	private static Map<YearMonth, List<MonthlyRecord>> groupByMonth(List<MonthlyRecord> records) {
		Map<YearMonth, List<MonthlyRecord>> result = new LinkedHashMap<>();
		records.stream()
			.sorted((left, right) -> right.yearMonth().compareTo(left.yearMonth()))
			.forEach(record -> result.computeIfAbsent(record.yearMonth(), ignored -> new ArrayList<>()).add(record));
		return result;
	}

	private static List<UtilityType> orderedUtilities(List<MonthlyRecord> records) {
		Map<UtilityType, Boolean> included = new EnumMap<>(UtilityType.class);
		records.forEach(record -> included.put(record.utilityType(), true));
		return UTILITY_ORDER.stream().filter(included::containsKey).toList();
	}

	private static Map<UtilityType, MonthlyRecord> byUtility(List<MonthlyRecord> records) {
		Map<UtilityType, MonthlyRecord> result = new EnumMap<>(UtilityType.class);
		records.forEach(record -> result.put(record.utilityType(), record));
		return result;
	}

	private static Long amount(MonthlyRecord record) {
		return record == null ? null : record.amount();
	}

}
