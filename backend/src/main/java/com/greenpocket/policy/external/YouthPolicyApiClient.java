package com.greenpocket.policy.external;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class YouthPolicyApiClient {

	private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(20);
	private static final String USER_AGENT = "Mozilla/5.0";
	private static final DateTimeFormatter SOURCE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final ObjectMapper objectMapper;
	private final String endpoint;
	private final String apiKey;
	private final Duration requestTimeout;
	private final HttpClient httpClient;

	@Autowired
	public YouthPolicyApiClient(
		ObjectMapper objectMapper,
		@Value("${greenpocket.youth-policy.endpoint}") String endpoint,
		@Value("${greenpocket.youth-policy.api-key:}") String apiKey,
		@Value("${greenpocket.youth-policy.connect-timeout-seconds:5}") long connectTimeoutSeconds,
		@Value("${greenpocket.youth-policy.request-timeout-seconds:20}") long requestTimeoutSeconds
	) {
		this(
			objectMapper,
			endpoint,
			apiKey,
			Duration.ofSeconds(connectTimeoutSeconds),
			Duration.ofSeconds(requestTimeoutSeconds),
			HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
				.build()
		);
	}

	YouthPolicyApiClient(
		ObjectMapper objectMapper,
		String endpoint,
		String apiKey,
		Duration connectTimeout,
		Duration requestTimeout,
		HttpClient httpClient
	) {
		this.objectMapper = objectMapper;
		this.endpoint = endpoint;
		this.apiKey = apiKey;
		this.requestTimeout = requestTimeout;
		this.httpClient = httpClient;
	}

	public YouthPolicyPage fetchPage(int pageNumber, int pageSize) {
		validateConfiguration();
		HttpRequest request = HttpRequest.newBuilder(buildUri(pageNumber, pageSize))
			.timeout(requestTimeout)
			.header("Accept", "application/json")
			.header("User-Agent", USER_AGENT)
			.GET()
			.build();
		try {
			HttpResponse<String> response = httpClient.send(
				request,
				HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
			);
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new YouthPolicyClientException("온통청년 API HTTP " + response.statusCode());
			}
			return parse(response.body());
		}
		catch (HttpTimeoutException exception) {
			throw new YouthPolicyClientException("온통청년 API 요청 시간이 초과됐습니다.");
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new YouthPolicyClientException(exception);
		}
		catch (IOException exception) {
			throw new YouthPolicyClientException(exception);
		}
	}

	YouthPolicyPage parse(String responseBody) {
		try {
			JsonNode root = objectMapper.readTree(responseBody);
			if (root.path("resultCode").asInt(0) != 200) {
				throw new YouthPolicyClientException("온통청년 API가 성공 응답을 반환하지 않았습니다.");
			}
			JsonNode result = root.path("result");
			JsonNode paging = result.path("pagging");
			JsonNode list = result.path("youthPolicyList");
			if (!list.isArray()) {
				throw new YouthPolicyClientException("온통청년 API 응답 형식이 올바르지 않습니다.");
			}

			List<YouthPolicySourcePolicy> policies = new ArrayList<>();
			for (JsonNode item : list) {
				YouthPolicySourcePolicy policy = toPolicy(item);
				if (policy.externalPolicyId() != null && policy.title() != null) {
					policies.add(policy);
				}
			}
			return new YouthPolicyPage(
				paging.path("totCount").asInt(),
				paging.path("pageNum").asInt(),
				paging.path("pageSize").asInt(),
				List.copyOf(policies)
			);
		}
		catch (JacksonException exception) {
			throw new YouthPolicyClientException(exception);
		}
	}

	private YouthPolicySourcePolicy toPolicy(JsonNode item) {
		return new YouthPolicySourcePolicy(
			text(item, "plcyNo"),
			text(item, "plcyNm"),
			text(item, "plcyKywdNm"),
			text(item, "plcyExplnCn"),
			text(item, "lclsfNm"),
			text(item, "mclsfNm"),
			text(item, "plcySprtCn"),
			text(item, "sprvsnInstCdNm"),
			text(item, "operInstCdNm"),
			text(item, "plcyAprvSttsCd"),
			text(item, "plcyPvsnMthdCd"),
			text(item, "aplyPrdSeCd"),
			date(text(item, "bizPrdBgngYmd")),
			date(text(item, "bizPrdEndYmd")),
			text(item, "aplyYmd"),
			text(item, "plcyAplyMthdCn"),
			text(item, "aplyUrlAddr"),
			text(item, "refUrlAddr1"),
			text(item, "refUrlAddr2"),
			text(item, "sprtTrgtAgeLmtYn"),
			age(text(item, "sprtTrgtMinAge")),
			age(text(item, "sprtTrgtMaxAge")),
			text(item, "mrgSttsCd"),
			text(item, "earnCndSeCd"),
			positiveLong(text(item, "earnMinAmt")),
			positiveLong(text(item, "earnMaxAmt")),
			text(item, "earnEtcCn"),
			text(item, "addAplyQlfcCndCn"),
			text(item, "ptcpPrpTrgtCn"),
			text(item, "zipCd"),
			text(item, "plcyMajorCd"),
			text(item, "jobCd"),
			text(item, "schoolCd"),
			text(item, "sbizCd"),
			dateTime(text(item, "frstRegDt")),
			dateTime(text(item, "lastMdfcnDt"))
		);
	}

	private URI buildUri(int pageNumber, int pageSize) {
		String separator = endpoint.contains("?") ? "&" : "?";
		return URI.create(endpoint + separator
			+ "apiKeyNm=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
			+ "&pageNum=" + pageNumber
			+ "&pageSize=" + pageSize
			+ "&rtnType=json");
	}

	private void validateConfiguration() {
		if (endpoint == null || endpoint.isBlank() || apiKey == null || apiKey.isBlank()) {
			throw new YouthPolicyClientException("온통청년 API 설정이 없습니다.");
		}
	}

	private static String text(JsonNode node, String field) {
		String value = node.path(field).asText("").strip();
		return value.isEmpty() ? null : value;
	}

	private static LocalDate date(String value) {
		if (value == null) {
			return null;
		}
		try {
			return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
		}
		catch (DateTimeParseException exception) {
			return null;
		}
	}

	private static LocalDateTime dateTime(String value) {
		if (value == null) {
			return null;
		}
		try {
			return LocalDateTime.parse(value, SOURCE_DATE_TIME);
		}
		catch (DateTimeParseException exception) {
			return null;
		}
	}

	private static Integer age(String value) {
		Long number = positiveLong(value);
		return number == null || number > 100 ? null : number.intValue();
	}

	private static Long positiveLong(String value) {
		if (value == null) {
			return null;
		}
		try {
			long number = Long.parseLong(value);
			return number > 0 ? number : null;
		}
		catch (NumberFormatException exception) {
			return null;
		}
	}
}
