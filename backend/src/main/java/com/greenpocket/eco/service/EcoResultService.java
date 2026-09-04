package com.greenpocket.eco.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.eco.dto.EcoResultResponse;
import com.greenpocket.eco.dto.EcoSettlementResponse;
import com.greenpocket.eco.entity.RoundStatus;
import com.greenpocket.eco.entity.TargetTier;
import com.greenpocket.eco.exception.EcoErrorCode;
import com.greenpocket.eco.repository.EcoResultRepository;
import com.greenpocket.eco.repository.EcoResultRepository.NextRoundSnapshot;
import com.greenpocket.eco.repository.EcoResultRepository.ResultRoundSnapshot;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.pocket.dto.ConvertibleMileageResponse;
import com.greenpocket.pocket.service.PocketQueryService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EcoResultService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final String CONFIRMED_SOURCE = "에코마일리지 누리집 기준";
	private static final String ECO_EXTERNAL_URL = "https://ecomileage.seoul.go.kr";
	private static final List<String> OTHER_MILEAGE_USES = List.of("서울시 세금", "상품권", "관리비 납부");

	private final EcoResultRepository ecoResultRepository;
	private final PocketQueryService pocketQueryService;

	public EcoResultResponse getResult(Long userId, Long roundId) {
		ResultRoundSnapshot round = findConfirmedRound(userId, roundId);
		TargetTier tier = tierForRate(round.finalRate());
		ConvertibleMileageResponse convertibleMileage = pocketQueryService.getConvertibleMileage(userId);

		return new EcoResultResponse(
			round.id(),
			toYearMonth(round.periodStart()),
			toYearMonth(round.periodEnd()),
			toOffsetDateTime(round.confirmedAt()),
			CONFIRMED_SOURCE,
			round.finalRate(),
			round.targetRate(),
			isAchieved(round.finalRate(), round.targetRate()),
			tier,
			tier == null ? null : tier.label() + " 구간",
			round.confirmedMileage(),
			new EcoResultResponse.Amount(
				round.baselineTotalAmount(),
				round.actualTotalAmount(),
				round.savedAmount(),
				false
			),
			ecoResultRepository.findUtilityResults(roundId).stream()
				.map(utility -> new EcoResultResponse.UtilityResult(
					utility.utilityType(),
					utility.baselineUsage(),
					utility.actualUsage(),
					utility.usageUnit(),
					utility.finalRate(),
					utility.targetRate(),
					Boolean.TRUE.equals(utility.achieved())
				))
				.toList(),
			ecoResultRepository.findMonthlyRates(userId, roundId).stream()
				.map(month -> new EcoResultResponse.MonthlyRate(
					toYearMonth(month.reportMonth()),
					month.monthlyRate(),
					month.achieved()
				))
				.toList(),
			isMileageConverted(round, convertibleMileage),
			ecoResultRepository.findNextRound(userId, round.periodEnd())
				.map(this::toNextRound)
				.orElse(null)
		);
	}

	public EcoSettlementResponse getSettlement(Long userId, Long roundId) {
		ResultRoundSnapshot round = findConfirmedRound(userId, roundId);
		ConvertibleMileageResponse convertibleMileage = pocketQueryService.getConvertibleMileage(userId);

		return new EcoSettlementResponse(
			round.id(),
			toYearMonth(round.periodStart()),
			toYearMonth(round.periodEnd()),
			round.confirmedMileage(),
			"확인",
			round.finalRate(),
			tierForRate(round.finalRate()),
			new EcoSettlementResponse.Calculation(
				round.baselineTotalAmount(),
				round.actualTotalAmount(),
				round.savedAmount(),
				calculationNote(round)
			),
			false,
			isConvertible(round.id(), convertibleMileage),
			ECO_EXTERNAL_URL,
			OTHER_MILEAGE_USES
		);
	}

	private ResultRoundSnapshot findConfirmedRound(Long userId, Long roundId) {
		ResultRoundSnapshot round = ecoResultRepository.findRound(userId, roundId)
			.orElseThrow(() -> new BusinessException(EcoErrorCode.ECO_ROUND_NOT_FOUND));
		if (round.roundStatus() != RoundStatus.CONFIRMED || round.confirmedAt() == null) {
			throw new BusinessException(EcoErrorCode.ECO_RESULT_NOT_CONFIRMED);
		}
		return round;
	}

	private boolean isMileageConverted(
		ResultRoundSnapshot round,
		ConvertibleMileageResponse convertibleMileage
	) {
		if (round.confirmedMileage() == null || round.confirmedMileage() <= 0) {
			return false;
		}
		return convertibleMileage.rounds().stream()
			.noneMatch(value -> value.roundId().equals(round.id()));
	}

	private boolean isConvertible(Long roundId, ConvertibleMileageResponse convertibleMileage) {
		return convertibleMileage.convertible() && convertibleMileage.rounds().stream()
			.anyMatch(value -> value.roundId().equals(roundId));
	}

	private String calculationNote(ResultRoundSnapshot round) {
		return "전기·도시가스·수도를 직전 2년 같은 기간(%d~%d월) 평균과 비교했어요".formatted(
			round.periodStart().getMonthValue(),
			round.periodEnd().getMonthValue()
		);
	}

	private EcoResultResponse.NextRound toNextRound(NextRoundSnapshot round) {
		return new EcoResultResponse.NextRound(
			round.id(),
			toYearMonth(round.periodStart()),
			toYearMonth(round.periodEnd()),
			round.goalSet()
		);
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

	private boolean isAchieved(BigDecimal finalRate, BigDecimal targetRate) {
		return finalRate != null && targetRate != null && finalRate.compareTo(targetRate) >= 0;
	}

	private String toYearMonth(java.time.LocalDate value) {
		return YearMonth.from(value).toString();
	}

	private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
		return value == null ? null : value.atZone(KOREA_ZONE_ID).toOffsetDateTime();
	}
}
