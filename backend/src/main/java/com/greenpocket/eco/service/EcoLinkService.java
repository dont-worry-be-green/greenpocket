package com.greenpocket.eco.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.eco.dto.EcoLinkProgressResponse;
import com.greenpocket.eco.dto.EcoLinkStartResponse;
import com.greenpocket.eco.dto.EcoStatusResponse;
import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.eco.entity.JobStatus;
import com.greenpocket.eco.exception.EcoErrorCode;
import com.greenpocket.eco.repository.EcoRepository;
import com.greenpocket.eco.repository.EcoRepository.EcoRoundSnapshot;
import com.greenpocket.eco.repository.EcoRepository.EcoUserSnapshot;
import com.greenpocket.eco.repository.EcoRepository.EcoUtilitySnapshot;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.global.type.UtilityType;

@Service
@RequiredArgsConstructor
public class EcoLinkService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final String SEOUL_CODE = "11";
	private static final String NOT_SEOUL = "NOT_SEOUL";
	private static final String EXTERNAL_URL = "https://ecomileage.seoul.go.kr";
	private static final int ESTIMATED_SECONDS = 20;
	private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

	private final EcoRepository ecoRepository;
	private final Map<String, MockLinkJob> linkJobs = new ConcurrentHashMap<>();

	@Transactional(readOnly = true)
	public EcoStatusResponse getStatus(Long userId) {
		EcoUserSnapshot user = findUser(userId);
		Optional<EcoRoundSnapshot> round = ecoRepository.findCurrentRound(userId);
		List<EcoUtilitySnapshot> savedUtilities = round
			.map(value -> ecoRepository.findUtilities(value.id()))
			.orElseGet(List::of);

		List<EcoStatusResponse.RegisteredUtility> utilities = List.of(UtilityType.values()).stream()
			.map(utilityType -> registeredUtility(utilityType, savedUtilities))
			.toList();
		long registeredCount = utilities.stream().filter(EcoStatusResponse.RegisteredUtility::registered).count();
		boolean electricityRegistered = utilities.stream()
			.anyMatch(value -> value.utilityType() == UtilityType.ELECTRICITY && value.registered());
		boolean seoulResident = user.isSeoulResident();

		return new EcoStatusResponse(
			user.linkStatus(),
			toOffsetDateTime(user.linkedAt()),
			seoulResident,
			seoulResident && (user.linkStatus() == EcoLinkStatus.UNLINKED || user.linkStatus() == EcoLinkStatus.FAILED),
			seoulResident ? null : NOT_SEOUL,
			utilities,
			electricityRegistered && registeredCount >= 2,
			statusAddress(user),
			EXTERNAL_URL
		);
	}

	@Transactional
	public EcoLinkStartResponse startLink(Long userId) {
		EcoUserSnapshot user = findUser(userId);
		if (!user.isSeoulResident()) {
			throw new BusinessException(EcoErrorCode.ECO_NOT_SEOUL);
		}

		String jobId = "eco_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		linkJobs.put(jobId, new MockLinkJob(userId));
		ecoRepository.markLinking(userId);
		return new EcoLinkStartResponse(jobId, JobStatus.RUNNING, ESTIMATED_SECONDS);
	}

	@Transactional
	public EcoLinkProgressResponse getLinkProgress(Long userId, String linkJobId) {
		MockLinkJob job = linkJobs.get(linkJobId);
		if (job == null || !job.userId().equals(userId)) {
			throw new BusinessException(EcoErrorCode.ECO_LINK_FAILED);
		}

		synchronized (job) {
			if (job.completedResponse() != null) {
				return job.completedResponse();
			}
			if (job.pollCount() == 0) {
				job.incrementPollCount();
				return EcoLinkProgressResponse.running(linkJobId);
			}

			EcoLinkProgressResponse completed = completeMockLink(userId, linkJobId);
			job.complete(completed);
			return completed;
		}
	}

	private EcoLinkProgressResponse completeMockLink(Long userId, String linkJobId) {
		EcoUserSnapshot user = findUser(userId);
		Long roundId = ecoRepository.upsertMockRound(
			userId,
			EcoMockData.PERIOD_START,
			EcoMockData.PERIOD_END,
			EcoMockData.TOTAL_AMOUNT,
			EcoMockData.TOTAL_CARBON_G,
			EcoMockData.LINKED_AT
		);

		for (EcoMockData.UtilityBaseline utility : EcoMockData.UTILITIES) {
			ecoRepository.upsertMockUtility(
				roundId,
				utility.utilityType(),
				utility.carbonFactorG(),
				utility.amount(),
				utility.usage(),
				utility.shareRate()
			);
			insertBaselineMonths(userId, utility);
		}

		String addressLabel = user.profileAddressLabel();
		ecoRepository.markLinked(
			userId,
			EcoMockData.LINKED_AT,
			user.sidoCode(),
			user.sigunguCode(),
			addressLabel,
			EcoMockData.ADDRESS_REGISTERED_AT
		);

		return EcoLinkProgressResponse.succeeded(
			linkJobId,
			toOffsetDateTime(EcoMockData.LINKED_AT),
			roundId,
			new EcoLinkProgressResponse.EcoAddress(
				addressLabel,
				user.sidoCode(),
				user.sigunguCode(),
				EcoMockData.ADDRESS_REGISTERED_AT.format(YEAR_MONTH_FORMATTER)
			)
		);
	}

	private void insertBaselineMonths(Long userId, EcoMockData.UtilityBaseline utility) {
		LocalDate firstMonth = LocalDate.of(2024, 4, 1);
		for (int index = 0; index < 24; index++) {
			ecoRepository.upsertMockBaselineRecord(
				userId,
				firstMonth.plusMonths(index),
				utility.utilityType(),
				utility.monthlyAmount(index),
				utility.monthlyUsage(index),
				utility.usageUnit()
			);
		}
	}

	private EcoStatusResponse.RegisteredUtility registeredUtility(
		UtilityType utilityType,
		List<EcoUtilitySnapshot> savedUtilities
	) {
		return savedUtilities.stream()
			.filter(value -> value.utilityType() == utilityType)
			.findFirst()
			.map(value -> new EcoStatusResponse.RegisteredUtility(
				value.utilityType(),
				value.registered(),
				value.unregisteredReason()
			))
			.orElseGet(() -> new EcoStatusResponse.RegisteredUtility(utilityType, false, null));
	}

	private EcoStatusResponse.EcoAddress statusAddress(EcoUserSnapshot user) {
		if (user.ecoAddressLabel() == null || user.ecoAddressRegisteredAt() == null) {
			return null;
		}
		return new EcoStatusResponse.EcoAddress(
			user.ecoAddressLabel(),
			user.ecoSidoCode(),
			user.ecoSigunguCode(),
			user.ecoAddressRegisteredAt().format(YEAR_MONTH_FORMATTER),
			java.util.Objects.equals(user.sidoCode(), user.ecoSidoCode())
				&& java.util.Objects.equals(user.sigunguCode(), user.ecoSigunguCode())
		);
	}

	private EcoUserSnapshot findUser(Long userId) {
		return ecoRepository.findUser(userId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED_DEMO_KEY));
	}

	private static OffsetDateTime toOffsetDateTime(java.time.LocalDateTime value) {
		return value == null ? null : value.atZone(KOREA_ZONE_ID).toOffsetDateTime();
	}

	private static final class MockLinkJob {

		private final Long userId;
		private int pollCount;
		private EcoLinkProgressResponse completedResponse;

		private MockLinkJob(Long userId) {
			this.userId = userId;
		}

		Long userId() {
			return userId;
		}

		int pollCount() {
			return pollCount;
		}

		void incrementPollCount() {
			pollCount++;
		}

		EcoLinkProgressResponse completedResponse() {
			return completedResponse;
		}

		void complete(EcoLinkProgressResponse response) {
			completedResponse = response;
		}
	}
}
