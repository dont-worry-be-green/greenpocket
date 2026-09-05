package com.greenpocket.bill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.greenpocket.bill.dto.BillOcrJobStatus;
import com.greenpocket.bill.entity.BillType;
import com.greenpocket.bill.entity.RecordStatus;
import com.greenpocket.bill.exception.BillErrorCode;
import com.greenpocket.bill.ocr.ClovaOcrClient;
import com.greenpocket.bill.ocr.ClovaOcrClient.Field;
import com.greenpocket.bill.ocr.ClovaOcrClient.Recognition;
import com.greenpocket.bill.ocr.ClovaOcrClientException;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.type.UtilityType;

class BillOcrServiceTest {

	private static final Long USER_ID = 42L;

	@Test
	void normalizesClovaManagementBillResult() {
		BillOcrService service = service((image, format) -> successFields());

		var started = service.start(USER_ID, png(), null);
		var result = service.getResult(USER_ID, started.jobId());

		assertThat(started.status()).isEqualTo(BillOcrJobStatus.PENDING);
		assertThat(started.progress()).isZero();
		assertThat(result.status()).isEqualTo(BillOcrJobStatus.SUCCEEDED);
		assertThat(result.progress()).isEqualTo(100);
		assertThat(result.billType()).isEqualTo(BillType.MANAGEMENT);
		assertThat(result.billingMonth()).isEqualTo("2026-07");
		assertThat(result.partialRecognition()).isFalse();
		assertThat(result.items()).hasSize(3);

		var electricity = result.items().getFirst();
		assertThat(electricity.utilityType()).isEqualTo(UtilityType.ELECTRICITY);
		assertThat(electricity.amount()).isEqualTo(18_080L);
		assertThat(electricity.usage()).isEqualByComparingTo("113.000");
		assertThat(electricity.confidence()).isEqualByComparingTo("0.9997");
		assertThat(electricity.recordStatus()).isEqualTo(RecordStatus.CONFIRMED);

		assertThat(result.items().get(1).utilityType()).isEqualTo(UtilityType.WATER);
		assertThat(result.items().get(1).amount()).isEqualTo(13_840L);
		assertThat(result.items().get(2).utilityType()).isEqualTo(UtilityType.GAS);
		assertThat(result.items().get(2).amount()).isEqualTo(12_400L);
	}

	@Test
	void returnsThreeFixedItemsAndMarksPartialRecognition() {
		BillOcrService service = service((image, format) -> new Recognition("SUCCESS", BillType.MANAGEMENT, List.of(
			field("billing_month", "2026년 7월", "0.9900"),
			field("electricity_usage", "113kWh", "0.9900"),
			field("electricity_amount", "18,080", "0.9900")
		)));

		var started = service.start(USER_ID, png(), null);
		var result = service.getResult(USER_ID, started.jobId());

		assertThat(result.status()).isEqualTo(BillOcrJobStatus.SUCCEEDED);
		assertThat(result.partialRecognition()).isTrue();
		assertThat(result.items()).extracting(item -> item.hasData())
			.containsExactly(true, false, false);
	}

	@Test
	void usesBillingMonthHintWhenMonthWasNotRecognized() {
		BillOcrService service = service((image, format) -> new Recognition("SUCCESS", BillType.ELECTRICITY, List.of(
			field("electricity_usage", "113kWh", "0.9900"),
			field("electricity_amount", "18,080", "0.9900")
		)));

		var started = service.start(USER_ID, png(), YearMonth.of(2026, 7));
		var result = service.getResult(USER_ID, started.jobId());

		assertThat(result.billingMonth()).isEqualTo("2026-07");
		assertThat(result.items().getFirst().hasData()).isTrue();
	}

	@Test
	void lowConfidenceRequiresReview() {
		BillOcrService service = service((image, format) -> new Recognition("SUCCESS", BillType.ELECTRICITY, List.of(
			field("billing_month", "2026년 7월", "0.9900"),
			field("electricity_usage", "113kWh", "0.6900"),
			field("electricity_amount", "18,080", "0.9900")
		)));

		var started = service.start(USER_ID, png(), null);
		var result = service.getResult(USER_ID, started.jobId());

		assertThat(result.items().getFirst().recordStatus()).isEqualTo(RecordStatus.REVIEW_REQUIRED);
	}

	@Test
	void convertsRecognitionFailureToFallbackResult() {
		BillOcrService service = service((image, format) -> new Recognition("FAILURE", null, List.of()));

		var started = service.start(USER_ID, png(), null);
		var result = service.getResult(USER_ID, started.jobId());

		assertThat(result.status()).isEqualTo(BillOcrJobStatus.FAILED);
		assertThat(result.errorCode()).isEqualTo("OCR_FAILED");
		assertThat(result.fallbackScreen()).isEqualTo("AN-05");
	}

	@Test
	void convertsProviderTimeoutToTimeoutResult() {
		BillOcrService service = service((image, format) -> {
			throw new ClovaOcrClientException(true, "timeout");
		});

		var started = service.start(USER_ID, png(), null);
		var result = service.getResult(USER_ID, started.jobId());

		assertThat(result.status()).isEqualTo(BillOcrJobStatus.TIMEOUT);
		assertThat(result.errorCode()).isEqualTo("EXTERNAL_TIMEOUT");
		assertThat(result.fallbackScreen()).isEqualTo("AN-05");
	}

