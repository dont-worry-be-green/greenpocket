package com.greenpocket.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.greenpocket.user.repository.UserPocketQueryRepository;
import com.greenpocket.user.repository.UserPocketQueryRepository.UserPocketSnapshot;

class UserPocketQueryServiceTest {

	@Test
	void returnsPocketOwnedByUser() {
		UserPocketQueryRepository repository = mock(UserPocketQueryRepository.class);
		when(repository.findByUserId(42L))
			.thenReturn(Optional.of(new UserPocketSnapshot("1005-1234-5678-90", "김수현")));

		Optional<UserPocketSnapshot> result = new UserPocketQueryService(repository).findPocket(42L);

		assertThat(result).hasValueSatisfying(pocket -> {
			assertThat(pocket.accountNo()).isEqualTo("1005-1234-5678-90");
			assertThat(pocket.holder()).isEqualTo("김수현");
		});
	}
}
