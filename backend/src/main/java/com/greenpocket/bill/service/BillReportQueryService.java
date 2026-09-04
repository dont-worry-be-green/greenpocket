package com.greenpocket.bill.service;

import java.time.YearMonth;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.bill.repository.BillReportQueryRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillReportQueryService {

	private final BillReportQueryRepository billReportQueryRepository;

	public long countBills(Long userId) {
		return billReportQueryRepository.countBills(userId);
	}

	public List<MonthlyDiagnosisReport> findMonthlyDiagnosisReports(Long userId) {
		return billReportQueryRepository.findMonthlyDiagnosisReports(userId).stream()
			.map(snapshot -> new MonthlyDiagnosisReport(
				YearMonth.from(snapshot.billingMonth()),
				snapshot.createdAt()
			))
			.toList();
	}

	public record MonthlyDiagnosisReport(
		YearMonth yearMonth,
		java.time.LocalDateTime createdAt
	) {
	}
}
