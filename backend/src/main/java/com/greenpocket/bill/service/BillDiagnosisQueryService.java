package com.greenpocket.bill.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.bill.repository.BillDiagnosisQueryRepository;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillDiagnosisQueryService {

	private final BillDiagnosisQueryRepository billDiagnosisQueryRepository;

	public List<MonthlyRecord> findAllBills(Long userId) {
		return billDiagnosisQueryRepository
			.findAllBillRecords(userId)
			.stream()
			.map(BillDiagnosisQueryService::toRecord)
			.toList();
	}

	public List<MonthlyRecord> findBills(Long userId, YearMonth from, YearMonth to) {
		return billDiagnosisQueryRepository.findBillRecords(userId, from.atDay(1), to.atDay(1))
			.stream()
			.map(BillDiagnosisQueryService::toRecord)
			.toList();
	}

	public List<MonthlyRecord> findPreviousYearBaseline(Long userId, YearMonth month) {
		return billDiagnosisQueryRepository.findEcoBaselineRecords(userId, month.atDay(1))
			.stream()
			.map(BillDiagnosisQueryService::toRecord)
			.toList();
	}

	private static MonthlyRecord toRecord(
		BillDiagnosisQueryRepository.MonthlyRecordSnapshot snapshot
	) {
		return new MonthlyRecord(
			YearMonth.from(snapshot.billingMonth()),
			snapshot.utilityType(),
			snapshot.amount(),
			snapshot.usage(),
			snapshot.usageUnit()
		);
	}

	public record MonthlyRecord(
		YearMonth yearMonth,
		UtilityType utilityType,
		long amount,
		BigDecimal usage,
		UsageUnit usageUnit
	) {
	}
}
