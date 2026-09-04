package com.greenpocket.eco.dto;

import com.greenpocket.eco.entity.JobStatus;

public record EcoLinkStartResponse(
	String linkJobId,
	JobStatus status,
	int estimatedSeconds
) {
}
