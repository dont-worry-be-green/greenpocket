package com.greenpocket.profile.dto;

public record PolicyPreferencesUpdateResponse(
	boolean policyProfileCompleted,
	boolean recommendationsUpdated
) {
}
