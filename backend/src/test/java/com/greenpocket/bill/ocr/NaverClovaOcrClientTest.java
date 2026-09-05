package com.greenpocket.bill.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import com.greenpocket.bill.entity.BillType;

class NaverClovaOcrClientTest {

	@Test
	void sendsMultipartRequestWithSecretAndAllTemplateIds() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		AtomicReference<String> receivedSecret = new AtomicReference<>();
		AtomicReference<String> receivedContentType = new AtomicReference<>();
		AtomicReference<String> receivedBody = new AtomicReference<>();
		server.createContext("/infer", exchange -> {
			receivedSecret.set(exchange.getRequestHeaders().getFirst("X-OCR-SECRET"));
			receivedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
			receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
			byte[] response = """
				{"images":[{"inferResult":"SUCCESS","matchedTemplate":{"id":43345},"fields":[
				  {"name":"billing_month","inferText":"2026-07","inferConfidence":0.99}
				]}]}
				""".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
			exchange.sendResponseHeaders(200, response.length);
			exchange.getResponseBody().write(response);
			exchange.close();
		});
		server.start();

		try {
			NaverClovaOcrClient client = new NaverClovaOcrClient(
				new ObjectMapper(),
				"http://127.0.0.1:" + server.getAddress().getPort() + "/infer",
				"local-test-secret",
				templateRegistry(),
				HttpClient.newHttpClient()
			);

			var result = client.recognize(
				new byte[] { (byte)0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a },
				"png"
			);

			assertThat(result.inferResult()).isEqualTo("SUCCESS");
			assertThat(result.billType()).isEqualTo(BillType.ELECTRICITY);
			assertThat(receivedSecret.get()).isEqualTo("local-test-secret");
			assertThat(receivedContentType.get()).startsWith("multipart/form-data; boundary=");
			assertThat(receivedBody.get()).contains(
				"name=\"message\"",
				"\"version\":\"V2\"",
				"\"templateIds\":[43341,43345,43347,43348]",
				"name=\"file\"; filename=\"bill.png\""
			);
		}
		finally {
			server.stop(0);
		}
	}

	@Test
	void parsesOnlyRequiredFieldsFromClovaResponse() {
		NaverClovaOcrClient client = new NaverClovaOcrClient(
			new ObjectMapper(), "https://example.com/infer", "secret", templateRegistry(), mock(HttpClient.class)
		);

		var result = client.parse("""
			{
			  "version": "V2",
			  "images": [{
			    "inferResult": "SUCCESS",
			    "message": "SUCCESS",
			    "matchedTemplate": { "id": 43341, "name": "관리비 통합 고지서" },
			    "fields": [
			      { "name": "billing_month", "inferText": "2026년 7월", "inferConfidence": 0.99975 },
			      { "name": "electricity_amount", "inferText": "18,080", "inferConfidence": 0.9998 }
			    ]
			  }]
			}
			""");

		assertThat(result.inferResult()).isEqualTo("SUCCESS");
		assertThat(result.billType()).isEqualTo(BillType.MANAGEMENT);
		assertThat(result.fields()).hasSize(2);
		assertThat(result.fields().getFirst().name()).isEqualTo("billing_month");
		assertThat(result.fields().getFirst().text()).isEqualTo("2026년 7월");
	}

	@Test
	void rejectsMalformedProviderResponse() {
		NaverClovaOcrClient client = new NaverClovaOcrClient(
			new ObjectMapper(), "https://example.com/infer", "secret", templateRegistry(), mock(HttpClient.class)
		);

		assertThatThrownBy(() -> client.parse("not-json"))
			.isInstanceOf(ClovaOcrClientException.class);
	}

	@Test
	void leavesBillTypeEmptyForUnknownOrMissingMatchedTemplate() {
		NaverClovaOcrClient client = new NaverClovaOcrClient(
			new ObjectMapper(), "https://example.com/infer", "secret", templateRegistry(), mock(HttpClient.class)
		);

		var unknown = client.parse("""
			{"images":[{"inferResult":"SUCCESS","matchedTemplate":{"id":99999},"fields":[]}]}
			""");
		var missing = client.parse("""
			{"images":[{"inferResult":"SUCCESS","fields":[]}]}
			""");

		assertThat(unknown.billType()).isNull();
		assertThat(missing.billType()).isNull();
	}

	private static ClovaOcrTemplateRegistry templateRegistry() {
		return new ClovaOcrTemplateRegistry("43341", "43345", "43347", "43348");
	}
}
