package com.greenpocket.greenlife.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.greenlife.dto.GreenlifeItemsResponse;
import com.greenpocket.greenlife.dto.GreenlifeLinkResponse;
import com.greenpocket.greenlife.dto.GreenlifeStatusResponse;
import com.greenpocket.greenlife.repository.GreenlifeRepository;
import com.greenpocket.greenlife.repository.GreenlifeRepository.FeaturedItemSnapshot;
import com.greenpocket.greenlife.repository.GreenlifeRepository.ItemSnapshot;
import com.greenpocket.greenlife.repository.GreenlifeRepository.MonthlyActivitySnapshot;
import com.greenpocket.greenlife.repository.GreenlifeRepository.PaidMonthSnapshot;
import com.greenpocket.greenlife.repository.GreenlifeRepository.UserSnapshot;

class GreenlifeServiceTest {

	private static final Long USER_ID = 1L;
	private static final Clock FIXED_CLOCK = Clock.fixed(
		Instant.parse("2026-09-04T09:00:00Z"),
		ZoneId.of("Asia/Seoul")
	);

	private GreenlifeRepository greenlifeRepository;
	private GreenlifeService greenlifeService;

	@BeforeEach
	void setUp() {
		greenlifeRepository = mock(GreenlifeRepository.class);
		greenlifeService = new GreenlifeService(greenlifeRepository, FIXED_CLOCK);
	}

	@Test
	void returnsProgramAndFourFeaturedItemsWhenNotParticipating() {
		when(greenlifeRepository.findUser(USER_ID)).thenReturn(Optional.of(new UserSnapshot(false, null)));
		when(greenlifeRepository.countActiveItems(2026)).thenReturn(17);
		when(greenlifeRepository.findFeaturedItems(2026)).thenReturn(List.of(
			new FeaturedItemSnapshot(1L, "전자영수증", 10L, "건", "receipt"),
			new FeaturedItemSnapshot(2L, "텀블러·다회용컵", 300L, "개", "tumbler"),
			new FeaturedItemSnapshot(5L, "다회용기", 500L, "회", "container"),
			new FeaturedItemSnapshot(4L, "리필스테이션", 500L, "회", "refill")
		));

		GreenlifeStatusResponse response = greenlifeService.getStatus(USER_ID, "2026-08");

		assertThat(response.participating()).isFalse();
		assertThat(response.screen()).isEqualTo("BN-01");
		assertThat(response.programInfo().itemCount()).isEqualTo(17);
		assertThat(response.programInfo().annualLimit()).isEqualTo(70_000L);
		assertThat(response.featuredItems()).hasSize(4);
		assertThat(response.monthSummary()).isNull();
	}

	@Test
	void returnsRequiredMonthlyAndAnnualAmountsWhenParticipating() {
		when(greenlifeRepository.findUser(USER_ID)).thenReturn(Optional.of(new UserSnapshot(
			true,
			LocalDateTime.of(2026, 9, 2, 18, 30)
		)));
		when(greenlifeRepository.findMonthlyActivity(USER_ID, LocalDate.of(2026, 8, 1)))
			.thenReturn(new MonthlyActivitySnapshot(44, 5_540L));
		when(greenlifeRepository.findLatestPaidMonth(USER_ID, LocalDate.of(2026, 8, 1), 2026))
			.thenReturn(Optional.of(new PaidMonthSnapshot(LocalDate.of(2026, 7, 1), 3_140L)));
		when(greenlifeRepository.findAnnualPaidAmount(USER_ID, 2026)).thenReturn(18_600L);

		GreenlifeStatusResponse response = greenlifeService.getStatus(USER_ID, "2026-08");

		assertThat(response.participating()).isTrue();
		assertThat(response.screen()).isEqualTo("BN-02");
		assertThat(response.month()).isEqualTo("2026-08");
		assertThat(response.monthSummary().activityCount()).isEqualTo(44);
		assertThat(response.monthSummary().pendingAmount()).isEqualTo(5_540L);
		assertThat(response.monthSummary().paidAmount()).isEqualTo(3_140L);
		assertThat(response.monthSummary().paidMonth()).isEqualTo("2026-07");
		assertThat(response.annual().paidAmount()).isEqualTo(18_600L);
		assertThat(response.annual().progressPercent()).isEqualByComparingTo("26.6");
		assertThat(response.annual().limitReached()).isFalse();
	}

