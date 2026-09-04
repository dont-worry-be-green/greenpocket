package com.greenpocket.mypage.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.bill.service.BillReportQueryService;
import com.greenpocket.eco.service.EcoReportQueryService;
import com.greenpocket.mypage.dto.ReportListResponse;
import com.greenpocket.mypage.dto.ReportType;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final String DIAGNOSIS_SCREEN = "AN-07";
	private static final String ECO_MONTHLY_SCREEN = "WF-07";
	private static final String ECO_RESULT_SCREEN = "WF-10";

	private final BillReportQueryService billReportQueryService;
	private final EcoReportQueryService ecoReportQueryService;

	public ReportListResponse getReports(Long userId, ReportType type, Integer year, int page, int size) {
		List<ReportCandidate> reports = new ArrayList<>();
		if (type == null || type == ReportType.MONTHLY_DIAGNOSIS) {
			reports.addAll(monthlyDiagnosisReports(userId));
		}
		if (type == null || type == ReportType.ECO_MONTHLY) {
			reports.addAll(ecoMonthlyReports(userId));
		}
		if (type == null || type == ReportType.ECO_RESULT) {
			reports.addAll(ecoResultReports(userId));
		}

		List<ReportCandidate> filtered = reports.stream()
			.filter(report -> year == null || report.yearMonth().getYear() == year)
			.sorted(Comparator
				.comparing(ReportCandidate::yearMonth, Comparator.reverseOrder())
				.thenComparing(ReportCandidate::createdAt, Comparator.reverseOrder())
				.thenComparing(ReportCandidate::reportId))
			.toList();

		long totalElements = filtered.size();
		int totalPages = totalElements == 0 ? 0 : (int)((totalElements + size - 1) / size);
		long firstIndex = (long)page * size;
		List<ReportListResponse.Item> content = firstIndex >= totalElements
			? List.of()
			: filtered.subList((int)firstIndex, (int)Math.min(firstIndex + size, totalElements)).stream()
				.map(this::toResponse)
				.toList();

		return new ReportListResponse(
			content,
			page,
			size,
			totalElements,
			totalPages,
			page + 1 < totalPages
		);
	}

	private List<ReportCandidate> monthlyDiagnosisReports(Long userId) {
		return billReportQueryService.findMonthlyDiagnosisReports(userId).stream()
			.map(report -> new ReportCandidate(
				"MONTHLY_DIAGNOSIS:" + report.yearMonth(),
				ReportType.MONTHLY_DIAGNOSIS,
				report.yearMonth(),
				report.yearMonth().getMonthValue() + "월 생활비 진단",
				report.createdAt(),
				DIAGNOSIS_SCREEN,
				Map.of("month", report.yearMonth().toString())
			))
			.toList();
	}

	private List<ReportCandidate> ecoMonthlyReports(Long userId) {
		return ecoReportQueryService.findMonthlyReports(userId).stream()
			.map(report -> new ReportCandidate(
				"ECO_MONTHLY:" + report.yearMonth(),
				ReportType.ECO_MONTHLY,
				report.yearMonth(),
				report.yearMonth().getMonthValue() + "월분 전달 리포트",
				report.createdAt(),
				ECO_MONTHLY_SCREEN,
				Map.of("month", report.yearMonth().toString())
			))
			.toList();
	}

	private List<ReportCandidate> ecoResultReports(Long userId) {
		return ecoReportQueryService.findResultReports(userId).stream()
			.map(report -> new ReportCandidate(
				"ECO_RESULT:" + report.roundId(),
				ReportType.ECO_RESULT,
				report.periodEnd(),
				resultTitle(report.periodStart(), report.periodEnd()),
				report.createdAt(),
				ECO_RESULT_SCREEN,
				Map.of("roundId", report.roundId())
			))
			.toList();
	}

	private ReportListResponse.Item toResponse(ReportCandidate report) {
		return new ReportListResponse.Item(
			report.reportId(),
			report.type(),
			report.yearMonth().toString(),
			report.title(),
			toOffsetDateTime(report.createdAt()),
			report.targetScreen(),
			report.targetParams(),
			false
		);
	}

	private String resultTitle(YearMonth start, YearMonth end) {
		if (start.getYear() == end.getYear()) {
			return "%d-%02d ~ %02d 평가 결과".formatted(
				start.getYear(),
				start.getMonthValue(),
				end.getMonthValue()
			);
		}
		return "%s ~ %s 평가 결과".formatted(start, end);
	}

	private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
		return value.atZone(KOREA_ZONE_ID).toOffsetDateTime();
	}

	private record ReportCandidate(
		String reportId,
		ReportType type,
		YearMonth yearMonth,
		String title,
		LocalDateTime createdAt,
		String targetScreen,
		Map<String, Object> targetParams
	) {
	}
}
