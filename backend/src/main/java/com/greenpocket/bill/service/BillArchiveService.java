package com.greenpocket.bill.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.bill.dto.BillDeleteResponse;
import com.greenpocket.bill.dto.BillDetailResponse;
import com.greenpocket.bill.dto.BillListResponse;
import com.greenpocket.bill.dto.BillRecalculatedResponse;
import com.greenpocket.bill.dto.BillUpdateRequest;
import com.greenpocket.bill.dto.BillUpdateResponse;
import com.greenpocket.bill.entity.RecordSource;
import com.greenpocket.bill.entity.UtilityMonthlyRecord;
import com.greenpocket.bill.repository.BillArchiveQueryRepository;
import com.greenpocket.bill.repository.BillRegistrationRepository;
import com.greenpocket.eco.service.EcoMonthlyReportRefreshService;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.global.type.UtilityType;

@Service
public class BillArchiveService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final Map<UtilityType, Integer> UTILITY_ORDER = Map.of(
		UtilityType.ELECTRICITY, 0,
		UtilityType.WATER, 1,
		UtilityType.GAS, 2
	);

	private final BillArchiveQueryRepository billArchiveQueryRepository;
	private final BillRegistrationRepository billRegistrationRepository;
	private final EcoMonthlyReportRefreshService ecoMonthlyReportRefreshService;
	private final Clock clock;

	@Autowired
	public BillArchiveService(
		BillArchiveQueryRepository billArchiveQueryRepository,
		BillRegistrationRepository billRegistrationRepository,
		EcoMonthlyReportRefreshService ecoMonthlyReportRefreshService
	) {
		this(
			billArchiveQueryRepository,
			billRegistrationRepository,
			ecoMonthlyReportRefreshService,
			Clock.system(KOREA_ZONE_ID)
		);
	}

	BillArchiveService(
		BillArchiveQueryRepository billArchiveQueryRepository,
		BillRegistrationRepository billRegistrationRepository,
		EcoMonthlyReportRefreshService ecoMonthlyReportRefreshService,
		Clock clock
	) {
		this.billArchiveQueryRepository = billArchiveQueryRepository;
		this.billRegistrationRepository = billRegistrationRepository;
		this.ecoMonthlyReportRefreshService = ecoMonthlyReportRefreshService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public BillListResponse findBills(
		Long userId,
		UtilityType utilityType,
		Integer year,
		int page,
		int size
	) {
		var snapshots = billArchiveQueryRepository.findBills(userId, utilityType, year, page, size);
		long totalElements = billArchiveQueryRepository.countBills(userId, utilityType, year);
		int totalPages = totalElements == 0 ? 0 : (int)((totalElements + size - 1) / size);
		List<BillListResponse.Item> content = snapshots.stream()
			.map(snapshot -> new BillListResponse.Item(
				snapshot.recordId(),
				YearMonth.from(snapshot.billingMonth()).toString(),
				snapshot.utilityType(),
				snapshot.billType(),
				snapshot.amount(),
				snapshot.usage(),
				snapshot.usageUnit(),
				snapshot.inputSource(),
				snapshot.recordStatus(),
				toOffsetDateTime(snapshot.registeredAt())
			))
			.toList();

		Map<UtilityType, Long> utilityCounts = billArchiveQueryRepository.countByUtility(userId, year);
		Map<String, Long> counts = new LinkedHashMap<>();
		counts.put("ALL", utilityCounts.values().stream().mapToLong(Long::longValue).sum());
		counts.put("ELECTRICITY", utilityCounts.getOrDefault(UtilityType.ELECTRICITY, 0L));
		counts.put("WATER", utilityCounts.getOrDefault(UtilityType.WATER, 0L));
		counts.put("GAS", utilityCounts.getOrDefault(UtilityType.GAS, 0L));

		return new BillListResponse(
			content,
			page,
			size,
			totalElements,
			totalPages,
			page + 1 < totalPages,
			counts
		);
	}

	@Transactional(readOnly = true)
	public BillDetailResponse findDetail(Long userId, Long recordId) {
		UtilityMonthlyRecord record = findOwnedBill(userId, recordId);
		List<BillDetailResponse.Sibling> siblings = billRegistrationRepository
			.findByUserIdAndRecordSourceAndBillingMonth(userId, RecordSource.BILL, record.getBillingMonth())
			.stream()
			.filter(sibling -> !sibling.getId().equals(recordId))
			.sorted(Comparator.comparingInt(sibling -> UTILITY_ORDER.get(sibling.getUtilityType())))
			.map(sibling -> new BillDetailResponse.Sibling(
				sibling.getId(),
				sibling.getUtilityType(),
				sibling.getAmount()
			))
			.toList();
		return new BillDetailResponse(
			record.getId(),
			YearMonth.from(record.getBillingMonth()).toString(),
			record.getUtilityType(),
			record.getBillType(),
			record.getAmount(),
			record.getUsageValue(),
			record.getUsageUnit(),
			record.getInputSource(),
			record.getConfidence(),
			record.getRecordStatus(),
			toOffsetDateTime(record.getRegisteredAt()),
			toOffsetDateTime(record.getUpdatedAt()),
			siblings
		);
	}

	@Transactional
	public BillUpdateResponse update(Long userId, Long recordId, BillUpdateRequest request) {
		UtilityMonthlyRecord record = findOwnedBill(userId, recordId);
		LocalDateTime updatedAt = LocalDateTime.now(clock);
		record.updateAmountAndUsage(request.amount(), request.usage(), updatedAt);
		billRegistrationRepository.flush();

		YearMonth billingMonth = YearMonth.from(record.getBillingMonth());
		var refreshResult = ecoMonthlyReportRefreshService.refresh(userId, billingMonth);
		return new BillUpdateResponse(
			record.getId(),
			record.getAmount(),
			record.getUsageValue(),
			toOffsetDateTime(updatedAt),
			new BillRecalculatedResponse(billingMonth.toString(), refreshResult.updated())
		);
	}

	@Transactional
	public BillDeleteResponse delete(Long userId, Long recordId) {
		UtilityMonthlyRecord record = findOwnedBill(userId, recordId);
		YearMonth billingMonth = YearMonth.from(record.getBillingMonth());
		billRegistrationRepository.delete(record);
		billRegistrationRepository.flush();

		var refreshResult = ecoMonthlyReportRefreshService.refresh(userId, billingMonth);
		return new BillDeleteResponse(
			recordId,
			new BillRecalculatedResponse(billingMonth.toString(), refreshResult.updated())
		);
	}

	private UtilityMonthlyRecord findOwnedBill(Long userId, Long recordId) {
		return billRegistrationRepository.findByIdAndUserIdAndRecordSource(
			recordId,
			userId,
			RecordSource.BILL
		).orElseThrow(() -> new BusinessException(
			CommonErrorCode.NOT_FOUND,
			null,
			Map.of("recordId", recordId)
		));
	}

	private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
		return value == null ? null : value.atZone(KOREA_ZONE_ID).toOffsetDateTime();
	}
}
