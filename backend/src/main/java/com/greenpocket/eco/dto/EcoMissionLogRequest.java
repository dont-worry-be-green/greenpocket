package com.greenpocket.eco.dto;

import java.util.List;

public record EcoMissionLogRequest(
	List<Long> completedMissionIds
) {
}
