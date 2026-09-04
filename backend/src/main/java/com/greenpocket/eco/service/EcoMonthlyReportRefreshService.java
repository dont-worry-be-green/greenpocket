package com.greenpocket.eco.service;

import java.time.YearMonth;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.eco.dto.EcoMonthlyReportResponse;
import com.greenpocket.eco.repository.EcoProgressRepository;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;

@Service
@RequiredArgsConstructor
public class EcoMonthlyReportRefreshService {

	private final EcoProgressRepository ecoProgressRepository;
	private final EcoProgressService ecoProgressService;
	private final ObjectMapper objectMapper;

	@Transactional
	public MonthlyReportRefreshResult refresh(Long userId, YearMonth reportMonth) {
		var round = ecoProgressRepository.findRoundForMonth(userId, reportMonth.atDay(1));
		if (round.isEmpty()) {
			ecoProgressRepository.deleteMonthlyReport(userId, reportMonth.atDay(1));
			return new MonthlyReportRefreshResult(false, null);
		}

		EcoMonthlyReportResponse report = ecoProgressService.getMonthlyReport(userId, reportMonth.toString());
		if (report.result() == null || report.cause() == null || report.prescription() == null) {
			ecoProgressRepository.deleteMonthlyReport(userId, reportMonth.atDay(1));
			return new MonthlyReportRefreshResult(false, round.get().id());
		}

		ecoProgressRepository.upsertMonthlyReport(
			userId,
			round.get().id(),
			reportMonth.atDay(1),
			report.result().monthlyRate(),
			report.result().cumulativeRate(),
			report.result().targetRate(),
			report.prescription().requiredRate(),
			report.prescription().remainingMonths(),
			report.result().achieved(),
			serializeByUtility(report)
		);
		return new MonthlyReportRefreshResult(true, round.get().id());
	}

	private String serializeByUtility(EcoMonthlyReportResponse report) {
		try {
			return objectMapper.writeValueAsString(report.cause().byUtility());
		}
		catch (JacksonException exception) {
			throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
		}
	}

	public record MonthlyReportRefreshResult(boolean updated, Long roundId) {
	}
}
