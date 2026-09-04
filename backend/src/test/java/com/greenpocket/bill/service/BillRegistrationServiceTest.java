package com.greenpocket.bill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.bill.dto.BillCreateRequest;
import com.greenpocket.bill.entity.BillType;
import com.greenpocket.bill.entity.InputSource;
import com.greenpocket.bill.entity.RecordSource;
import com.greenpocket.bill.entity.RecordStatus;
import com.greenpocket.bill.entity.UsageUnit;
import com.greenpocket.bill.entity.UtilityMonthlyRecord;
import com.greenpocket.bill.exception.BillErrorCode;
import com.greenpocket.bill.repository.BillRegistrationRepository;
import com.greenpocket.eco.service.EcoMonthlyReportRefreshService;
import com.greenpocket.eco.service.EcoMonthlyReportRefreshService.MonthlyReportRefreshResult;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.type.UtilityType;

class BillRegistrationServiceTest {

	private static final Long USER_ID = 42L;
	private static final Clock CLOCK = Clock.fixed(
		Instant.parse("2026-09-05T00:00:00Z"),
		ZoneId.of("Asia/Seoul")
	);

	private BillRegistrationRepository billRegistrationRepository;
	private EcoMonthlyReportRefreshService ecoMonthlyReportRefreshService;
	private BillRegistrationService service;

	@BeforeEach
	void setUp() {
		billRegistrationRepository = mock(BillRegistrationRepository.class);
		ecoMonthlyReportRefreshService = mock(EcoMonthlyReportRefreshService.class);
		service = new BillRegistrationService(
			billRegistrationRepository,
			ecoMonthlyReportRefreshService,
			CLOCK
		);
	}

	@Test
	void returnsPreviousCalendarMonthAsTarget() {
		when(billRegistrationRepository.findByUserIdAndRecordSourceAndBillingMonth(
			USER_ID, RecordSource.BILL, LocalDate.of(2026, 8, 1)
		)).thenReturn(List.of());
		when(billRegistrationRepository.findFirstByUserIdAndRecordSourceOrderByBillingMonthDescIdDesc(
			USER_ID, RecordSource.BILL
		)).thenReturn(java.util.Optional.empty());

		var response = service.getTargetMonth(USER_ID);

		assertThat(response.targetYearMonth()).isEqualTo("2026-08");
		assertThat(response.lastRegisteredMonth()).isNull();
		assertThat(response.alreadyRegistered()).isFalse();
		assertThat(response.registeredUtilitiesInTarget()).isEmpty();
		assertThat(response.nextScreen()).isEqualTo("AN-02");
	}

	@Test
	void returnsRegisteredUtilitiesAndDiagnosisScreenForTargetMonth() {
		UtilityMonthlyRecord water = record(UtilityType.WATER, 8_900L, "10.000", null);
		UtilityMonthlyRecord electricity = record(UtilityType.ELECTRICITY, 43_200L, "210.000", null);
		when(billRegistrationRepository.findByUserIdAndRecordSourceAndBillingMonth(
			USER_ID, RecordSource.BILL, LocalDate.of(2026, 8, 1)
		)).thenReturn(List.of(water, electricity));
		when(billRegistrationRepository.findFirstByUserIdAndRecordSourceOrderByBillingMonthDescIdDesc(
			USER_ID, RecordSource.BILL
		)).thenReturn(java.util.Optional.of(water));

		var response = service.getTargetMonth(USER_ID);

		assertThat(response.alreadyRegistered()).isTrue();
		assertThat(response.registeredUtilitiesInTarget())
			.containsExactly(UtilityType.ELECTRICITY, UtilityType.WATER);
		assertThat(response.nextScreen()).isEqualTo("AN-07");
	}

	@Test
	void reportsDuplicatesPerRequestedUtility() {
		UtilityMonthlyRecord electricity = record(UtilityType.ELECTRICITY, 43_200L, "210.000", null);
		when(billRegistrationRepository.findByUserIdAndRecordSourceAndBillingMonthAndUtilityTypeIn(
			eq(USER_ID),
			eq(RecordSource.BILL),
			eq(LocalDate.of(2026, 8, 1)),
			anyCollection()
		)).thenReturn(List.of(electricity));

		var response = service.checkDuplicates(
			USER_ID,
			java.time.YearMonth.of(2026, 8),
			List.of(UtilityType.ELECTRICITY, UtilityType.WATER)
		);

		assertThat(response.results()).extracting(result -> result.duplicated())
			.containsExactly(true, false);
	}

