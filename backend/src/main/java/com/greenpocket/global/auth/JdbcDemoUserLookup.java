package com.greenpocket.global.auth;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcDemoUserLookup implements DemoUserLookup {

	private final JdbcClient jdbcClient;

	@Override
	public Optional<Long> findUserIdByDemoKey(String demoKey) {
		return jdbcClient.sql("SELECT id FROM app_user WHERE demo_key = :demoKey")
			.param("demoKey", demoKey)
			.query(Long.class)
			.optional();
	}
}
