package com.greenpocket.user.repository;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRegionQueryRepository {

	private final JdbcClient jdbcClient;

	public Optional<String> findSidoCodeByUserId(Long userId) {
		return jdbcClient.sql("""
				SELECT sido_code
				FROM app_user
				WHERE id = :userId
				  AND sido_code IS NOT NULL
				""")
			.param("userId", userId)
			.query(String.class)
			.optional();
	}
}
