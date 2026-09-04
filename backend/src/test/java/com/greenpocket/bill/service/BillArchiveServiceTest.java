package com.greenpocket.bill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.greenpocket.bill.dto.BillUpdateRequest;
import com.greenpocket.bill.entity.BillType;
import com.greenpocket.bill.entity.InputSource;
import com.greenpocket.bill.entity.RecordSource;
import com.greenpocket.bill.entity.RecordStatus;
import com.greenpocket.bill.entity.UsageUnit;
import com.greenpocket.bill.entity.UtilityMonthlyRecord;
import com.greenpocket.bill.repository.BillArchiveQueryRepository;
import com.greenpocket.bill.repository.BillArchiveQueryRepository.BillListSnapshot;
import com.greenpocket.bill.repository.BillRegistrationRepository;
import com.greenpocket.eco.service.EcoMonthlyReportRefreshService;
import com.greenpocket.eco.service.EcoMonthlyReportRefreshService.MonthlyReportRefreshResult;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.global.type.UtilityType;

class BillArchiveServiceTest {

	private static final Long USER_ID = 42L;
	private static final Clock CLOCK = Clock.fixed(
		Instant.parse("2026-09-05T01:30:00Z"),
		ZoneId.of("Asia/Seoul")
	);

	private BillArchiveQueryRepository billArchiveQueryRepository;
	private BillRegistrationRepository billRegistrationRepository;
	private EcoMonthlyReportRefreshService ecoMonthlyReportRefreshService;
	private BillArchiveService service;

	@BeforeEach
	void setUp() {
		billArchiveQueryRepository = mock(BillArchiveQueryRepository.class);
		billRegistrationRepository = mock(BillRegistrationRepository.class);
		ecoMonthlyReportRefreshService = mock(EcoMonthlyReportRefreshService.class);
		service = new BillArchiveService(
			billArchiveQueryRepository,
			billRegistrationRepository,
			ecoMonthlyReportRefreshService,
			CLOCK
		);
	}

	@Test
	void returnsFilteredPageAndYearScopedTabCounts() {
		when(billArchiveQueryRepository.findBills(USER_ID, UtilityType.ELECTRICITY, 2026, 0, 1))
			.thenReturn(List.of(new BillListSnapshot(
				51L,
				LocalDate.of(2026, 8, 1),
				UtilityType.ELECTRICITY,
				BillType.MANAGEMENT,
				43_200L,
				new BigDecimal("210.000"),
				UsageUnit.kWh,
				InputSource.OCR,
				RecordStatus.CONFIRMED,
				LocalDateTime.of(2026, 9, 1, 10, 22)
			)));
		when(billArchiveQueryRepository.countBills(USER_ID, UtilityType.ELECTRICITY, 2026))
			.thenReturn(2L);
		when(billArchiveQueryRepository.countByUtility(USER_ID, 2026)).thenReturn(Map.of(
			UtilityType.ELECTRICITY, 2L,
			UtilityType.WATER, 1L
		));

		var response = service.findBills(USER_ID, UtilityType.ELECTRICITY, 2026, 0, 1);

		assertThat(response.content()).hasSize(1);
		assertThat(response.totalElements()).isEqualTo(2L);
		assertThat(response.totalPages()).isEqualTo(2);
		assertThat(response.hasNext()).isTrue();
		assertThat(response.counts()).containsExactly(
			Map.entry("ALL", 3L),
			Map.entry("ELECTRICITY", 2L),
			Map.entry("WATER", 1L),
			Map.entry("GAS", 0L)
		);
	}

