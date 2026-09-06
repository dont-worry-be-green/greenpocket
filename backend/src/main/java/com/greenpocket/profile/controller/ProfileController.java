package com.greenpocket.profile.controller;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.global.auth.CurrentUserId;
import com.greenpocket.global.response.ApiResponse;
import com.greenpocket.profile.dto.ProfileResponse;
import com.greenpocket.profile.dto.ProfileSaveRequest;
import com.greenpocket.profile.dto.ProfileSaveResponse;
import com.greenpocket.profile.dto.ProfileUpdateRequest;
import com.greenpocket.profile.dto.ProfileUpdateResponse;
import com.greenpocket.profile.service.ProfileService;

@Tag(name = "Profiles", description = "서울 거주 프로필 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/profile")
public class ProfileController {

	private final ProfileService profileService;

	@Operation(summary = "프로필 저장", description = "서울 자치구·주거 형태·평수 구간을 저장하고 온보딩을 완료합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 저장 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "서울 자치구 코드 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "필수 프로필 값 누락")
	})
	@PostMapping
	public ApiResponse<ProfileSaveResponse> save(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@RequestBody ProfileSaveRequest request
	) {
		return ApiResponse.success(profileService.save(userId, request));
	}

	@Operation(summary = "프로필 조회", description = "현재 사용자의 서울 거주 프로필과 공통 요약 문자열을 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "프로필 미완성")
	})
	@GetMapping
	public ApiResponse<ProfileResponse> find(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(profileService.find(userId));
	}

	@Operation(summary = "프로필 수정", description = "이름과 서울 거주 프로필을 수정합니다. 진행 중 평가에서 지역을 바꾸면 확인 동의가 필요합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 수정 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이름 입력 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "서울 자치구 코드 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "필수값 누락 또는 진행 중 평가의 지역 변경 미동의")
	})
	@PutMapping
	public ApiResponse<ProfileUpdateResponse> update(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@RequestBody ProfileUpdateRequest request
	) {
		return ApiResponse.success(profileService.update(userId, request));
	}
}
