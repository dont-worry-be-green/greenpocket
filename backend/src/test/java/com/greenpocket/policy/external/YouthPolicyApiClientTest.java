package com.greenpocket.policy.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

class YouthPolicyApiClientTest {

	private final YouthPolicyApiClient client = new YouthPolicyApiClient(
		new ObjectMapper(),
		"https://example.test/policies",
		"test-key",
		Duration.ofSeconds(1),
		Duration.ofSeconds(1),
		HttpClient.newHttpClient()
	);

	@Test
	void sendsUserAgentAndPagingParameters() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		AtomicReference<String> receivedAccept = new AtomicReference<>();
		AtomicReference<String> receivedUserAgent = new AtomicReference<>();
		AtomicReference<String> receivedQuery = new AtomicReference<>();
		server.createContext("/policies", exchange -> {
			receivedAccept.set(exchange.getRequestHeaders().getFirst("Accept"));
			receivedUserAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
			receivedQuery.set(exchange.getRequestURI().getRawQuery());
			byte[] response = """
				{"resultCode":200,"result":{"pagging":{"totCount":0,"pageNum":2,"pageSize":1},"youthPolicyList":[]}}
				""".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
			exchange.sendResponseHeaders(200, response.length);
			exchange.getResponseBody().write(response);
			exchange.close();
		});
		server.start();

		try {
			YouthPolicyApiClient httpClient = new YouthPolicyApiClient(
				new ObjectMapper(),
				"http://127.0.0.1:" + server.getAddress().getPort() + "/policies",
				"test-key",
				Duration.ofSeconds(1),
				Duration.ofSeconds(1),
				HttpClient.newHttpClient()
			);

			YouthPolicyPage page = httpClient.fetchPage(2, 1);

			assertThat(page.totalCount()).isZero();
			assertThat(receivedAccept.get()).isEqualTo("application/json");
			assertThat(receivedUserAgent.get()).isEqualTo("Mozilla/5.0");
			assertThat(receivedQuery.get()).isEqualTo(
				"apiKeyNm=test-key&pageNum=2&pageSize=1&rtnType=json"
			);
		}
		finally {
			server.stop(0);
		}
	}

	@Test
	void parsesOfficialResponseAndNormalizesBlankNumbers() {
		YouthPolicyPage page = client.parse("""
			{
			  "resultCode": 200,
			  "result": {
			    "pagging": {"totCount": 1, "pageNum": 1, "pageSize": 100},
			    "youthPolicyList": [{
			      "plcyNo": "20260908005400213380",
			      "plcyNm": "청년 교육 지원",
			      "lclsfNm": "교육･직업훈련",
			      "mclsfNm": "미래역량강화",
			      "bizPrdBgngYmd": "20260101",
			      "bizPrdEndYmd": "20270228",
			      "sprtTrgtMinAge": "19",
			      "sprtTrgtMaxAge": "39",
			      "earnMinAmt": "0",
			      "earnMaxAmt": " ",
			      "zipCd": "11620",
			      "frstRegDt": "2026-09-08 09:13:11"
			    }]
			  }
			}
			""");

		assertThat(page.totalCount()).isEqualTo(1);
		assertThat(page.policies()).hasSize(1);
		YouthPolicySourcePolicy policy = page.policies().getFirst();
		assertThat(policy.externalPolicyId()).isEqualTo("20260908005400213380");
		assertThat(policy.businessStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
		assertThat(policy.minAge()).isEqualTo(19);
		assertThat(policy.incomeMinAmount()).isNull();
	}

	@Test
	void rejectsErrorOrMalformedResponseWithoutExposingBody() {
		assertThatThrownBy(() -> client.parse("{\"errorCode\":\"e001\",\"errorMsg\":\"invalid api key\"}"))
			.isInstanceOf(YouthPolicyClientException.class)
			.hasMessageNotContaining("invalid api key");

		assertThatThrownBy(() -> client.parse("{"))
			.isInstanceOf(YouthPolicyClientException.class);
	}
}
