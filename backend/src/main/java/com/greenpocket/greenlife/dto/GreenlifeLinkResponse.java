package com.greenpocket.greenlife.dto;

import java.time.OffsetDateTime;

public record GreenlifeLinkResponse(
	boolean participating,
	OffsetDateTime linkedAt,
	int syncedActivityCount,
	String screen
) {
}
