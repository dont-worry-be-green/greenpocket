package com.greenpocket.profile.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.profile.dto.PolicyPreferencesRequest;
import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.HouseholdStatus;
import com.greenpocket.user.dto.DemoResetRequest;
import com.greenpocket.user.service.DemoResetService;

@SpringBootTest
@Transactional
class ProfileDemoResetIntegrationTest {

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private ProfileService profileService;

	@Autowired
	private DemoResetService demoResetService;

	@Test
	void savesPolicyProfileAndResetCascadesUserData() {
		String demoKey = UUID.randomUUID().toString();
		Long userId = createUser(demoKey);

		var saved = profileService.updatePolicyPreferences(userId, new PolicyPreferencesRequest(
			CurrentStatus.EMPLOYED, AnnualIncomeBand.FROM_24M_TO_36M, HouseholdStatus.ONE_PERSON
		));
		createBill(userId);

		assertThat(saved.policyProfileCompleted()).isTrue();
		assertThat(profileService.find(userId).birthDate()).isEqualTo(LocalDate.of(1998, 3, 15));

		var reset = demoResetService.reset(new DemoResetRequest(demoKey));

		assertThat(reset.nextScreen()).isEqualTo("ONB-01");
		assertThat(count("app_user", "demo_key", demoKey)).isZero();
		assertThat(count("utility_monthly_record", "user_id", userId)).isZero();

		assertThat(demoResetService.reset(new DemoResetRequest(demoKey)).nextScreen()).isEqualTo("ONB-01");
	}

	private Long createUser(String demoKey) {
		String accountNo = "1005-%04d-%04d-%02d".formatted(
			Math.abs(demoKey.hashCode()) % 10_000,
			Math.abs((demoKey + "profile").hashCode()) % 10_000,
			Math.abs((demoKey + "reset").hashCode()) % 100
		);
		jdbcClient.sql("""
				INSERT INTO app_user (demo_key, name, birth_date, pocket_account_no, pocket_holder)
				VALUES (:demoKey, '김그린', '1998-03-15', :accountNo, '김그린')
				""")
			.param("demoKey", demoKey)
			.param("accountNo", accountNo)
			.update();
		return jdbcClient.sql("SELECT id FROM app_user WHERE demo_key = :demoKey")
			.param("demoKey", demoKey)
			.query(Long.class)
			.single();
	}

	private void createBill(Long userId) {
		jdbcClient.sql("""
				INSERT INTO utility_monthly_record (
				    user_id, record_source, billing_month, utility_type,
				    amount, usage_value, usage_unit, input_source, record_status
				)
				VALUES (
				    :userId, 'BILL', '2026-08-01', 'ELECTRICITY',
				    43200, :usage, 'kWh', 'MANUAL', 'CONFIRMED'
				)
				""")
			.param("userId", userId)
			.param("usage", new BigDecimal("210.000"))
			.update();
	}

	private int count(String table, String column, Object value) {
		return jdbcClient.sql("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = :value")
			.param("value", value)
			.query(Integer.class)
			.single();
	}
}
