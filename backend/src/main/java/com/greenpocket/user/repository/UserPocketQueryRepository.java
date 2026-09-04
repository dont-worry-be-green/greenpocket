package com.greenpocket.user.repository;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserPocketQueryRepository {

	private final JdbcClient jdbcClient;

	public Optional<UserPocketSnapshot> findByUserId(Long userId) {
		return jdbcClient.sql("""
				SELECT pocket_account_no, pocket_holder
				FROM app_user
				WHERE id = :userId
				""")
			.param("userId", userId)
			.query((resultSet, rowNum) -> new UserPocketSnapshot(
				resultSet.getString("pocket_account_no"),
				resultSet.getString("pocket_holder")
			))
			.optional();
	}

	public record UserPocketSnapshot(
		String accountNo,
		String holder
	) {
	}
}
