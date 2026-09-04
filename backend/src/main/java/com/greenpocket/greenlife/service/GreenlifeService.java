package com.greenpocket.greenlife.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.greenlife.dto.GreenlifeItemsResponse;
import com.greenpocket.greenlife.dto.GreenlifeLinkResponse;
import com.greenpocket.greenlife.dto.GreenlifeStatusResponse;
import com.greenpocket.greenlife.repository.GreenlifeRepository;
import com.greenpocket.greenlife.repository.GreenlifeRepository.ItemSnapshot;
import com.greenpocket.greenlife.repository.GreenlifeRepository.MonthlyActivitySnapshot;
import com.greenpocket.greenlife.repository.GreenlifeRepository.PaidMonthSnapshot;
import com.greenpocket.greenlife.repository.GreenlifeRepository.UserSnapshot;

@Service
public class GreenlifeService {

	static final int STANDARD_YEAR = 2026;
	static final long ANNUAL_LIMIT = 70_000L;

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final String PARTICIPATING_SCREEN = "BN-02";
	private static final String NOT_PARTICIPATING_SCREEN = "BN-01";
	private static final String PROGRAM_NAME = "탄소중립포인트 녹색생활실천";
	private static final String PROGRAM_URL = "https://cpoint.or.kr";
	private static final String DELAY_NOTICE = "실적 반영까지 최소 3일~익월 말이 걸릴 수 있어요";
	private static final int COLLAPSED_AFTER = 6;
	private static final List<String> JOIN_STEPS = List.of(
		"공식 누리집에서 회원가입해요",
		"참여기업 앱에서 실천 항목을 설정해요",
		"친환경 활동을 하면 포인트가 쌓여요"
	);

	private final GreenlifeRepository greenlifeRepository;
	private final Clock clock;

	@Autowired
	public GreenlifeService(GreenlifeRepository greenlifeRepository) {
		this(greenlifeRepository, Clock.system(KOREA_ZONE_ID));
	}

