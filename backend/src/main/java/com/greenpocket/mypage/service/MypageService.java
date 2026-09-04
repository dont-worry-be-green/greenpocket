package com.greenpocket.mypage.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.bill.service.BillReportQueryService;
import com.greenpocket.eco.dto.EcoStatusResponse;
import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.eco.service.EcoLinkService;
import com.greenpocket.eco.service.EcoReportQueryService;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.mypage.dto.MypageResponse;
import com.greenpocket.user.repository.UserMypageQueryRepository.UserMypageSnapshot;
import com.greenpocket.user.service.UserMypageQueryService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MypageService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
	private static final String BILL_ARCHIVE_SCREEN = "MY-03";
	private static final String REPORT_ARCHIVE_SCREEN = "MY-04";
	private static final String MOVING_NOTICE =
		"이사했다면 꼭 바꿔주세요. 바꾸지 않으면 지금 살지 않는 집의 사용량과 비교돼요";

	private final UserMypageQueryService userMypageQueryService;
	private final BillReportQueryService billReportQueryService;
	private final EcoReportQueryService ecoReportQueryService;
	private final EcoLinkService ecoLinkService;

	public MypageResponse getMypage(Long userId) {
		UserMypageSnapshot user = userMypageQueryService.findMypageUser(userId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED_DEMO_KEY));
		EcoStatusResponse ecoStatus = ecoLinkService.getStatus(userId);
		long reportCount = billReportQueryService.findMonthlyDiagnosisReports(userId).size()
			+ ecoReportQueryService.findMonthlyReports(userId).size()
			+ ecoReportQueryService.findResultReports(userId).size();

		return new MypageResponse(
			new MypageResponse.Profile(
				user.name(),
				user.sidoName(),
				user.sigunguName(),
				user.housingType(),
				user.areaBand(),
				profileSummary(user)
			),
			new MypageResponse.Links(
				new MypageResponse.ArchiveLink(
					billReportQueryService.countBills(userId),
					BILL_ARCHIVE_SCREEN
				),
				new MypageResponse.ArchiveLink(reportCount, REPORT_ARCHIVE_SCREEN)
			),
			ecoAddress(user),
			new MypageResponse.Integration(
				user.ecoLinkStatus(),
				toOffsetDateTime(user.ecoLinkedAt()),
				user.greenlifeParticipating(),
				toOffsetDateTime(user.greenlifeLinkedAt()),
				ecoStatus.registeredUtilities().stream()
					.filter(EcoStatusResponse.RegisteredUtility::registered)
					.map(EcoStatusResponse.RegisteredUtility::utilityType)
					.toList()
			),
			user.pocketAccountNo()
		);
	}

	private MypageResponse.EcoAddress ecoAddress(UserMypageSnapshot user) {
		if (user.ecoLinkStatus() != EcoLinkStatus.LINKED || user.ecoAddressLabel() == null) {
			return null;
		}
		boolean matchesProfile = equals(user.ecoSidoCode(), user.sidoCode())
			&& equals(user.ecoSigunguCode(), user.sigunguCode());
		return new MypageResponse.EcoAddress(
			user.ecoAddressLabel(),
			user.ecoAddressRegisteredAt() == null
				? null
				: YEAR_MONTH_FORMATTER.format(user.ecoAddressRegisteredAt()),
			matchesProfile,
			MOVING_NOTICE
		);
	}

	private String profileSummary(UserMypageSnapshot user) {
		return Stream.of(
			joinNonBlank(shortSidoName(user.sidoName()), user.sigunguName()),
			housingTypeLabel(user.housingType()),
			areaBandLabel(user.areaBand())
		)
			.filter(value -> value != null && !value.isBlank())
			.reduce((left, right) -> left + " · " + right)
			.orElse("");
	}

	private String shortSidoName(String value) {
		return "서울특별시".equals(value) ? "서울" : value;
	}

	private String housingTypeLabel(String value) {
		if (value == null) {
			return null;
		}
		return switch (value) {
			case "ONE_ROOM" -> "원룸";
			case "OFFICETEL" -> "오피스텔";
			case "APARTMENT" -> "아파트";
			case "MULTI_HOUSE" -> "다세대";
			default -> null;
		};
	}

	private String areaBandLabel(String value) {
		if (value == null) {
			return null;
		}
		return switch (value) {
			case "UNDER_10" -> "10평 이하";
			case "FROM_10_TO_20" -> "10~20평";
			case "OVER_20" -> "20평 이상";
			default -> null;
		};
	}

	private String joinNonBlank(String left, String right) {
		if (left == null || left.isBlank()) {
			return right == null ? "" : right;
		}
		return right == null || right.isBlank() ? left : left + " " + right;
	}

	private boolean equals(String left, String right) {
		return left != null && left.equals(right);
	}

	private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
		return value == null ? null : value.atZone(KOREA_ZONE_ID).toOffsetDateTime();
	}
}
