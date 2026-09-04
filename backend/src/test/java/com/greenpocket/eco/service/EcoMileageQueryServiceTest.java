package com.greenpocket.eco.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.greenpocket.eco.repository.EcoMileageQueryRepository;
import com.greenpocket.eco.repository.EcoMileageQueryRepository.ConfirmedMileageRoundSnapshot;

class EcoMileageQueryServiceTest {

	@Test
	void returnsConfirmedMileageRoundsOwnedByUser() {
		EcoMileageQueryRepository repository = mock(EcoMileageQueryRepository.class);
		when(repository.findConfirmedMileageRounds(42L)).thenReturn(List.of(
			new ConfirmedMileageRoundSnapshot(
				7L,
				LocalDate.of(2026, 4, 1),
				LocalDate.of(2026, 9, 1),
				30_000L
			)
		));

		List<ConfirmedMileageRoundSnapshot> result =
			new EcoMileageQueryService(repository).findConfirmedMileageRounds(42L);

		assertThat(result).singleElement().satisfies(round -> {
			assertThat(round.roundId()).isEqualTo(7L);
			assertThat(round.confirmedMileage()).isEqualTo(30_000L);
		});
	}
}
