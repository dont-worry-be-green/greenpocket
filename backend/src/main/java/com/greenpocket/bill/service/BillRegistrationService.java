package com.greenpocket.bill.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.bill.dto.BillCreateRequest;
import com.greenpocket.bill.dto.BillCreateResponse;
import com.greenpocket.bill.dto.BillDuplicateCheckResponse;
import com.greenpocket.bill.dto.BillTargetMonthResponse;
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
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.global.type.UtilityType;

@Service
public class BillRegistrationService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final BigDecimal REVIEW_REQUIRED_THRESHOLD = new BigDecimal("0.7000");
	private static final String INPUT_SCREEN = "AN-02";
	private static final String DIAGNOSIS_SCREEN = "AN-07";
	private static final Map<UtilityType, Integer> UTILITY_ORDER = new EnumMap<>(UtilityType.class);

	static {
		UTILITY_ORDER.put(UtilityType.ELECTRICITY, 0);
		UTILITY_ORDER.put(UtilityType.WATER, 1);
		UTILITY_ORDER.put(UtilityType.GAS, 2);
	}

	private final BillRegistrationRepository billRegistrationRepository;
	private final EcoMonthlyReportRefreshService ecoMonthlyReportRefreshService;
	private final Clock clock;

	@Autowired
	public BillRegistrationService(
		BillRegistrationRepository billRegistrationRepository,
		EcoMonthlyReportRefreshService ecoMonthlyReportRefreshService
	) {
		this(billRegistrationRepository, ecoMonthlyReportRefreshService, Clock.system(KOREA_ZONE_ID));
	}

	BillRegistrationService(
		BillRegistrationRepository billRegistrationRepository,
		EcoMonthlyReportRefreshService ecoMonthlyReportRefreshService,
		Clock clock
	) {
		this.billRegistrationRepository = billRegistrationRepository;
		this.ecoMonthlyReportRefreshService = ecoMonthlyReportRefreshService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public BillTargetMonthResponse getTargetMonth(Long userId) {
		YearMonth targetMonth = YearMonth.now(clock).minusMonths(1);
		List<UtilityMonthlyRecord> targetRecords = billRegistrationRepository
			.findByUserIdAndRecordSourceAndBillingMonth(userId, RecordSource.BILL, targetMonth.atDay(1));
		List<UtilityType> utilities = targetRecords.stream()
			.map(UtilityMonthlyRecord::getUtilityType)
			.distinct()
			.sorted(Comparator.comparingInt(UTILITY_ORDER::get))
			.toList();
		String lastRegisteredMonth = billRegistrationRepository
			.findFirstByUserIdAndRecordSourceOrderByBillingMonthDescIdDesc(userId, RecordSource.BILL)
			.map(UtilityMonthlyRecord::getBillingMonth)
			.map(YearMonth::from)
			.map(YearMonth::toString)
			.orElse(null);
		boolean alreadyRegistered = !utilities.isEmpty();
		return new BillTargetMonthResponse(
			targetMonth.toString(),
			lastRegisteredMonth,
			alreadyRegistered,
			utilities,
			alreadyRegistered ? DIAGNOSIS_SCREEN : INPUT_SCREEN
		);
	}

	@Transactional(readOnly = true)
	public BillDuplicateCheckResponse checkDuplicates(
		Long userId,
		YearMonth billingMonth,
		List<UtilityType> utilityTypes
	) {
		if (utilityTypes == null || utilityTypes.isEmpty()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "utilityTypes", null);
		}
		Set<UtilityType> requestedTypes = new LinkedHashSet<>(utilityTypes);
		if (requestedTypes.size() != utilityTypes.size()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "utilityTypes", null);
		}

		Map<UtilityType, Long> existingIds = new EnumMap<>(UtilityType.class);
		billRegistrationRepository
			.findByUserIdAndRecordSourceAndBillingMonthAndUtilityTypeIn(
				userId,
				RecordSource.BILL,
				billingMonth.atDay(1),
				requestedTypes
			)
			.forEach(record -> existingIds.put(record.getUtilityType(), record.getId()));

		List<BillDuplicateCheckResponse.Result> results = requestedTypes.stream()
			.map(utilityType -> new BillDuplicateCheckResponse.Result(
				utilityType,
				existingIds.containsKey(utilityType),
				existingIds.get(utilityType)
			))
			.toList();
		return new BillDuplicateCheckResponse(billingMonth.toString(), results);
	}

	@Transactional
	public BillCreateResponse create(Long userId, BillCreateRequest request) {
		List<BillCreateRequest.Item> items = validateItems(request);
		List<UtilityType> utilityTypes = items.stream().map(BillCreateRequest.Item::utilityType).toList();
		List<UtilityMonthlyRecord> duplicates = findDuplicates(userId, request.billingMonth(), utilityTypes);
		if (!duplicates.isEmpty()) {
			throw duplicated(request.billingMonth(), duplicates);
		}

		List<UtilityMonthlyRecord> records = items.stream()
			.map(item -> UtilityMonthlyRecord.createBill(
				userId,
				request.billingMonth().atDay(1),
				item.utilityType(),
				request.billType(),
				item.amount(),
				item.usage(),
				item.usageUnit(),
				request.inputSource(),
				item.confidence(),
				recordStatus(item.confidence())
			))
			.toList();

		try {
			billRegistrationRepository.saveAllAndFlush(records);
		}
		catch (DataIntegrityViolationException exception) {
			List<UtilityMonthlyRecord> concurrentDuplicates = findDuplicates(
				userId,
				request.billingMonth(),
				utilityTypes
			);
			if (!concurrentDuplicates.isEmpty()) {
				throw duplicated(request.billingMonth(), concurrentDuplicates);
			}
			throw exception;
		}

		MonthlyReportRefreshResult refreshResult = ecoMonthlyReportRefreshService.refresh(
			userId,
			request.billingMonth()
		);
		List<BillCreateResponse.Record> responseRecords = records.stream()
			.sorted(Comparator.comparingInt(record -> UTILITY_ORDER.get(record.getUtilityType())))
			.map(record -> new BillCreateResponse.Record(
				record.getId(),
				record.getUtilityType(),
				record.getAmount(),
				record.getUsageValue(),
				record.getRecordStatus()
			))
			.toList();
		long totalAmount = records.stream().mapToLong(UtilityMonthlyRecord::getAmount).sum();
		return new BillCreateResponse(
			request.billingMonth().toString(),
			responseRecords,
			totalAmount,
			new BillCreateResponse.Recalculated(
				request.billingMonth().toString(),
				refreshResult.updated(),
				refreshResult.roundId()
			),
			DIAGNOSIS_SCREEN
		);
	}

	private List<BillCreateRequest.Item> validateItems(BillCreateRequest request) {
		if (request.items() == null || request.items().isEmpty()) {
			throw new BusinessException(BillErrorCode.BILL_ITEM_EMPTY, "items", null);
		}
		Set<UtilityType> uniqueTypes = new LinkedHashSet<>();
		List<BillCreateRequest.Item> items = new ArrayList<>(request.items());
		for (int index = 0; index < items.size(); index++) {
			BillCreateRequest.Item item = items.get(index);
			if (item.usage() == null) {
				throw new BusinessException(BillErrorCode.BILL_USAGE_REQUIRED, "items[%d].usage".formatted(index), null);
			}
			if (!expectedUnit(item.utilityType()).equals(item.usageUnit())) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "items[%d].usageUnit".formatted(index), null);
			}
			if (!uniqueTypes.add(item.utilityType())) {
				throw new BusinessException(
					BillErrorCode.BILL_DUPLICATED,
					"items[%d].utilityType".formatted(index),
					Map.of("utilityType", item.utilityType().name())
				);
			}
		}
		if (request.inputSource() == InputSource.MANUAL && !uniqueTypes.contains(UtilityType.ELECTRICITY)) {
			throw new BusinessException(BillErrorCode.BILL_ELECTRICITY_REQUIRED, "items", null);
		}
		return items;
	}

	private List<UtilityMonthlyRecord> findDuplicates(
		Long userId,
		YearMonth billingMonth,
		List<UtilityType> utilityTypes
	) {
		return billRegistrationRepository.findByUserIdAndRecordSourceAndBillingMonthAndUtilityTypeIn(
			userId,
			RecordSource.BILL,
			billingMonth.atDay(1),
			utilityTypes
		);
	}

	private BusinessException duplicated(YearMonth billingMonth, List<UtilityMonthlyRecord> duplicates) {
		List<String> duplicatedUtilities = duplicates.stream()
			.map(UtilityMonthlyRecord::getUtilityType)
			.distinct()
			.sorted(Comparator.comparingInt(UTILITY_ORDER::get))
			.map(Enum::name)
			.toList();
		return new BusinessException(
			BillErrorCode.BILL_DUPLICATED,
			null,
			Map.of("billingMonth", billingMonth.toString(), "utilityTypes", duplicatedUtilities)
		);
	}

	private RecordStatus recordStatus(BigDecimal confidence) {
		return confidence != null && confidence.compareTo(REVIEW_REQUIRED_THRESHOLD) < 0
			? RecordStatus.REVIEW_REQUIRED
			: RecordStatus.CONFIRMED;
	}

	private UsageUnit expectedUnit(UtilityType utilityType) {
		return utilityType == UtilityType.ELECTRICITY ? UsageUnit.kWh : UsageUnit.m3;
	}
}
