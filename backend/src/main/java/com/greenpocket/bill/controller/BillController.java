package com.greenpocket.bill.controller;

import java.time.YearMonth;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.greenpocket.bill.dto.BillCreateRequest;
import com.greenpocket.bill.dto.BillCreateResponse;
import com.greenpocket.bill.dto.BillDeleteResponse;
import com.greenpocket.bill.dto.BillDetailResponse;
import com.greenpocket.bill.dto.BillDuplicateCheckResponse;
import com.greenpocket.bill.dto.BillListResponse;
import com.greenpocket.bill.dto.BillOcrResultResponse;
import com.greenpocket.bill.dto.BillOcrStartResponse;
import com.greenpocket.bill.dto.BillTargetMonthResponse;
import com.greenpocket.bill.dto.BillUpdateRequest;
import com.greenpocket.bill.dto.BillUpdateResponse;
import com.greenpocket.bill.service.BillArchiveService;
import com.greenpocket.bill.service.BillOcrService;
import com.greenpocket.bill.service.BillRegistrationService;
import com.greenpocket.global.auth.CurrentUserId;
import com.greenpocket.global.response.ApiResponse;
import com.greenpocket.global.type.UtilityType;

@Tag(name = "Bills", description = "생활요금 고지서 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bills")
public class BillController {

	private final BillRegistrationService billRegistrationService;
	private final BillArchiveService billArchiveService;
	private final BillOcrService billOcrService;

	@Operation(
		summary = "고지서 OCR 요청",
		description = "JPG·PNG 관리비 통합·개별 전기·수도·도시가스 고지서를 CLOVA OCR로 비동기 분석하고 조회할 작업 ID를 반환합니다. 원본 이미지는 분석 후 저장하지 않습니다."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "OCR 작업 접수 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미지 누락 또는 청구 월 힌트 형식 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "이미지 크기 10MB 초과"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "JPG·PNG가 아닌 이미지")
	})
	@PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<BillOcrStartResponse>> startOcr(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "JPG·PNG 고지서 이미지(최대 10MB)", required = true)
		@RequestPart("image") MultipartFile image,
		@Parameter(description = "대상 월 힌트(YYYY-MM)", example = "2026-07")
		@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth billingMonthHint
	) {
		BillOcrStartResponse response = billOcrService.start(userId, image, billingMonthHint);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response));
	}

	@Operation(
		summary = "OCR 진행·결과 조회",
		description = "OCR 작업의 진행 상태를 조회합니다. 관리비 통합 고지서는 전기·수도·가스 3개 항목을 고정 순서로, 개별 고지서는 해당 에너지원 1개 항목을 반환합니다."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OCR 진행·성공·실패 결과 조회"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "OCR 작업 없음 또는 다른 사용자의 작업")
	})
	@GetMapping("/ocr/{jobId}")
	public ApiResponse<BillOcrResultResponse> getOcrResult(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "OCR 요청에서 반환된 작업 ID", example = "ocr_01J8ZK3")
		@PathVariable String jobId
	) {
		return ApiResponse.success(billOcrService.getResult(userId, jobId));
	}

	@Operation(summary = "고지서 보관함 목록 조회", description = "에너지원·연도·페이지 조건으로 고지서를 최신 월 우선 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "고지서 목록 조회 성공 또는 빈 목록"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필터 또는 페이지 값 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping
	public ApiResponse<BillListResponse> findBills(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "에너지원 필터", example = "ELECTRICITY")
		@RequestParam(required = false) UtilityType utility,
		@Parameter(description = "청구 연도", example = "2026")
		@RequestParam(required = false) @Min(1900) @Max(9998) Integer year,
		@Parameter(description = "0부터 시작하는 페이지", example = "0")
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@Parameter(description = "페이지 크기(최대 100)", example = "20")
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return ApiResponse.success(billArchiveService.findBills(userId, utility, year, page, size));
	}

	@Operation(summary = "등록 대상 고지월 조회", description = "현재 KST 기준 직전 월의 등록 상태와 가장 최근 등록 월을 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 대상 월 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping("/target-month")
	public ApiResponse<BillTargetMonthResponse> getTargetMonth(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(billRegistrationService.getTargetMonth(userId));
	}

	@Operation(summary = "고지서 항목 중복 사전 확인", description = "청구 월과 에너지원별 기존 등록 레코드 ID를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "중복 확인 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "청구 월 또는 에너지원 형식 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping("/duplicate-check")
	public ApiResponse<BillDuplicateCheckResponse> checkDuplicates(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "청구 월(YYYY-MM)", example = "2026-08", required = true)
		@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth billingMonth,
		@Parameter(description = "쉼표로 구분한 에너지원", example = "ELECTRICITY,WATER", required = true)
		@RequestParam @NotEmpty List<UtilityType> utilityTypes
	) {
		return ApiResponse.success(
			billRegistrationService.checkDuplicates(userId, billingMonth, utilityTypes)
		);
	}

	@Operation(summary = "고지서 확정 저장", description = "검증된 고지서 항목을 저장하고 진단 및 What-if 월 리포트 갱신 결과를 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "고지서 저장 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 또는 필수 항목 누락"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "같은 월과 에너지원 고지서 중복")
	})
	@PostMapping
	public ResponseEntity<ApiResponse<BillCreateResponse>> create(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Valid @RequestBody BillCreateRequest request
	) {
		BillCreateResponse response = billRegistrationService.create(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}

	@Operation(summary = "고지서 상세 조회", description = "고지서 한 건과 같은 청구 월의 다른 에너지원 항목을 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "고지서 상세 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "고지서 없음 또는 다른 사용자 고지서")
	})
	@GetMapping("/{recordId}")
	public ApiResponse<BillDetailResponse> findDetail(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "고지서 레코드 ID", example = "51")
		@PathVariable @Positive Long recordId
	) {
		return ApiResponse.success(billArchiveService.findDetail(userId, recordId));
	}

	@Operation(summary = "고지서 수정", description = "금액과 사용량을 수정하고 해당 월의 진단·What-if 월 리포트를 갱신합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "고지서 수정 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "금액 또는 사용량 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "고지서 없음 또는 다른 사용자 고지서")
	})
	@PutMapping("/{recordId}")
	public ApiResponse<BillUpdateResponse> update(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "고지서 레코드 ID", example = "51")
		@PathVariable @Positive Long recordId,
		@Valid @RequestBody BillUpdateRequest request
	) {
		return ApiResponse.success(billArchiveService.update(userId, recordId, request));
	}

	@Operation(summary = "고지서 삭제", description = "고지서 한 건을 삭제하고 해당 월의 진단·What-if 월 리포트를 갱신합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "고지서 삭제 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "고지서 없음 또는 다른 사용자 고지서")
	})
	@DeleteMapping("/{recordId}")
	public ApiResponse<BillDeleteResponse> delete(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "고지서 레코드 ID", example = "51")
		@PathVariable @Positive Long recordId
	) {
		return ApiResponse.success(billArchiveService.delete(userId, recordId));
	}
}