	@Test
	void linksMockActivitiesAndReturnsSyncedCount() {
		when(greenlifeRepository.findUser(USER_ID)).thenReturn(Optional.of(new UserSnapshot(false, null)));
		when(greenlifeRepository.findMonthlyActivity(USER_ID, GreenlifeMockData.SUMMARY_MONTH))
			.thenReturn(new MonthlyActivitySnapshot(44, 5_540L));

		GreenlifeLinkResponse response = greenlifeService.link(USER_ID);

		assertThat(response.participating()).isTrue();
		assertThat(response.syncedActivityCount()).isEqualTo(44);
		assertThat(response.screen()).isEqualTo("BN-02");
		verify(greenlifeRepository, times(87)).upsertMockActivity(
			anyLong(), anyString(), anyString(), any(), any(), any(), anyLong(), any(), any(), any(), any()
		);
		verify(greenlifeRepository).markParticipating(USER_ID, GreenlifeMockData.LINKED_AT);
	}

	@Test
	void mockActivitiesMatchTheRequiredDemoTotals() {
		List<GreenlifeMockData.MockActivity> activities = GreenlifeMockData.activities();
		List<GreenlifeMockData.MockActivity> august = activities.stream()
			.filter(activity -> activity.activityMonth().equals(LocalDate.of(2026, 8, 1)))
			.toList();

		assertThat(august).hasSize(44);
		assertThat(august).allMatch(activity -> activity.rewardStatus().name().equals("PENDING"));
		assertThat(august).extracting(GreenlifeMockData.MockActivity::rewardAmount)
			.satisfies(amounts -> assertThat(amounts.stream().mapToLong(Long::longValue).sum())
				.isEqualTo(5_540L));
		assertThat(activities.stream()
			.filter(activity -> activity.rewardStatus().name().equals("PAID"))
			.mapToLong(GreenlifeMockData.MockActivity::rewardAmount)
			.sum()).isEqualTo(18_600L);
		assertThat(activities.stream()
			.filter(activity -> activity.activityMonth().equals(LocalDate.of(2026, 7, 1)))
			.mapToLong(GreenlifeMockData.MockActivity::rewardAmount)
			.sum()).isEqualTo(3_140L);
	}

	@Test
	void returnsAllItemsIncludingItemsWithoutActivity() {
		when(greenlifeRepository.findUser(USER_ID)).thenReturn(Optional.of(new UserSnapshot(true, null)));
		when(greenlifeRepository.findItems(USER_ID, LocalDate.of(2026, 8, 1), 2026)).thenReturn(List.of(
			new ItemSnapshot(
				1L, "E_RECEIPT", "전자영수증", 10L, "건", "receipt", 1,
				new BigDecimal("24.000"), 240L, null, null, 1_000L
			),
			new ItemSnapshot(
				7L, "ECO_PRODUCT", "친환경제품 구매", 500L, "건", "eco", 7,
				new BigDecimal("0.000"), 0L, null, null, 0L
			)
		));

		GreenlifeItemsResponse response = greenlifeService.getItems(USER_ID, "2026-08");

		assertThat(response.month()).isEqualTo("2026-08");
		assertThat(response.totalCount()).isEqualTo(2);
		assertThat(response.collapsedAfter()).isEqualTo(6);
		assertThat(response.items().getFirst().monthCount()).isEqualByComparingTo("24");
		assertThat(response.items().get(1).monthCount()).isEqualByComparingTo("0");
		assertThat(response.items()).allMatch(item -> !item.capReached());
	}

	@Test
	void defaultsItemsMonthToCurrentKoreaMonth() {
		when(greenlifeRepository.findUser(USER_ID)).thenReturn(Optional.of(new UserSnapshot(true, null)));
		when(greenlifeRepository.findItems(USER_ID, LocalDate.of(2026, 9, 1), 2026)).thenReturn(List.of());

		GreenlifeItemsResponse response = greenlifeService.getItems(USER_ID, null);

		assertThat(response.month()).isEqualTo("2026-09");
	}

	@Test
	void rejectsMonthThatIsNotZeroPadded() {
		when(greenlifeRepository.findUser(USER_ID)).thenReturn(Optional.of(new UserSnapshot(true, null)));

		assertThatThrownBy(() -> greenlifeService.getStatus(USER_ID, "2026-7"))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> {
				BusinessException businessException = (BusinessException)error;
				assertThat(businessException.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_REQUEST);
				assertThat(businessException.getField()).isEqualTo("month");
				assertThat(businessException.getDetails()).containsEntry("expectedFormat", "YYYY-MM");
			});
	}

	@Test
	void rejectsUnknownUser() {
		when(greenlifeRepository.findUser(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> greenlifeService.getItems(USER_ID, "2026-08"))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> assertThat(((BusinessException)error).getErrorCode())
				.isEqualTo(CommonErrorCode.UNAUTHENTICATED_DEMO_KEY));
	}
}