	GreenlifeService(GreenlifeRepository greenlifeRepository, Clock clock) {
		this.greenlifeRepository = greenlifeRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public GreenlifeStatusResponse getStatus(Long userId, String monthValue) {
		UserSnapshot user = findUser(userId);
		YearMonth month = parseMonth(monthValue);
		if (!user.participating()) {
			return notParticipatingStatus(user);
		}

		MonthlyActivitySnapshot monthly = greenlifeRepository.findMonthlyActivity(userId, month.atDay(1));
		PaidMonthSnapshot paidMonth = greenlifeRepository
			.findLatestPaidMonth(userId, month.atDay(1), STANDARD_YEAR)
			.orElse(null);
		long annualPaidAmount = greenlifeRepository.findAnnualPaidAmount(userId, STANDARD_YEAR);

		return new GreenlifeStatusResponse(
			true,
			PARTICIPATING_SCREEN,
			toOffsetDateTime(user.linkedAt()),
			null,
			null,
			month.toString(),
			new GreenlifeStatusResponse.MonthSummary(
				monthly.activityCount(),
				monthly.pendingAmount(),
				paidMonth == null ? 0L : paidMonth.paidAmount(),
				paidMonth == null ? null : YearMonth.from(paidMonth.month()).toString()
			),
			new GreenlifeStatusResponse.Annual(
				STANDARD_YEAR,
				annualPaidAmount,
				ANNUAL_LIMIT,
				progressPercent(annualPaidAmount),
				annualPaidAmount >= ANNUAL_LIMIT
			),
			DELAY_NOTICE,
			STANDARD_YEAR
		);
	}

	@Transactional
	public GreenlifeLinkResponse link(Long userId) {
		findUser(userId);
		for (GreenlifeMockData.MockActivity activity : GreenlifeMockData.activities()) {
			greenlifeRepository.upsertMockActivity(
				userId,
				activity.itemCode(),
				"greenlife-demo-u" + userId + "-" + activity.sourceKeySuffix(),
				activity.activityMonth(),
				activity.occurredAt(),
				activity.quantity(),
				activity.rewardAmount(),
				activity.rewardStatus(),
				activity.pendingAt(),
				activity.paidAt(),
				GreenlifeMockData.LINKED_AT
			);
		}
		greenlifeRepository.markParticipating(userId, GreenlifeMockData.LINKED_AT);
		int syncedActivityCount = greenlifeRepository
			.findMonthlyActivity(userId, GreenlifeMockData.SUMMARY_MONTH)
			.activityCount();

		return new GreenlifeLinkResponse(
			true,
			toOffsetDateTime(GreenlifeMockData.LINKED_AT),
			syncedActivityCount,
			PARTICIPATING_SCREEN
		);
	}

	@Transactional(readOnly = true)
	public GreenlifeItemsResponse getItems(Long userId, String monthValue) {
		findUser(userId);
		YearMonth month = parseMonth(monthValue);
		List<GreenlifeItemsResponse.Item> items = greenlifeRepository
			.findItems(userId, month.atDay(1), STANDARD_YEAR)
			.stream()
			.map(this::toItemResponse)
			.toList();

		return new GreenlifeItemsResponse(
			month.toString(),
			STANDARD_YEAR,
			items,
			items.size(),
			COLLAPSED_AFTER
		);
	}

	private GreenlifeStatusResponse notParticipatingStatus(UserSnapshot user) {
		List<GreenlifeStatusResponse.FeaturedItem> featuredItems = greenlifeRepository
			.findFeaturedItems(STANDARD_YEAR)
			.stream()
			.map(item -> new GreenlifeStatusResponse.FeaturedItem(
				item.id(),
				item.name(),
				item.unitPrice(),
				item.rewardUnit(),
				item.iconKey()
			))
			.toList();

		return new GreenlifeStatusResponse(
			false,
			NOT_PARTICIPATING_SCREEN,
			toOffsetDateTime(user.linkedAt()),
			new GreenlifeStatusResponse.ProgramInfo(
				PROGRAM_NAME,
				greenlifeRepository.countActiveItems(STANDARD_YEAR),
				ANNUAL_LIMIT,
				STANDARD_YEAR,
				JOIN_STEPS,
				PROGRAM_URL
			),
			featuredItems,
			null,
			null,
			null,
			null,
			null
		);
	}

	private GreenlifeItemsResponse.Item toItemResponse(ItemSnapshot item) {
		boolean monthlyCapReached = item.monthlyCapAmount() != null
			&& item.monthAmount() >= item.monthlyCapAmount();
		boolean annualCapReached = item.annualCapAmount() != null
			&& item.annualPaidAmount() >= item.annualCapAmount();
		return new GreenlifeItemsResponse.Item(
			item.id(),
			item.itemCode(),
			item.name(),
			item.unitPrice(),
			item.rewardUnit(),
			item.iconKey(),
			item.displayOrder(),
			normalize(item.monthCount()),
			item.monthAmount(),
			item.monthlyCapAmount(),
			item.annualCapAmount(),
			monthlyCapReached || annualCapReached
		);
	}

	private UserSnapshot findUser(Long userId) {
		return greenlifeRepository.findUser(userId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED_DEMO_KEY));
	}

	private YearMonth parseMonth(String value) {
		if (value == null || value.isBlank()) {
			return YearMonth.now(clock);
		}
		try {
			return YearMonth.parse(value);
		} catch (DateTimeParseException exception) {
			throw new BusinessException(
				CommonErrorCode.INVALID_REQUEST,
				"month",
				Map.of("expectedFormat", "YYYY-MM")
			);
		}
	}

	private BigDecimal progressPercent(long paidAmount) {
		BigDecimal calculated = BigDecimal.valueOf(paidAmount)
			.multiply(BigDecimal.valueOf(100))
			.divide(BigDecimal.valueOf(ANNUAL_LIMIT), 1, RoundingMode.HALF_UP);
		return calculated.min(new BigDecimal("100.0"));
	}

	private static BigDecimal normalize(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value.stripTrailingZeros();
	}

	private static OffsetDateTime toOffsetDateTime(java.time.LocalDateTime value) {
		return value == null ? null : value.atZone(KOREA_ZONE_ID).toOffsetDateTime();
	}
}