	@Test
	void returnsDetailWithSameMonthSiblingsOnly() {
		UtilityMonthlyRecord electricity = record(51L, UtilityType.ELECTRICITY, 43_200L, "210.000");
		UtilityMonthlyRecord water = record(52L, UtilityType.WATER, 8_900L, "10.000");
		when(billRegistrationRepository.findByIdAndUserIdAndRecordSource(
			51L, USER_ID, RecordSource.BILL
		)).thenReturn(Optional.of(electricity));
		when(billRegistrationRepository.findByUserIdAndRecordSourceAndBillingMonth(
			USER_ID, RecordSource.BILL, LocalDate.of(2026, 8, 1)
		)).thenReturn(List.of(electricity, water));

		var response = service.findDetail(USER_ID, 51L);

		assertThat(response.recordId()).isEqualTo(51L);
		assertThat(response.billingMonth()).isEqualTo("2026-08");
		assertThat(response.siblings()).containsExactly(
			new com.greenpocket.bill.dto.BillDetailResponse.Sibling(52L, UtilityType.WATER, 8_900L)
		);
	}

	@Test
	void rejectsMissingOrOtherUsersRecord() {
		when(billRegistrationRepository.findByIdAndUserIdAndRecordSource(
			999L, USER_ID, RecordSource.BILL
		)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findDetail(USER_ID, 999L))
			.isInstanceOfSatisfying(BusinessException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.NOT_FOUND);
				assertThat(exception.getDetails()).containsEntry("recordId", 999L);
			});
	}

	@Test
	void updatesAmountAndUsageThenRefreshesMonthlyReport() {
		UtilityMonthlyRecord record = record(51L, UtilityType.ELECTRICITY, 43_200L, "210.000");
		when(billRegistrationRepository.findByIdAndUserIdAndRecordSource(
			51L, USER_ID, RecordSource.BILL
		)).thenReturn(Optional.of(record));
		when(ecoMonthlyReportRefreshService.refresh(USER_ID, java.time.YearMonth.of(2026, 8)))
			.thenReturn(new MonthlyReportRefreshResult(true, 7L));

		var response = service.update(
			USER_ID,
			51L,
			new BillUpdateRequest(41_800L, new BigDecimal("203.000"))
		);

		assertThat(response.amount()).isEqualTo(41_800L);
		assertThat(response.usage()).isEqualByComparingTo("203.000");
		assertThat(response.updatedAt().toString()).isEqualTo("2026-09-05T10:30+09:00");
		assertThat(response.recalculated().monthlyReportUpdated()).isTrue();
		verify(billRegistrationRepository).flush();
	}

	@Test
	void deletesOwnedRecordThenRefreshesMonthlyReport() {
		UtilityMonthlyRecord record = record(51L, UtilityType.ELECTRICITY, 43_200L, "210.000");
		when(billRegistrationRepository.findByIdAndUserIdAndRecordSource(
			51L, USER_ID, RecordSource.BILL
		)).thenReturn(Optional.of(record));
		when(ecoMonthlyReportRefreshService.refresh(USER_ID, java.time.YearMonth.of(2026, 8)))
			.thenReturn(new MonthlyReportRefreshResult(false, null));

		var response = service.delete(USER_ID, 51L);

		assertThat(response.deletedRecordId()).isEqualTo(51L);
		assertThat(response.recalculated().diagnosisMonth()).isEqualTo("2026-08");
		assertThat(response.recalculated().monthlyReportUpdated()).isFalse();
		verify(billRegistrationRepository).delete(record);
		verify(billRegistrationRepository).flush();
	}

	private UtilityMonthlyRecord record(Long id, UtilityType utilityType, long amount, String usage) {
		UtilityMonthlyRecord record = UtilityMonthlyRecord.createBill(
			USER_ID,
			LocalDate.of(2026, 8, 1),
			utilityType,
			BillType.MANAGEMENT,
			amount,
			new BigDecimal(usage),
			utilityType == UtilityType.ELECTRICITY ? UsageUnit.kWh : UsageUnit.m3,
			InputSource.OCR,
			new BigDecimal("0.9000"),
			RecordStatus.CONFIRMED
		);
		ReflectionTestUtils.setField(record, "id", id);
		ReflectionTestUtils.setField(record, "registeredAt", LocalDateTime.of(2026, 9, 1, 10, 22));
		ReflectionTestUtils.setField(record, "updatedAt", LocalDateTime.of(2026, 9, 1, 10, 22));
		return record;
	}
}
