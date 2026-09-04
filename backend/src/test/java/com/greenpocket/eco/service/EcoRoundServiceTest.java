package com.greenpocket.eco.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.eco.dto.EcoCurrentRoundResponse;
import com.greenpocket.eco.entity.ApplicationStatus;
import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.eco.entity.RoundStatus;
import com.greenpocket.eco.exception.EcoErrorCode;
import com.greenpocket.eco.repository.EcoRepository;
import com.greenpocket.eco.repository.EcoRepository.EcoRoundSnapshot;
import com.greenpocket.eco.repository.EcoRepository.EcoUserSnapshot;
import com.greenpocket.eco.repository.EcoRepository.EcoUtilitySnapshot;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.type.UtilityType;

class EcoRoundServiceTest {

	private static final Long USER_ID = 1L;

	private EcoRepository ecoRepository;
	private EcoRoundService ecoRoundService;

	@BeforeEach
	void setUp() {
		ecoRepository = mock(EcoRepository.class);
		ecoRoundService = new EcoRoundService(ecoRepository);
	}

	@Test
	void returnsCurrentRoundBaselineFromLinkedMockData() {
		when(ecoRepository.findUser(USER_ID)).thenReturn(Optional.of(user(EcoLinkStatus.LINKED)));
		when(ecoRepository.findCurrentRound(USER_ID)).thenReturn(Optional.of(round()));
		when(ecoRepository.findUtilities(7L)).thenReturn(utilities());

		EcoCurrentRoundResponse response = ecoRoundService.getCurrentRound(USER_ID);

		assertThat(response.roundId()).isEqualTo(7L);
		assertThat(response.periodStart()).isEqualTo("2026-04");
		assertThat(response.periodEnd()).isEqualTo("2026-09");
		assertThat(response.baselineDescription()).isEqualTo("2024·2025년 4~9월 평균");
		assertThat(response.goalSet()).isFalse();
		assertThat(response.nextScreen()).isEqualTo("WF-03");
		assertThat(response.baseline().totalAmount()).isEqualTo(420_600L);
		assertThat(response.baseline().totalCarbonG()).isEqualByComparingTo("831992.000");
		assertThat(response.baseline().largestShareUtility()).isEqualTo(UtilityType.ELECTRICITY);
		assertThat(response.baseline().items()).hasSize(3);
		assertThat(response.baseline().items().getFirst().amount()).isEqualTo(268_000L);
	}

	@Test
	void requiresEcoLinkBeforeRoundLookup() {
		when(ecoRepository.findUser(USER_ID)).thenReturn(Optional.of(user(EcoLinkStatus.UNLINKED)));

		assertThatThrownBy(() -> ecoRoundService.getCurrentRound(USER_ID))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> assertThat(((BusinessException)error).getErrorCode())
				.isEqualTo(EcoErrorCode.ECO_NOT_LINKED));
	}

	@Test
	void reportsMissingRoundForLinkedUser() {
		when(ecoRepository.findUser(USER_ID)).thenReturn(Optional.of(user(EcoLinkStatus.LINKED)));
		when(ecoRepository.findCurrentRound(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> ecoRoundService.getCurrentRound(USER_ID))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> assertThat(((BusinessException)error).getErrorCode())
				.isEqualTo(EcoErrorCode.ECO_ROUND_NOT_FOUND));
	}

	@Test
	void fillsMissingUtilityAndHidesSharesWhenTotalAmountIsZero() {
		EcoRoundSnapshot zeroAmountRound = new EcoRoundSnapshot(
			7L,
			LocalDate.of(2026, 4, 1),
			LocalDate.of(2026, 9, 1),
			RoundStatus.READY,
			ApplicationStatus.NOT_APPLIED,
			0L,
			BigDecimal.ZERO,
			LocalDateTime.of(2026, 9, 1, 9, 0),
			null
		);
		when(ecoRepository.findUser(USER_ID)).thenReturn(Optional.of(user(EcoLinkStatus.LINKED)));
		when(ecoRepository.findCurrentRound(USER_ID)).thenReturn(Optional.of(zeroAmountRound));
		when(ecoRepository.findUtilities(7L)).thenReturn(List.of(
			utility(UtilityType.ELECTRICITY, "424.000", 0L, "0.000", "100.000")
		));

		EcoCurrentRoundResponse response = ecoRoundService.getCurrentRound(USER_ID);

		assertThat(response.baseline().items())
			.extracting(EcoCurrentRoundResponse.BaselineItem::utilityType)
			.containsExactly(UtilityType.ELECTRICITY, UtilityType.GAS, UtilityType.WATER);
		assertThat(response.baseline().items())
			.extracting(EcoCurrentRoundResponse.BaselineItem::shareRate)
			.containsOnlyNulls();
		assertThat(response.baseline().items().get(1).registered()).isFalse();
		assertThat(response.baseline().largestShareUtility()).isNull();
	}

	private EcoUserSnapshot user(EcoLinkStatus status) {
		return new EcoUserSnapshot(
			"11", "서울특별시", "11620", "관악구", status, null,
			"11", "11620", "서울특별시 관악구", LocalDate.of(2026, 3, 1)
		);
	}

	private EcoRoundSnapshot round() {
		return new EcoRoundSnapshot(
			7L,
			LocalDate.of(2026, 4, 1),
			LocalDate.of(2026, 9, 1),
			RoundStatus.READY,
			ApplicationStatus.NOT_APPLIED,
			420_600L,
			new BigDecimal("831992.000"),
			LocalDateTime.of(2026, 9, 1, 9, 0),
			null
		);
	}

	private List<EcoUtilitySnapshot> utilities() {
		return List.of(
			utility(UtilityType.ELECTRICITY, "424.000", 268_000L, "1340.000", "64.000"),
			utility(UtilityType.GAS, "2240.000", 96_600L, "108.000", "23.000"),
			utility(UtilityType.WATER, "332.000", 56_000L, "66.000", "13.000")
		);
	}

	private EcoUtilitySnapshot utility(
		UtilityType type,
		String factor,
		Long amount,
		String usage,
		String shareRate
	) {
		return new EcoUtilitySnapshot(
			type,
			true,
			null,
			new BigDecimal(factor),
			amount,
			new BigDecimal(usage),
			new BigDecimal(shareRate)
		);
	}
}
