package com.greenpocket.mypage.controller;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.global.auth.CurrentUserId;
import com.greenpocket.global.response.ApiResponse;
import com.greenpocket.mypage.dto.MypageResponse;
import com.greenpocket.mypage.service.MypageService;

@Tag(name = "Mypage", description = "마이페이지 및 보관함 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MypageController {

	private final MypageService mypageService;

	@Operation(summary = "마이페이지 메인 조회", description = "프로필, 보관함, 제도 연동 및 그린포켓 정보를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "마이페이지 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping("/mypage")
	public ApiResponse<MypageResponse> getMypage(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(mypageService.getMypage(userId));
	}
}
