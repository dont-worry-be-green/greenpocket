package com.greenpocket.bill.ocr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NaverClovaOcrClient implements ClovaOcrClient {

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

	private final ObjectMapper objectMapper;
	private final String invokeUrl;
	private final String secret;
	private final Long templateId;
	private final HttpClient httpClient;

	@Autowired
	public NaverClovaOcrClient(
		ObjectMapper objectMapper,
		@Value("${clova.ocr.invoke-url:}") String invokeUrl,
		@Value("${clova.ocr.secret:}") String secret,
		@Value("${clova.ocr.template-id:}") String templateId
	) {
		this(objectMapper, invokeUrl, secret, templateId, HttpClient.newBuilder()
			.connectTimeout(CONNECT_TIMEOUT)
			.build());
	}

	NaverClovaOcrClient(
		ObjectMapper objectMapper,
		String invokeUrl,
		String secret,
		String templateId,
		HttpClient httpClient
	) {
		this.objectMapper = objectMapper;
		this.invokeUrl = invokeUrl;
		this.secret = secret;
		this.templateId = parseTemplateId(templateId);
		this.httpClient = httpClient;
	}

	@Override
	public Recognition recognize(byte[] image, String format) {
		validateConfiguration();
		String boundary = "----GreenPocketOcr" + UUID.randomUUID().toString().replace("-", "");
		byte[] body = multipartBody(boundary, image, format);
		HttpRequest request = HttpRequest.newBuilder(URI.create(invokeUrl))
			.timeout(REQUEST_TIMEOUT)
			.header("Content-Type", "multipart/form-data; boundary=" + boundary)
			.header("X-OCR-SECRET", secret)
			.POST(HttpRequest.BodyPublishers.ofByteArray(body))
			.build();

		try {
			HttpResponse<String> response = httpClient.send(
				request,
				HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
			);
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new ClovaOcrClientException(false, "CLOVA OCR returned HTTP " + response.statusCode());
			}
			return parse(response.body());
		}
		catch (HttpTimeoutException exception) {
			throw new ClovaOcrClientException(true, exception);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new ClovaOcrClientException(false, exception);
		}
		catch (IOException exception) {
			throw new ClovaOcrClientException(false, exception);
		}
	}

	Recognition parse(String responseBody) {
		try {
			ClovaResponse response = objectMapper.readValue(responseBody, ClovaResponse.class);
			if (response.images() == null || response.images().isEmpty()) {
				throw new ClovaOcrClientException(false, "CLOVA OCR response has no image result");
			}
			ClovaImage image = response.images().getFirst();
			List<Field> fields = image.fields() == null ? List.of() : image.fields().stream()
				.map(field -> new Field(field.name(), field.inferText(), field.inferConfidence()))
				.toList();
			return new Recognition(image.inferResult(), fields);
		}
		catch (JacksonException exception) {
			throw new ClovaOcrClientException(false, exception);
		}
	}

	private byte[] multipartBody(String boundary, byte[] image, String format) {
		try {
			Map<String, Object> imageMessage = templateId == null
				? Map.of("format", format, "name", "greenpocket-management-bill")
				: Map.of(
					"format", format,
					"name", "greenpocket-management-bill",
					"templateIds", List.of(templateId)
				);
			Map<String, Object> message = Map.of(
				"version", "V2",
				"requestId", UUID.randomUUID().toString(),
				"timestamp", System.currentTimeMillis(),
				"lang", "ko",
				"images", List.of(imageMessage)
			);

			ByteArrayOutputStream output = new ByteArrayOutputStream();
			writeText(output, "--" + boundary + "\r\n");
			writeText(output, "Content-Disposition: form-data; name=\"message\"\r\n");
			writeText(output, "Content-Type: application/json; charset=UTF-8\r\n\r\n");
			output.write(objectMapper.writeValueAsBytes(message));
			writeText(output, "\r\n--" + boundary + "\r\n");
			writeText(output, "Content-Disposition: form-data; name=\"file\"; filename=\"bill."
				+ format + "\"\r\n");
			writeText(output, "Content-Type: image/" + ("jpg".equals(format) ? "jpeg" : "png") + "\r\n\r\n");
			output.write(image);
			writeText(output, "\r\n--" + boundary + "--\r\n");
			return output.toByteArray();
		}
		catch (IOException | JacksonException exception) {
			throw new ClovaOcrClientException(false, exception);
		}
	}

	private void validateConfiguration() {
		if (invokeUrl == null || invokeUrl.isBlank() || secret == null || secret.isBlank()) {
			throw new ClovaOcrClientException(false, "CLOVA OCR configuration is missing");
		}
	}

	private static void writeText(ByteArrayOutputStream output, String value) throws IOException {
		output.write(value.getBytes(StandardCharsets.UTF_8));
	}

	private static Long parseTemplateId(String templateId) {
		if (templateId == null || templateId.isBlank()) {
			return null;
		}
		try {
			return Long.valueOf(templateId);
		}
		catch (NumberFormatException exception) {
			throw new IllegalArgumentException("CLOVA OCR template ID must be a number", exception);
		}
	}

	private record ClovaResponse(List<ClovaImage> images) {
	}

	private record ClovaImage(String inferResult, List<ClovaField> fields) {
	}

	private record ClovaField(String name, String inferText, BigDecimal inferConfidence) {
	}
}
