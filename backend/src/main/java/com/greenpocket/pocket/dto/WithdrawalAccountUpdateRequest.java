package com.greenpocket.pocket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WithdrawalAccountUpdateRequest(
	@NotBlank @Size(max = 10) String bankCode,
	@NotBlank @Size(max = 30) String bankName,
	@NotBlank String accountNo,
	@NotBlank @Size(max = 30) String holder
) {
}
