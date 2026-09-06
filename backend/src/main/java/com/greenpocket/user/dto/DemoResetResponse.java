package com.greenpocket.user.dto;

import java.time.OffsetDateTime;

public record DemoResetResponse(
	OffsetDateTime resetAt,
	String nextScreen
) {
}