	@Test
	void createsRecordsAndReturnsRecalculationResult() {
		when(billRegistrationRepository.findByUserIdAndRecordSourceAndBillingMonthAndUtilityTypeIn(
			USER_ID,
			RecordSource.BILL,
			LocalDate.of(2026, 8, 1),
			List.of(UtilityType.ELECTRICITY, UtilityType.WATER)
		)).thenReturn(List.of());
		when(billRegistrationRepository.saveAllAndFlush(anyList()))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(ecoMonthlyReportRefreshService.refresh(USER_ID, java.time.YearMonth.of(2026, 8)))
			.thenReturn(new MonthlyReportRefreshResult(true, 7L));

		var response = service.create(USER_ID, ocrRequest());

		assertThat(response.totalAmount()).isEqualTo(52_100L);
		assertThat(response.records()).extracting(record -> record.recordStatus())
			.containsExactly(RecordStatus.CONFIRMED, RecordStatus.REVIEW_REQUIRED);
		assertThat(response.recalculated().monthlyReportUpdated()).isTrue();
		assertThat(response.recalculated().roundId()).isEqualTo(7L);
		assertThat(response.nextScreen()).isEqualTo("AN-07");
		verify(billRegistrationRepository).saveAllAndFlush(anyList());
	}

	@Test
	void rejectsExistingUtilityForSameMonth() {
		UtilityMonthlyRecord existing = record(UtilityType.ELECTRICITY, 40_000L, "200.000", null);
		when(billRegistrationRepository.findByUserIdAndRecordSourceAndBillingMonthAndUtilityTypeIn(
			USER_ID,
			RecordSource.BILL,
			LocalDate.of(2026, 8, 1),
			List.of(UtilityType.ELECTRICITY, UtilityType.WATER)
		)).thenReturn(List.of(existing));

		assertThatThrownBy(() -> service.create(USER_ID, ocrRequest()))
			.isInstanceOfSatisfying(BusinessException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(BillErrorCode.BILL_DUPLICATED);
				assertThat(exception.getDetails()).containsEntry("billingMonth", "2026-08");
			});
	}

	@Test
	void rejectsEmptyItems() {
		BillCreateRequest request = new BillCreateRequest(
			java.time.YearMonth.of(2026, 8),
			BillType.MANAGEMENT,
			InputSource.OCR,
			List.of()
		);

		assertThatThrownBy(() -> service.create(USER_ID, request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(BillErrorCode.BILL_ITEM_EMPTY));
	}

	@Test
	void rejectsManualInputWithoutElectricity() {
		BillCreateRequest request = new BillCreateRequest(
			java.time.YearMonth.of(2026, 8),
			BillType.WATER,
			InputSource.MANUAL,
			List.of(new BillCreateRequest.Item(
				UtilityType.WATER, 8_900L, new BigDecimal("10.000"), UsageUnit.m3, null
			))
		);

		assertThatThrownBy(() -> service.create(USER_ID, request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(BillErrorCode.BILL_ELECTRICITY_REQUIRED));
	}

	@Test
	void rejectsMissingUsage() {
		BillCreateRequest request = new BillCreateRequest(
			java.time.YearMonth.of(2026, 8),
			BillType.ELECTRICITY,
			InputSource.MANUAL,
			List.of(new BillCreateRequest.Item(
				UtilityType.ELECTRICITY, 43_200L, null, UsageUnit.kWh, null
			))
		);

		assertThatThrownBy(() -> service.create(USER_ID, request))
			.isInstanceOfSatisfying(BusinessException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(BillErrorCode.BILL_USAGE_REQUIRED);
				assertThat(exception.getField()).isEqualTo("items[0].usage");
			});
	}

	private BillCreateRequest ocrRequest() {
		return new BillCreateRequest(
			java.time.YearMonth.of(2026, 8),
			BillType.MANAGEMENT,
			InputSource.OCR,
			List.of(
				new BillCreateRequest.Item(
					UtilityType.ELECTRICITY,
					43_200L,
					new BigDecimal("210.000"),
					UsageUnit.kWh,
					new BigDecimal("0.9412")
				),
				new BillCreateRequest.Item(
					UtilityType.WATER,
					8_900L,
					new BigDecimal("10.000"),
					UsageUnit.m3,
					new BigDecimal("0.6120")
				)
			)
		);
	}

	private UtilityMonthlyRecord record(
		UtilityType utilityType,
		long amount,
		String usage,
		BigDecimal confidence
	) {
		return UtilityMonthlyRecord.createBill(
			USER_ID,
			LocalDate.of(2026, 8, 1),
			utilityType,
			BillType.MANAGEMENT,
			amount,
			new BigDecimal(usage),
			utilityType == UtilityType.ELECTRICITY ? UsageUnit.kWh : UsageUnit.m3,
			InputSource.OCR,
			confidence,
			RecordStatus.CONFIRMED
		);
	}
}
