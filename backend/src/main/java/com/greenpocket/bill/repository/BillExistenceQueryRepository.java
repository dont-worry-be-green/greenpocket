package com.greenpocket.bill.repository;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BillExistenceQueryRepository {

	private final JdbcClient jdbcClient;

	public boolean existsByUserId(Long userId) {
		return jdbcClient.sql("""
				SELECT EXISTS(
				    SELECT 1
				    FROM utility_monthly_record
				    WHERE user_id = :userId AND record_source = 'BILL'
				)
				""")
			.param("userId", userId)
			.query(Boolean.class)
			.single();
	}
}
