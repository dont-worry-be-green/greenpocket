package com.greenpocket.pocket.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AccountNumberCipher {

	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final String ALGORITHM = "AES";
	private static final int KEY_LENGTH_BYTES = 32;
	private static final int IV_LENGTH_BYTES = 12;
	private static final int TAG_LENGTH_BITS = 128;

	private final SecretKeySpec key;
	private final SecureRandom secureRandom;

	@Autowired
	public AccountNumberCipher(
		@Value("${ACCOUNT_NUMBER_ENCRYPTION_KEY_BASE64}") String encodedKey
	) {
		this(encodedKey, new SecureRandom());
	}

	AccountNumberCipher(String encodedKey, SecureRandom secureRandom) {
		this.key = new SecretKeySpec(decodeKey(encodedKey), ALGORITHM);
		this.secureRandom = secureRandom;
	}

	public byte[] encrypt(String accountNumber) {
		byte[] iv = new byte[IV_LENGTH_BYTES];
		secureRandom.nextBytes(iv);

		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			byte[] encrypted = cipher.doFinal(accountNumber.getBytes(StandardCharsets.UTF_8));
			return ByteBuffer.allocate(iv.length + encrypted.length)
				.put(iv)
				.put(encrypted)
				.array();
		}
		catch (GeneralSecurityException exception) {
			throw new IllegalStateException("계좌번호 암호화에 실패했습니다.", exception);
		}
	}

	public String decrypt(byte[] encryptedAccountNumber) {
		if (encryptedAccountNumber == null || encryptedAccountNumber.length <= IV_LENGTH_BYTES) {
			throw new IllegalStateException("암호화된 계좌번호 형식이 올바르지 않습니다.");
		}

		byte[] iv = Arrays.copyOfRange(encryptedAccountNumber, 0, IV_LENGTH_BYTES);
		byte[] encrypted = Arrays.copyOfRange(
			encryptedAccountNumber,
			IV_LENGTH_BYTES,
			encryptedAccountNumber.length
		);

		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		}
		catch (GeneralSecurityException exception) {
			throw new IllegalStateException("계좌번호 복호화에 실패했습니다.", exception);
		}
	}

	private static byte[] decodeKey(String encodedKey) {
		try {
			byte[] decoded = Base64.getDecoder().decode(encodedKey);
			if (decoded.length != KEY_LENGTH_BYTES) {
				throw new IllegalArgumentException("계좌번호 암호화 키는 32바이트여야 합니다.");
			}
			return decoded;
		}
		catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException(
				"ACCOUNT_NUMBER_ENCRYPTION_KEY_BASE64에 Base64 형식의 32바이트 키가 필요합니다.",
				exception
			);
		}
	}
}
