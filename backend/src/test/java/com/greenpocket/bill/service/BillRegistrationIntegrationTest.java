package com.greenpocket.bill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.bill.dto.BillCreateRequest;
import com.greenpocket.bill.entity.BillType;
import com.greenpocket.bill.entity.InputSource;
import com.greenpocket.bill.entity.RecordStatus;
import com.greenpocket.bill.entity.UsageUnit;
import com.greenpocket.bill.exception.BillErrorCode;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.type.UtilityType;

@SpringBootTest
@Transactional
class BillRegistrationIntegrationTest {

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private BillRegistrationService billRegistrationService;

	@Test
	void savesBillRefreshesMonthlyReportAndRejectsDuplicate() {
		Long userId = createUser();
		Long roundId = createRound(userId);
		createRoundUtility(roundId);
		createBaseline(userId, "2024-08-01", new BigDecimal("218.000"));
		createBaseline(userId, "2025-08-01", new BigDecimal("222.000"));

		BillCreateRequest request = new BillCreateRequest(
			YearMonth.of(2026, 8),
			BillType.ELECTRICITY,
			InputSource.MANUAL,
			List.of(new BillCreateRequest.Item(
				UtilityType.ELECTRICITY,
				43_200L,
				new BigDecimal("210.000"),
				UsageUnit.kWh,
				null
			))
		);

		var response = billRegistrationService.create(userId, request);

		assertThat(response.records()).hasSize(1);
		assertThat(response.records().getFirst().recordId()).isNotNull();
		assertThat(response.records().getFirst().recordStatus()).isEqualTo(RecordStatus.CONFIRMED);
		assertThat(response.recalculated().monthlyReportUpdated()).isTrue();
		assertThat(response.recalculated().roundId()).isEqualTo(roundId);
		assertThat(countBills(userId, "2026-08-01")).isEqualTo(1);
		assertThat(monthlyRate(userId, "2026-08-01")).isEqualByComparingTo("4.545");

		assertThatThrownBy(() -> billRegistrationService.create(userId, request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(BillErrorCode.BILL_DUPLICATED));
		assertThat(countBills(userId, "2026-08-01")).isEqualTo(1);
	}

	private Long createUser() {
		String demoKey = UUID.randomUUID().toString();
		String accountNo = "1005-%04d-%04d-%02d".formatted(
			Math.abs(demoKey.hashCode()) % 10_000,
			Math.abs((demoKey + "bill").hashCode()) % 10_000,
			Math.abs(demoKey.hashCode()) % 100
		);
		jdbcClient.sql("""
				INSERT INTO app_user (demo_key, name, pocket_account_no, pocket_holder)
				VALUES (:demoKey, '고지서테스트', :accountNo, '고지서테스트')
				""")
			.param("demoKey", demoKey)
			.param("accountNo", accountNo)
			.update();
		return jdbcClient.sql("SELECT id FROM app_user WHERE demo_key = :demoKey")
			.param("demoKey", demoKey)
			.query(Long.class)
			.single();
	}

	private Long createRound(Long userId) {
		jdbcClient.sql("""
				INSERT INTO eco_round (
				    user_id, period_start, period_end, round_status,
				    application_status, combined_target_rate, goal_set_at
				)
				VALUES (
				    :userId, '2026-04-01', '2026-09-01', 'GOAL_SET',
				    'APPLIED', 10.000, '2026-04-02 00:00:00'
				)
				""")
			.param("userId", userId)
			.update();
		return jdbcClient.sql("SELECT id FROM eco_round WHERE user_id = :userId")
			.param("userId", userId)
			.query(Long.class)
			.single();
	}

	private void createRoundUtility(Long roundId) {
		jdbcClient.sql("""
				INSERT INTO eco_round_utility (
				    eco_round_id, utility_type, is_registered,
				    carbon_factor_g, target_tier, target_rate
				)
				VALUES (:roundId, 'ELECTRICITY', 1, 424.000, 'TIER_10', 10.000)
				""")
			.param("roundId", roundId)
			.update();
	}

	private void createBaseline(Long userId, String month, BigDecimal usage) {
		jdbcClient.sql("""
				INSERT INTO utility_monthly_record (
				    user_id, record_source, billing_month, utility_type,
				    amount, usage_value, usage_unit, input_source, record_status
				)
				VALUES (
				    :userId, 'ECO_BASELINE', :month, 'ELECTRICITY',
				    40000, :usage, 'kWh', 'ECO_LINK', 'CONFIRMED'
				)
				""")
			.param("userId", userId)
			.param("month", month)
			.param("usage", usage)
			.update();
	}

	private int countBills(Long userId, String month) {
		return jdbcClient.sql("""
				SELECT COUNT(*)
				FROM utility_monthly_record
				WHERE user_id = :userId
				  AND record_source = 'BILL'
				  AND billing_month = :month
				""")
			.param("userId", userId)
			.param("month", month)
			.query(Integer.class)
			.single();
	}

	private BigDecimal monthlyRate(Long userId, String month) {
		return jdbcClient.sql("""
				SELECT monthly_rate
				FROM eco_monthly_report
				WHERE user_id = :userId AND report_month = :month
				""")
			.param("userId", userId)
			.param("month", month)
			.query(BigDecimal.class)
			.single();
	}
}
