package com.greenpocket.eco.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.eco.repository.EcoReportQueryRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EcoReportQueryService {

	private final EcoReportQueryRepository ecoReportQueryRepository;

	public List<EcoMonthlyReport> findMonthlyReports(Long userId) {
		return ecoReportQueryRepository.findMonthlyReports(userId).stream()
			.map(snapshot -> new EcoMonthlyReport(
				YearMonth.from(snapshot.reportMonth()),
				snapshot.calculatedAt()
			))
			.toList();
	}

	public List<EcoResultReport> findResultReports(Long userId) {
		return ecoReportQueryRepository.findResultReports(userId).stream()
			.map(snapshot -> new EcoResultReport(
				snapshot.roundId(),
				YearMonth.from(snapshot.periodStart()),
				YearMonth.from(snapshot.periodEnd()),
				snapshot.confirmedAt()
			))
			.toList();
	}

	public record EcoMonthlyReport(
		YearMonth yearMonth,
		LocalDateTime createdAt
	) {
	}

	public record EcoResultReport(
		Long roundId,
		YearMonth periodStart,
		YearMonth periodEnd,
		LocalDateTime createdAt
	) {
	}
}
