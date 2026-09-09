package com.greenpocket.policy.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.greenpocket.policy.external.YouthPolicyApiClient;
import com.greenpocket.policy.external.YouthPolicyClientException;
import com.greenpocket.policy.external.YouthPolicyPage;
import com.greenpocket.policy.external.YouthPolicySourcePolicy;
import com.greenpocket.policy.repository.YouthPolicySyncRepository;

@Service
@RequiredArgsConstructor
public class YouthPolicySyncService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

	private final YouthPolicyApiClient youthPolicyApiClient;
	private final YouthPolicyPersistenceService youthPolicyPersistenceService;
	private final YouthPolicySyncRepository youthPolicySyncRepository;
	private final AtomicBoolean syncing = new AtomicBoolean(false);

	@Value("${greenpocket.youth-policy.sync-enabled:false}")
	private boolean syncEnabled;

	@Value("${greenpocket.youth-policy.sync-on-startup:true}")
	private boolean syncOnStartup;

	@Value("${greenpocket.youth-policy.page-size:100}")
	private int pageSize;

	@Value("${greenpocket.youth-policy.retry-attempts:3}")
	private int retryAttempts;

	@Value("${greenpocket.youth-policy.retry-delay-millis:500}")
	private long retryDelayMillis;

	@EventListener(ApplicationReadyEvent.class)
	public void syncOnStartup() {
		youthPolicyPersistenceService.ensureCuratedCache();
		if (syncEnabled && syncOnStartup) {
			sync();
		}
	}

	@Scheduled(cron = "${greenpocket.youth-policy.sync-cron:0 0 3 * * *}", zone = "Asia/Seoul")
	public void scheduledSync() {
		if (syncEnabled) {
			sync();
		}
	}

	public void sync() {
		if (!syncing.compareAndSet(false, true)) {
			return;
		}
		LocalDateTime startedAt = now();
		Long syncId = youthPolicySyncRepository.start(startedAt);
		try {
			FetchedPolicies fetched = fetchAll();
			LocalDateTime syncedAt = now();
			int upsertedCount = youthPolicyPersistenceService.replaceCache(fetched.policies(), syncedAt);
			youthPolicySyncRepository.succeed(
				syncId, now(), fetched.totalCount(), fetched.policies().size(), upsertedCount, fetched.lastPage()
			);
		}
		catch (RuntimeException exception) {
			youthPolicySyncRepository.fail(
				syncId,
				now(),
				exception.getClass().getSimpleName(),
				"외부 API 요청 또는 정책 캐시 저장에 실패했습니다."
			);
		}
		finally {
			syncing.set(false);
		}
	}

	FetchedPolicies fetchAll() {
		YouthPolicyPage firstPage = fetchPageWithRetry(1);
		int totalPages = Math.max(1, (firstPage.totalCount() + pageSize - 1) / pageSize);
		List<YouthPolicySourcePolicy> policies = new ArrayList<>(firstPage.policies());
		for (int page = 2; page <= totalPages; page++) {
			policies.addAll(fetchPageWithRetry(page).policies());
		}
		if (policies.size() != firstPage.totalCount()) {
			throw new IllegalStateException("온통청년 API 전체 건수와 수집 건수가 다릅니다.");
		}
		return new FetchedPolicies(firstPage.totalCount(), totalPages, List.copyOf(policies));
	}

	private YouthPolicyPage fetchPageWithRetry(int pageNumber) {
		for (int attempt = 1; attempt <= Math.max(1, retryAttempts); attempt++) {
			try {
				return youthPolicyApiClient.fetchPage(pageNumber, pageSize);
			}
			catch (YouthPolicyClientException ignored) {
				if (attempt < Math.max(1, retryAttempts)) {
					waitBeforeRetry(attempt);
				}
			}
		}
		throw new YouthPolicyClientException(
			"온통청년 API " + pageNumber + "페이지 요청이 반복해서 실패했습니다."
		);
	}

	private void waitBeforeRetry(int attempt) {
		try {
			Thread.sleep(Math.max(0, retryDelayMillis) * attempt);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new YouthPolicyClientException("온통청년 API 재시도 대기가 중단됐습니다.");
		}
	}

	private static LocalDateTime now() {
		return LocalDateTime.now(KOREA_ZONE_ID);
	}

	record FetchedPolicies(
		int totalCount,
		int lastPage,
		List<YouthPolicySourcePolicy> policies
	) {
	}
}
