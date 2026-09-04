package com.greenpocket.eco.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.eco.dto.EcoLinkProgressResponse;
import com.greenpocket.eco.dto.EcoLinkStartResponse;
import com.greenpocket.eco.dto.EcoStatusResponse;
import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.eco.entity.JobStatus;
import com.greenpocket.eco.exception.EcoErrorCode;
import com.greenpocket.eco.repository.EcoRepository;
import com.greenpocket.eco.repository.EcoRepository.EcoUserSnapshot;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.type.UtilityType;

class EcoLinkServiceTest {

	private static final Long USER_ID = 1L;

	private EcoRepository ecoRepository;
	private EcoLinkService ecoLinkService;

	@BeforeEach
	void setUp() {
		ecoRepository = mock(EcoRepository.class);
		ecoLinkService = new EcoLinkService(ecoRepository);
	}

	@Test
	void returnsUnlinkedSeoulStatusWithThreeFixedUtilities() {
		when(ecoRepository.findUser(USER_ID)).thenReturn(Optional.of(seoulUser(EcoLinkStatus.UNLINKED)));
		when(ecoRepository.findCurrentRound(USER_ID)).thenReturn(Optional.empty());

		EcoStatusResponse response = ecoLinkService.getStatus(USER_ID);

		assertThat(response.linkStatus()).isEqualTo(EcoLinkStatus.UNLINKED);
		assertThat(response.seoulResident()).isTrue();
		assertThat(response.linkable()).isTrue();
		assertThat(response.eligibleForRound()).isFalse();
		assertThat(response.registeredUtilities())
			.extracting(EcoStatusResponse.RegisteredUtility::utilityType)
			.containsExactly(UtilityType.ELECTRICITY, UtilityType.GAS, UtilityType.WATER);
		assertThat(response.registeredUtilities()).allMatch(value -> !value.registered());
	}

	@Test
	void rejectsLinkForNonSeoulResident() {
		when(ecoRepository.findUser(USER_ID)).thenReturn(Optional.of(nonSeoulUser()));

		assertThatThrownBy(() -> ecoLinkService.startLink(USER_ID))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> assertThat(((BusinessException)error).getErrorCode())
				.isEqualTo(EcoErrorCode.ECO_NOT_SEOUL));
	}

	@Test
	void completesMockLinkOnSecondPollAndPersistsBaseline() {
		when(ecoRepository.findUser(USER_ID)).thenReturn(Optional.of(seoulUser(EcoLinkStatus.UNLINKED)));
		when(ecoRepository.upsertMockRound(anyLong(), any(), any(), anyLong(), any(), any()))
			.thenReturn(7L);

		EcoLinkStartResponse started = ecoLinkService.startLink(USER_ID);
		EcoLinkProgressResponse running = ecoLinkService.getLinkProgress(USER_ID, started.linkJobId());
		EcoLinkProgressResponse completed = ecoLinkService.getLinkProgress(USER_ID, started.linkJobId());

		assertThat(started.status()).isEqualTo(JobStatus.RUNNING);
		assertThat(started.estimatedSeconds()).isEqualTo(20);
		assertThat(running.status()).isEqualTo(JobStatus.RUNNING);
		assertThat(completed.status()).isEqualTo(JobStatus.SUCCEEDED);
		assertThat(completed.roundId()).isEqualTo(7L);
		assertThat(completed.registeredUtilities())
			.containsExactly(UtilityType.ELECTRICITY, UtilityType.GAS, UtilityType.WATER);
		assertThat(completed.baselineMonthsLoaded()).isEqualTo(24);
		assertThat(completed.nextScreen()).isEqualTo("WF-03");

		verify(ecoRepository).markLinking(USER_ID);
		verify(ecoRepository, times(3)).upsertMockUtility(anyLong(), any(), any(), anyLong(), any(), any());
		verify(ecoRepository, times(72))
			.upsertMockBaselineRecord(anyLong(), any(), any(), anyLong(), any(), any());
		verify(ecoRepository).markLinked(
			anyLong(), any(), any(), any(), any(), any(LocalDate.class)
		);
	}

	@Test
	void rejectsUnknownLinkJob() {
		assertThatThrownBy(() -> ecoLinkService.getLinkProgress(USER_ID, "eco_unknown"))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> assertThat(((BusinessException)error).getErrorCode())
				.isEqualTo(EcoErrorCode.ECO_LINK_FAILED));
	}

	private EcoUserSnapshot seoulUser(EcoLinkStatus status) {
		return new EcoUserSnapshot(
			"11", "서울특별시", "11620", "관악구", status, null,
			null, null, null, null
		);
	}

	private EcoUserSnapshot nonSeoulUser() {
		return new EcoUserSnapshot(
			"26", "부산광역시", "26440", "강서구", EcoLinkStatus.UNLINKED, null,
			null, null, null, null
		);
	}
}
