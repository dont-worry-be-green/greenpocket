package com.greenpocket.eco.dto;

import java.util.List;

public record EcoMissionUpdateRequest(
	List<Long> selectedMissionIds
) {
}
