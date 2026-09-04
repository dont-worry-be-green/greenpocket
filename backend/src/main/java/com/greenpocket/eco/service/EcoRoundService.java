package com.greenpocket.eco.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.eco.dto.EcoCurrentRoundResponse;
import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.eco.exception.EcoErrorCode;
import com.greenpocket.eco.repository.EcoRepository;
import com.greenpocket.eco.repository.EcoRepository.EcoRoundSnapshot;
import com.greenpocket.eco.repository.EcoRepository.EcoUserSnapshot;
import com.greenpocket.eco.repository.EcoRepository.EcoUtilitySnapshot;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.global.type.UtilityType;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EcoRoundService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

	private final EcoRepository ecoRepository;

	public EcoCurrentRoundResponse getCurrentRound(Long userId) {
		EcoUserSnapshot user = ecoRepository.findUser(userId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED_DEMO_KEY));
		if (user.linkStatus() != EcoLinkStatus.LINKED) {
			throw new BusinessException(EcoErrorCode.ECO_NOT_LINKED);
		}

		EcoRoundSnapshot round = ecoRepository.findCurrentRound(userId)
			.orElseThrow(() -> new BusinessException(EcoErrorCode.ECO_ROUND_NOT_FOUND));
		List<EcoUtilitySnapshot> utilities = ecoRepository.findUtilities(round.id());
		boolean hideShares = round.baselineTotalAmount() != null && round.baselineTotalAmount() == 0L;
		UtilityType largestShareUtility = hideShares ? null : utilities.stream()
			.filter(EcoUtilitySnapshot::registered)
			.filter(value -> value.baselineShareRate() != null)
			.max(Comparator.comparing(EcoUtilitySnapshot::baselineShareRate))
			.map(EcoUtilitySnapshot::utilityType)
			.orElse(null);

		List<EcoCurrentRoundResponse.BaselineItem> items = List.of(UtilityType.values()).stream()
			.map(utilityType -> utilities.stream()
				.filter(value -> value.utilityType() == utilityType)
				.findFirst()
				.map(value -> toBaselineItem(value, hideShares))
				.orElseGet(() -> emptyBaselineItem(utilityType)))
			.toList();
		boolean goalSet = round.goalSetAt() != null;

		return new EcoCurrentRoundResponse(
			round.id(),
			YearMonth.from(round.periodStart()).toString(),
			YearMonth.from(round.periodEnd()).toString(),
			remainingMonths(round.periodEnd()),
			round.roundStatus(),
			round.applicationStatus(),
			goalSet,
			toOffsetDateTime(round.baselineQueriedAt()),
			baselineDescription(round.periodStart(), round.periodEnd()),
			new EcoCurrentRoundResponse.Baseline(
				round.baselineTotalAmount(),
				round.baselineTotalCarbonG(),
				items,
				largestShareUtility
			),
			goalSet ? "WF-06" : "WF-03"
		);
	}

	private EcoCurrentRoundResponse.BaselineItem toBaselineItem(EcoUtilitySnapshot utility, boolean hideShares) {
		return new EcoCurrentRoundResponse.BaselineItem(
			utility.utilityType(),
			utility.registered(),
			utility.baselineAmount(),
			utility.baselineUsage(),
			usageUnit(utility.utilityType()),
			utility.carbonFactorG(),
			hideShares ? null : utility.baselineShareRate()
		);
	}

	private EcoCurrentRoundResponse.BaselineItem emptyBaselineItem(UtilityType utilityType) {
		return new EcoCurrentRoundResponse.BaselineItem(
			utilityType,
			false,
			null,
			null,
			usageUnit(utilityType),
			null,
			null
		);
	}

	private UsageUnit usageUnit(UtilityType utilityType) {
		return utilityType == UtilityType.ELECTRICITY ? UsageUnit.kWh : UsageUnit.m3;
	}

	private int remainingMonths(LocalDate periodEnd) {
		YearMonth currentMonth = YearMonth.now(KOREA_ZONE_ID);
		YearMonth endMonth = YearMonth.from(periodEnd);
		long months = ChronoUnit.MONTHS.between(currentMonth, endMonth) + 1;
		return (int)Math.max(months, 0);
	}

	private String baselineDescription(LocalDate periodStart, LocalDate periodEnd) {
		int firstBaselineYear = periodStart.getYear() - 2;
		int secondBaselineYear = periodStart.getYear() - 1;
		return "%d·%d년 %d~%d월 평균".formatted(
			firstBaselineYear,
			secondBaselineYear,
			periodStart.getMonthValue(),
			periodEnd.getMonthValue()
		);
	}

	private OffsetDateTime toOffsetDateTime(java.time.LocalDateTime value) {
		return value == null ? null : value.atZone(KOREA_ZONE_ID).toOffsetDateTime();
	}
}
