package com.greenpocket.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.greenpocket.profile.entity.AreaBand;
import com.greenpocket.profile.entity.HousingType;

public record ProfileSaveRequest(
	@Schema(example = "11") String sidoCode,
	@Schema(example = "서울특별시") String sidoName,
	@Schema(example = "11620") String sigunguCode,
	@Schema(example = "관악구") String sigunguName,
	@Schema(example = "APARTMENT") HousingType housingType,
	@Schema(example = "OVER_20") AreaBand areaBand
) {
}
