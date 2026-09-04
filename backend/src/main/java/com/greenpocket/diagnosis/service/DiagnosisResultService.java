package com.greenpocket.diagnosis.service;

import static com.greenpocket.diagnosis.exception.DiagnosisErrorCode.DIAGNOSIS_MONTH_EMPTY;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.bill.service.BillDiagnosisQueryService;
import com.greenpocket.bill.service.BillDiagnosisQueryService.MonthlyRecord;
import com.greenpocket.diagnosis.dto.DiagnosisMonthsResponse;
import com.greenpocket.diagnosis.dto.DiagnosisResponse;
import com.greenpocket.diagnosis.entity.RegionLevel;
import com.greenpocket.diagnosis.entity.RegionUtilitySnapshot;
import com.greenpocket.diagnosis.repository.RegionUtilitySnapshotRepository;
import com.greenpocket.eco.service.EcoCurrentRoundQueryService;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.type.UtilityType;
import com.greenpocket.user.service.UserRegionQueryService;
import com.greenpocket.user.service.UserRegionQueryService.UserDiagnosisProfile;

@Service
@Transactional(readOnly = true)
public class DiagnosisResultService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final String SIDO_SIGUNGU_CODE = "";
	private static final List<UtilityType> UTILITY_ORDER = List.of(
		UtilityType.ELECTRICITY,
		UtilityType.GAS,
		UtilityType.WATER
	);

	private final BillDiagnosisQueryService billDiagnosisQueryService;
	private final UserRegionQueryService userRegionQueryService;
	private final EcoCurrentRoundQueryService ecoCurrentRoundQueryService;
	private final RegionUtilitySnapshotRepository regionUtilitySnapshotRepository;
	private final Clock clock;

	@Autowired
	public DiagnosisResultService(
		BillDiagnosisQueryService billDiagnosisQueryService,
		UserRegionQueryService userRegionQueryService,
		EcoCurrentRoundQueryService ecoCurrentRoundQueryService,
		RegionUtilitySnapshotRepository regionUtilitySnapshotRepository
	) {
		this(
			billDiagnosisQueryService,
			userRegionQueryService,
			ecoCurrentRoundQueryService,
			regionUtilitySnapshotRepository,
			Clock.system(KOREA_ZONE_ID)
		);
	}

	DiagnosisResultService(
		BillDiagnosisQueryService billDiagnosisQueryService,
		UserRegionQueryService userRegionQueryService,
		EcoCurrentRoundQueryService ecoCurrentRoundQueryService,
		RegionUtilitySnapshotRepository regionUtilitySnapshotRepository,
		Clock clock
	) {
		this.billDiagnosisQueryService = billDiagnosisQueryService;
		this.userRegionQueryService = userRegionQueryService;
		this.ecoCurrentRoundQueryService = ecoCurrentRoundQueryService;
		this.regionUtilitySnapshotRepository = regionUtilitySnapshotRepository;
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
			createRegionComparison(userId, targetMonth, currentRecords, profile),
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

	private DiagnosisResponse.RegionComparison createRegionComparison(
		Long userId,
		YearMonth targetMonth,
		List<MonthlyRecord> currentRecords,
		Optional<UserDiagnosisProfile> profile
	) {
		Map<UtilityType, MonthlyRecord> currentByUtility = byUtility(currentRecords);
		Optional<RegionUtilitySnapshot> targetBaseline = profile.flatMap(value ->
			findLatestAvailableBaseline(value, UtilityType.ELECTRICITY, targetMonth));

		List<DiagnosisResponse.RegionTab> tabs = new ArrayList<>();
		tabs.add(createElectricityTab(userId, targetMonth, currentByUtility, profile, targetBaseline));
		tabs.add(unavailableRegionTab(UtilityType.GAS, currentByUtility));
		tabs.add(unavailableRegionTab(UtilityType.WATER, currentByUtility));

		return new DiagnosisResponse.RegionComparison(
			targetBaseline.map(RegionUtilitySnapshot::getRegionLevel).orElse(null),
			targetBaseline.map(snapshot -> regionLabel(snapshot, profile.orElse(null))).orElse(null),
			targetBaseline.map(snapshot -> snapshot.getRegionLevel() == RegionLevel.SIDO).orElse(false),
			targetBaseline.map(RegionUtilitySnapshot::getSourceName).orElse(null),
			targetBaseline.map(snapshot -> YearMonth.from(snapshot.getBaseMonth()).toString()).orElse(null),
			targetBaseline.map(snapshot -> snapshot.getExtractedAt().atZone(KOREA_ZONE_ID).toOffsetDateTime())
				.orElse(null),
			tabs
		);
	}

	private DiagnosisResponse.RegionTab createElectricityTab(
		Long userId,
		YearMonth targetMonth,
		Map<UtilityType, MonthlyRecord> currentByUtility,
		Optional<UserDiagnosisProfile> profile,
		Optional<RegionUtilitySnapshot> targetBaseline
	) {
		Long myAmount = amount(currentByUtility.get(UtilityType.ELECTRICITY));
		if (targetBaseline.isEmpty()) {
			return new DiagnosisResponse.RegionTab(
				UtilityType.ELECTRICITY, false, "NO_BASELINE", myAmount, null, null, null
			);
		}

		long regionAverage = targetBaseline.get().getAvgAmount();
		YearMonth firstMonth = targetMonth.minusMonths(5);
		Map<YearMonth, Long> mineByMonth = billDiagnosisQueryService
			.findBills(userId, firstMonth, targetMonth)
			.stream()
			.filter(record -> record.utilityType() == UtilityType.ELECTRICITY)
			.collect(java.util.stream.Collectors.toMap(MonthlyRecord::yearMonth, MonthlyRecord::amount));
		List<DiagnosisResponse.SeriesPoint> series = new ArrayList<>();
		for (int index = 0; index < 6; index++) {
			YearMonth month = firstMonth.plusMonths(index);
			Long average = profile.flatMap(value -> findLatestAvailableAtLevel(
				value,
				UtilityType.ELECTRICITY,
				month,
				targetBaseline.get().getRegionLevel()
			))
				.map(RegionUtilitySnapshot::getAvgAmount)
				.orElse(null);
			series.add(new DiagnosisResponse.SeriesPoint(month.toString(), mineByMonth.get(month), average));
		}
		return new DiagnosisResponse.RegionTab(
			UtilityType.ELECTRICITY,
			true,
			null,
			myAmount,
			regionAverage,
			myAmount == null ? null : myAmount - regionAverage,
			series
		);
	}

	private Optional<RegionUtilitySnapshot> findLatestAvailableAtLevel(
		UserDiagnosisProfile profile,
		UtilityType utilityType,
		YearMonth month,
		RegionLevel level
	) {
		String sigunguCode = level == RegionLevel.SIDO ? SIDO_SIGUNGU_CODE : profile.sigunguCode();
		return findLatestAvailable(
			level,
			profile.sidoCode(),
			sigunguCode,
			utilityType,
			month.atDay(1)
		);
	}

	private DiagnosisResponse.RegionTab unavailableRegionTab(
		UtilityType utilityType,
		Map<UtilityType, MonthlyRecord> currentByUtility
	) {
		return new DiagnosisResponse.RegionTab(
			utilityType,
			false,
			"REGION_DATA_NOT_PUBLISHED",
			amount(currentByUtility.get(utilityType)),
			null,
			null,
			null
		);
	}

	private Optional<RegionUtilitySnapshot> findLatestAvailableBaseline(
		UserDiagnosisProfile profile,
		UtilityType utilityType,
		YearMonth month
	) {
		if (profile.sidoCode() == null || profile.sigunguCode() == null) {
			return Optional.empty();
		}
		LocalDate latestMonth = month.atDay(1);
		Optional<RegionUtilitySnapshot> sigungu = findLatestAvailable(
			RegionLevel.SIGUNGU,
			profile.sidoCode(),
			profile.sigunguCode(),
			utilityType,
			latestMonth
		);
		return sigungu.isPresent() ? sigungu : findLatestAvailable(
			RegionLevel.SIDO,
			profile.sidoCode(),
			SIDO_SIGUNGU_CODE,
			utilityType,
			latestMonth
		);
	}

	private Optional<RegionUtilitySnapshot> findLatestAvailable(
		RegionLevel level,
		String sidoCode,
		String sigunguCode,
		UtilityType utilityType,
		LocalDate latestMonth
	) {
		return regionUtilitySnapshotRepository
			.findFirstByRegionLevelAndSidoCodeAndSigunguCodeAndUtilityTypeAndBaseMonthLessThanEqualAndAvgUsageIsNotNullAndAvgAmountIsNotNullOrderByBaseMonthDesc(
				level,
				sidoCode,
				sigunguCode,
				utilityType,
				latestMonth
			);
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

	private static String regionLabel(
		RegionUtilitySnapshot snapshot,
		UserDiagnosisProfile profile
	) {
		if (profile == null) {
			return null;
		}
		return snapshot.getRegionLevel() == RegionLevel.SIDO
			? profile.sidoLabel()
			: profile.regionLabel();
	}
}
