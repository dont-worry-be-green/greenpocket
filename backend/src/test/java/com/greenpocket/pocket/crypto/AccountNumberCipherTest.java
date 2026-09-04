package com.greenpocket.pocket.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class AccountNumberCipherTest {

	private static final String ACCOUNT_NUMBER = "110-123-456789";
	private static final String VALID_KEY = Base64.getEncoder()
		.encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

	private AccountNumberCipher accountNumberCipher;

	@BeforeEach
	void setUp() {
		accountNumberCipher = new AccountNumberCipher(VALID_KEY);
	}

	@Test
	void encryptsAndDecryptsAccountNumber() {
		byte[] encrypted = accountNumberCipher.encrypt(ACCOUNT_NUMBER);

		assertThat(encrypted).isNotEqualTo(ACCOUNT_NUMBER.getBytes(StandardCharsets.UTF_8));
		assertThat(accountNumberCipher.decrypt(encrypted)).isEqualTo(ACCOUNT_NUMBER);
	}

	@Test
	void createsDifferentCiphertextForSameAccountNumber() {
		byte[] first = accountNumberCipher.encrypt(ACCOUNT_NUMBER);
		byte[] second = accountNumberCipher.encrypt(ACCOUNT_NUMBER);

		assertThat(first).isNotEqualTo(second);
		assertThat(accountNumberCipher.decrypt(first)).isEqualTo(ACCOUNT_NUMBER);
		assertThat(accountNumberCipher.decrypt(second)).isEqualTo(ACCOUNT_NUMBER);
	}

	@Test
	void rejectsInvalidEncryptionKey() {
		assertThatThrownBy(() -> new AccountNumberCipher("not-a-valid-key"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("ACCOUNT_NUMBER_ENCRYPTION_KEY_BASE64");
	}

	@Test
	void rejectsTamperedCiphertext() {
		byte[] encrypted = accountNumberCipher.encrypt(ACCOUNT_NUMBER);
		encrypted[encrypted.length - 1] ^= 1;

		assertThatThrownBy(() -> accountNumberCipher.decrypt(encrypted))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("계좌번호 복호화에 실패했습니다.");
	}

	@Test
	void springCreatesCipherBeanFromConfiguredKey() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.getEnvironment().getPropertySources().addFirst(
				new MapPropertySource(
					"account-number-cipher-test",
					Map.of("ACCOUNT_NUMBER_ENCRYPTION_KEY_BASE64", VALID_KEY)
				)
			);
			context.register(AccountNumberCipher.class);
			context.refresh();

			AccountNumberCipher cipher = context.getBean(AccountNumberCipher.class);
			assertThat(cipher.decrypt(cipher.encrypt(ACCOUNT_NUMBER))).isEqualTo(ACCOUNT_NUMBER);
		}
	}
}
