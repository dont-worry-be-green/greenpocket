package com.greenpocket.pocket.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.pocket.dto.PocketGreenlifeCreditResult;

@SpringBootTest
@Transactional
class PocketGreenlifeCreditIntegrationTest {

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private PocketGreenlifeCreditService pocketGreenlifeCreditService;

	@Test
	void allowsSameSettlementMonthForDifferentUsersAndDeduplicatesPerUser() {
		Long firstUserId = createUser("첫번째");
		Long secondUserId = createUser("두번째");
		YearMonth yearMonth = YearMonth.of(2026, 7);
		LocalDateTime completedAt = LocalDateTime.of(2026, 8, 10, 0, 0);

		PocketGreenlifeCreditResult first = pocketGreenlifeCreditService.creditGreenlifeMonth(
			firstUserId, yearMonth, 3_140L, completedAt
		);
		PocketGreenlifeCreditResult repeated = pocketGreenlifeCreditService.creditGreenlifeMonth(
			firstUserId, yearMonth, 3_140L, completedAt
		);
		PocketGreenlifeCreditResult second = pocketGreenlifeCreditService.creditGreenlifeMonth(
			secondUserId, yearMonth, 2_000L, completedAt
		);

		assertThat(first.created()).isTrue();
		assertThat(repeated.created()).isFalse();
		assertThat(repeated.transactionId()).isEqualTo(first.transactionId());
		assertThat(second.created()).isTrue();
		assertThat(countGreenlifeMonthTransactions(yearMonth)).isEqualTo(2);
	}

	private Long createUser(String name) {
		String demoKey = UUID.randomUUID().toString();
		String accountNo = "1005-%04d-%04d-%02d".formatted(
			Math.abs(demoKey.hashCode()) % 10_000,
			Math.abs(name.hashCode()) % 10_000,
			name.length()
		);
		jdbcClient.sql("""
				INSERT INTO app_user (demo_key, name, pocket_account_no, pocket_holder)
				VALUES (:demoKey, :name, :accountNo, :name)
				""")
			.param("demoKey", demoKey)
			.param("name", name)
			.param("accountNo", accountNo)
			.update();
		return jdbcClient.sql("SELECT id FROM app_user WHERE demo_key = :demoKey")
			.param("demoKey", demoKey)
			.query(Long.class)
			.single();
	}

	private int countGreenlifeMonthTransactions(YearMonth yearMonth) {
		return jdbcClient.sql("""
				SELECT COUNT(*)
				FROM pocket_transaction
				WHERE source_type = 'GREENLIFE_MONTH' AND source_key = :sourceKey
				""")
			.param("sourceKey", yearMonth.toString())
			.query(Integer.class)
			.single();
	}
}
