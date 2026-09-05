package com.greenpocket.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.greenpocket.profile.entity.AreaBand;
import com.greenpocket.profile.entity.HousingType;

public record ProfileUpdateRequest(
	@Schema(example = "김수현") String name,
	@Schema(example = "11") String sidoCode,
	@Schema(example = "서울특별시") String sidoName,
	@Schema(example = "11620") String sigunguCode,
	@Schema(example = "관악구") String sigunguName,
	@Schema(example = "ONE_ROOM") HousingType housingType,
	@Schema(example = "UNDER_10") AreaBand areaBand,
	@Schema(example = "true") Boolean confirmBaselineChange
) {
}