	@Test
	void normalizesIndividualElectricityWaterAndGasResults() {
		assertIndividual(
			BillType.ELECTRICITY,
			UtilityType.ELECTRICITY,
			"electricity_usage",
			"113 kWh",
			"electricity_amount",
			"18,080원",
			18_080L,
			"113.000"
		);
		assertIndividual(
			BillType.WATER,
			UtilityType.WATER,
			"water_usage",
			"11 m³",
			"water_amount",
			"13,840원",
			13_840L,
			"11.000"
		);
		assertIndividual(
			BillType.GAS,
			UtilityType.GAS,
			"gas_usage",
			"14 m³",
			"gas_amount",
			"12,400원",
			12_400L,
			"14.000"
		);
	}

	@Test
	void rejectsUnknownTemplateAndMismatchedIndividualFields() {
		BillOcrService unknownTemplateService = service((image, format) ->
			new Recognition("SUCCESS", null, List.of(
				field("billing_month", "2026년 7월", "0.9900"),
				field("electricity_usage", "113 kWh", "0.9900"),
				field("electricity_amount", "18,080원", "0.9900")
			))
		);
		BillOcrService mismatchedFieldsService = service((image, format) ->
			new Recognition("SUCCESS", BillType.WATER, List.of(
				field("billing_month", "2026년 7월", "0.9900"),
				field("electricity_usage", "113 kWh", "0.9900"),
				field("electricity_amount", "18,080원", "0.9900")
			))
		);

		var unknownResult = unknownTemplateService.getResult(
			USER_ID,
			unknownTemplateService.start(USER_ID, png(), null).jobId()
		);
		var mismatchedResult = mismatchedFieldsService.getResult(
			USER_ID,
			mismatchedFieldsService.start(USER_ID, png(), null).jobId()
		);

		assertThat(unknownResult.status()).isEqualTo(BillOcrJobStatus.FAILED);
		assertThat(unknownResult.errorCode()).isEqualTo("OCR_FAILED");
		assertThat(mismatchedResult.status()).isEqualTo(BillOcrJobStatus.FAILED);
		assertThat(mismatchedResult.errorCode()).isEqualTo("OCR_FAILED");
	}

	@Test
	void rejectsUnsupportedAndOversizedImages() {
		BillOcrService service = service((image, format) -> successFields());

		assertThatThrownBy(() -> service.start(USER_ID, new MockMultipartFile(
			"image", "bill.gif", "image/gif", new byte[] { 1, 2, 3 }
		), null))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(BillErrorCode.IMAGE_UNSUPPORTED));

		byte[] oversized = new byte[10 * 1024 * 1024 + 1];
		assertThatThrownBy(() -> service.start(USER_ID, new MockMultipartFile(
			"image", "bill.png", "image/png", oversized
		), null))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(BillErrorCode.IMAGE_TOO_LARGE));
	}

	@Test
	void hidesUnknownAndOtherUsersJobs() {
		BillOcrService service = service((image, format) -> successFields());
		var started = service.start(USER_ID, png(), null);

		assertThatThrownBy(() -> service.getResult(99L, started.jobId()))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(BillErrorCode.OCR_JOB_NOT_FOUND));
		assertThatThrownBy(() -> service.getResult(USER_ID, "ocr_missing"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(BillErrorCode.OCR_JOB_NOT_FOUND));
	}

	private static BillOcrService service(ClovaOcrClient client) {
		return new BillOcrService(client, Runnable::run);
	}

	private static Recognition successFields() {
		return new Recognition("SUCCESS", BillType.MANAGEMENT, List.of(
			field("billing_month", "2026년 7월", "0.99975"),
			field("electricity_usage", "113kWh", "0.9997"),
			field("electricity_amount", "18,080", "0.9998"),
			field("water_usage", "11ton", "0.9999"),
			field("water_amount", "13,840", "1.0"),
			field("gas_usage", "14m3", "0.9997"),
			field("gas_amount", "12,400", "1.0")
		));
	}

	private static void assertIndividual(
		BillType billType,
		UtilityType utilityType,
		String usageFieldName,
		String usageText,
		String amountFieldName,
		String amountText,
		long expectedAmount,
		String expectedUsage
	) {
		BillOcrService service = service((image, format) -> new Recognition("SUCCESS", billType, List.of(
			field("billing_month", "2026년 7월", "0.9999"),
			field(usageFieldName, usageText, "0.9998"),
			field(amountFieldName, amountText, "0.9997")
		)));

		var result = service.getResult(USER_ID, service.start(USER_ID, png(), null).jobId());

		assertThat(result.status()).isEqualTo(BillOcrJobStatus.SUCCEEDED);
		assertThat(result.billType()).isEqualTo(billType);
		assertThat(result.partialRecognition()).isFalse();
		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().utilityType()).isEqualTo(utilityType);
		assertThat(result.items().getFirst().amount()).isEqualTo(expectedAmount);
		assertThat(result.items().getFirst().usage()).isEqualByComparingTo(expectedUsage);
	}

	private static Field field(String name, String text, String confidence) {
		return new Field(name, text, new BigDecimal(confidence));
	}

	private static MockMultipartFile png() {
		return new MockMultipartFile(
			"image",
			"bill.png",
			"image/png",
			new byte[] { (byte)0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1 }
		);
	}
}
