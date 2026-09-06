package com.greenpocket.eco.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.greenpocket.eco.entity.ApplicationStatus;
import com.greenpocket.eco.entity.RoundStatus;
import com.greenpocket.eco.repository.EcoRepository;
import com.greenpocket.eco.repository.EcoRepository.EcoRoundSnapshot;

class EcoCurrentRoundQueryServiceTest {

	@Test
	void returnsOnlyRoundThatAlreadyHasActiveGoal() {
		EcoRepository repository = mock(EcoRepository.class);
		EcoCurrentRoundQueryService service = new EcoCurrentRoundQueryService(repository);
		when(repository.findCurrentRound(1L)).thenReturn(Optional.of(round(RoundStatus.IN_PROGRESS)));

		assertThat(service.findGoalActiveRoundId(1L)).contains(7L);

		when(repository.findCurrentRound(1L)).thenReturn(Optional.of(round(RoundStatus.READY)));
		assertThat(service.findGoalActiveRoundId(1L)).isEmpty();
	}

	private EcoRoundSnapshot round(RoundStatus status) {
		return new EcoRoundSnapshot(
			7L,
			LocalDate.of(2026, 4, 1),
			LocalDate.of(2026, 9, 30),
			status,
			ApplicationStatus.NOT_APPLIED,
			null,
			null,
			null,
			null
		);
	}
}
