package com.greenpocket.bill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.bill.dto.BillUpdateRequest;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.global.type.UtilityType;

@SpringBootTest
@Transactional
class BillArchiveIntegrationTest {

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private BillArchiveService billArchiveService;

	@Test
	void listsDetailsUpdatesAndDeletesOwnedBills() {
		Long userId = createUser();
		Long electricityId = createBill(userId, "2026-08-01", "ELECTRICITY", 43_200L, "210.000", "kWh");
		Long waterId = createBill(userId, "2026-08-01", "WATER", 8_900L, "10.000", "m3");
		createBill(userId, "2025-12-01", "GAS", 12_400L, "14.000", "m3");

		var page = billArchiveService.findBills(userId, UtilityType.ELECTRICITY, 2026, 0, 20);
		assertThat(page.content()).hasSize(1);
		assertThat(page.content().getFirst().recordId()).isEqualTo(electricityId);
		assertThat(page.totalElements()).isEqualTo(1L);
		assertThat(page.counts()).containsEntry("ALL", 2L);
		assertThat(page.counts()).containsEntry("ELECTRICITY", 1L);
		assertThat(page.counts()).containsEntry("WATER", 1L);
		assertThat(page.counts()).containsEntry("GAS", 0L);

		var detail = billArchiveService.findDetail(userId, electricityId);
		assertThat(detail.siblings()).hasSize(1);
		assertThat(detail.siblings().getFirst().recordId()).isEqualTo(waterId);

		var updated = billArchiveService.update(
			userId,
			electricityId,
			new BillUpdateRequest(41_800L, new BigDecimal("203.000"))
		);
		assertThat(updated.amount()).isEqualTo(41_800L);
		assertThat(updated.usage()).isEqualByComparingTo("203.000");
		assertThat(updated.recalculated().monthlyReportUpdated()).isFalse();
		assertThat(readAmount(electricityId)).isEqualTo(41_800L);

		var deleted = billArchiveService.delete(userId, waterId);
		assertThat(deleted.deletedRecordId()).isEqualTo(waterId);
		assertThat(countById(waterId)).isZero();
		assertThatThrownBy(() -> billArchiveService.findDetail(userId, waterId))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.NOT_FOUND));
	}

	private Long createUser() {
		String demoKey = UUID.randomUUID().toString();
		String accountNo = "1005-%04d-%04d-%02d".formatted(
			Math.abs(demoKey.hashCode()) % 10_000,
			Math.abs((demoKey + "archive").hashCode()) % 10_000,
			Math.abs((demoKey + "bill").hashCode()) % 100
		);
		jdbcClient.sql("""
				INSERT INTO app_user (demo_key, name, pocket_account_no, pocket_holder)
				VALUES (:demoKey, '보관함테스트', :accountNo, '보관함테스트')
				""")
			.param("demoKey", demoKey)
			.param("accountNo", accountNo)
			.update();
		return jdbcClient.sql("SELECT id FROM app_user WHERE demo_key = :demoKey")
			.param("demoKey", demoKey)
			.query(Long.class)
			.single();
	}

	private Long createBill(
		Long userId,
		String month,
		String utilityType,
		long amount,
		String usage,
		String unit
	) {
		jdbcClient.sql("""
				INSERT INTO utility_monthly_record (
				    user_id, record_source, billing_month, utility_type,
				    bill_type, amount, usage_value, usage_unit,
				    input_source, record_status
				)
				VALUES (
				    :userId, 'BILL', :month, :utilityType,
				    'MANAGEMENT', :amount, :usage, :unit,
				    'MANUAL', 'CONFIRMED'
				)
				""")
			.param("userId", userId)
			.param("month", month)
			.param("utilityType", utilityType)
			.param("amount", amount)
			.param("usage", new BigDecimal(usage))
			.param("unit", unit)
			.update();
		return jdbcClient.sql("""
				SELECT id
				FROM utility_monthly_record
				WHERE user_id = :userId
				  AND record_source = 'BILL'
				  AND billing_month = :month
				  AND utility_type = :utilityType
				""")
			.param("userId", userId)
			.param("month", month)
			.param("utilityType", utilityType)
			.query(Long.class)
			.single();
	}

	private long readAmount(Long recordId) {
		return jdbcClient.sql("SELECT amount FROM utility_monthly_record WHERE id = :recordId")
			.param("recordId", recordId)
			.query(Long.class)
			.single();
	}

	private int countById(Long recordId) {
		return jdbcClient.sql("SELECT COUNT(*) FROM utility_monthly_record WHERE id = :recordId")
			.param("recordId", recordId)
			.query(Integer.class)
			.single();
	}
}
