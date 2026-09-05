package com.greenpocket.bill.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.greenpocket.bill.dto.BillOcrJobStatus;
import com.greenpocket.bill.dto.BillOcrResultResponse;
import com.greenpocket.bill.dto.BillOcrStartResponse;
import com.greenpocket.bill.entity.BillType;
import com.greenpocket.bill.entity.RecordStatus;
import com.greenpocket.bill.entity.UsageUnit;
import com.greenpocket.bill.exception.BillErrorCode;
import com.greenpocket.bill.ocr.ClovaOcrClient;
import com.greenpocket.bill.ocr.ClovaOcrClient.Field;
import com.greenpocket.bill.ocr.ClovaOcrClient.Recognition;
import com.greenpocket.bill.ocr.ClovaOcrClientException;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.global.type.UtilityType;

@Service
public class BillOcrService {

	private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
	private static final int POLL_AFTER_MS = 1_000;
	private static final BigDecimal REVIEW_THRESHOLD = new BigDecimal("0.7000");
	private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile("(\\d{4})\\D+(\\d{1,2})");
	private static final Pattern DECIMAL_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?");
	private static final String OCR_FAILED_MESSAGE =
		"사진에서 값을 읽지 못했어요. 지원하지 않는 양식이거나 글자가 흐릴 수 있어요.";
	private static final String OCR_TIMEOUT_MESSAGE = "시간이 오래 걸리고 있어요. 다시 시도해 주세요.";

	private final ClovaOcrClient clovaOcrClient;
	private final Executor executor;
	private final AutoCloseable ownedExecutor;
	private final Map<String, OcrJob> jobs = new ConcurrentHashMap<>();

	@Autowired
	public BillOcrService(ClovaOcrClient clovaOcrClient) {
		this.clovaOcrClient = clovaOcrClient;
		var virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
		this.executor = virtualThreadExecutor;
		this.ownedExecutor = virtualThreadExecutor;
	}

	BillOcrService(ClovaOcrClient clovaOcrClient, Executor executor) {
		this.clovaOcrClient = clovaOcrClient;
		this.executor = executor;
		this.ownedExecutor = null;
	}

	public BillOcrStartResponse start(Long userId, MultipartFile image, YearMonth billingMonthHint) {
		ValidatedImage validatedImage = validate(image);
		String jobId = "ocr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		jobs.put(jobId, OcrJob.pending(userId, jobId));
		executor.execute(() -> recognize(jobId, validatedImage, billingMonthHint));
		return new BillOcrStartResponse(jobId, BillOcrJobStatus.PENDING, 0, POLL_AFTER_MS);
	}

	public BillOcrResultResponse getResult(Long userId, String jobId) {
		OcrJob job = jobs.get(jobId);
		if (job == null || !job.userId().equals(userId)) {
			throw new BusinessException(BillErrorCode.OCR_JOB_NOT_FOUND);
		}
		return job.response();
	}

	private void recognize(String jobId, ValidatedImage image, YearMonth billingMonthHint) {
		Long userId = jobs.get(jobId).userId();
		jobs.put(jobId, OcrJob.running(userId, jobId));
		try {
			Recognition recognition = clovaOcrClient.recognize(image.bytes(), image.format());
			if (!"SUCCESS".equals(recognition.inferResult())) {
				jobs.put(jobId, OcrJob.failed(userId, failed(jobId, false)));
				return;
			}
			BillOcrResultResponse response = normalize(jobId, recognition.fields(), billingMonthHint);
			jobs.put(jobId, OcrJob.completed(userId, response));
		}
		catch (ClovaOcrClientException exception) {
			jobs.put(jobId, OcrJob.failed(userId, failed(jobId, exception.isTimeout())));
		}
		catch (RuntimeException exception) {
			jobs.put(jobId, OcrJob.failed(userId, failed(jobId, false)));
		}
	}

	private BillOcrResultResponse normalize(String jobId, List<Field> fields, YearMonth billingMonthHint) {
		Map<String, Field> byName = new java.util.HashMap<>();
		for (Field field : fields) {
			if (field.name() != null) {
				byName.put(field.name().toLowerCase(Locale.ROOT), field);
			}
		}

		Field monthField = byName.get("billing_month");
		YearMonth recognizedMonth = parseYearMonth(monthField == null ? null : monthField.text());
		YearMonth billingMonth = recognizedMonth == null ? billingMonthHint : recognizedMonth;
		EnumMap<UtilityType, BillOcrResultResponse.Item> items = new EnumMap<>(UtilityType.class);
		items.put(UtilityType.ELECTRICITY, item(
			UtilityType.ELECTRICITY, UsageUnit.kWh, billingMonth, monthField,
			byName.get("electricity_amount"), byName.get("electricity_usage")
		));
		items.put(UtilityType.WATER, item(
			UtilityType.WATER, UsageUnit.m3, billingMonth, monthField,
			byName.get("water_amount"), byName.get("water_usage")
		));
		items.put(UtilityType.GAS, item(
			UtilityType.GAS, UsageUnit.m3, billingMonth, monthField,
			byName.get("gas_amount"), byName.get("gas_usage")
		));

		List<BillOcrResultResponse.Item> orderedItems = List.of(
			items.get(UtilityType.ELECTRICITY),
			items.get(UtilityType.WATER),
			items.get(UtilityType.GAS)
		);
		long recognizedCount = orderedItems.stream().filter(BillOcrResultResponse.Item::hasData).count();
		if (recognizedCount == 0) {
			return failed(jobId, false);
		}
		return new BillOcrResultResponse(
			jobId,
			BillOcrJobStatus.SUCCEEDED,
			100,
			BillType.MANAGEMENT,
			billingMonth == null ? null : billingMonth.toString(),
			recognizedCount < orderedItems.size(),
			orderedItems,
			null,
			null,
			null
		);
	}

	private BillOcrResultResponse.Item item(
		UtilityType utilityType,
		UsageUnit usageUnit,
		YearMonth billingMonth,
		Field monthField,
		Field amountField,
		Field usageField
	) {
		Long amount = parseAmount(amountField == null ? null : amountField.text());
		BigDecimal usage = parseUsage(usageField == null ? null : usageField.text());
		boolean hasData = billingMonth != null && amount != null && usage != null;
		if (!hasData) {
			return new BillOcrResultResponse.Item(
				utilityType, false, null, null, null, usageUnit, null, null
			);
		}

		BigDecimal confidence = minConfidence(monthField, amountField, usageField);
		RecordStatus recordStatus = confidence != null && confidence.compareTo(REVIEW_THRESHOLD) < 0
			? RecordStatus.REVIEW_REQUIRED
			: RecordStatus.CONFIRMED;
		return new BillOcrResultResponse.Item(
			utilityType,
			true,
			billingMonth.toString(),
			amount,
			usage,
			usageUnit,
			confidence,
			recordStatus
		);
	}

	private ValidatedImage validate(MultipartFile image) {
		if (image == null || image.isEmpty()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "image", null);
		}
		if (image.getSize() > MAX_IMAGE_SIZE) {
			throw new BusinessException(BillErrorCode.IMAGE_TOO_LARGE, "image", null);
		}
		try {
			byte[] bytes = image.getBytes();
			String format = detectFormat(bytes);
			if (format == null) {
				throw new BusinessException(BillErrorCode.IMAGE_UNSUPPORTED, "image", null);
			}
			return new ValidatedImage(bytes, format);
		}
		catch (java.io.IOException exception) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "image", null);
		}
	}

	private static String detectFormat(byte[] bytes) {
		if (bytes.length >= 8
			&& (bytes[0] & 0xff) == 0x89
			&& bytes[1] == 0x50
			&& bytes[2] == 0x4e
			&& bytes[3] == 0x47
			&& bytes[4] == 0x0d
			&& bytes[5] == 0x0a
			&& bytes[6] == 0x1a
			&& bytes[7] == 0x0a) {
			return "png";
		}
		if (bytes.length >= 3
			&& (bytes[0] & 0xff) == 0xff
			&& (bytes[1] & 0xff) == 0xd8
			&& (bytes[2] & 0xff) == 0xff) {
			return "jpg";
		}
		return null;
	}

	private static YearMonth parseYearMonth(String text) {
		if (text == null) {
			return null;
		}
		Matcher matcher = YEAR_MONTH_PATTERN.matcher(text);
		if (!matcher.find()) {
			return null;
		}
		try {
			return YearMonth.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
		}
		catch (DateTimeException | NumberFormatException exception) {
			return null;
		}
	}

	private static Long parseAmount(String text) {
		if (text == null) {
			return null;
		}
		String digits = text.replaceAll("[^0-9]", "");
		if (digits.isEmpty()) {
			return null;
		}
		try {
			return Long.valueOf(digits);
		}
		catch (NumberFormatException exception) {
			return null;
		}
	}

	private static BigDecimal parseUsage(String text) {
		if (text == null) {
			return null;
		}
		Matcher matcher = DECIMAL_PATTERN.matcher(text.replace(",", ""));
		if (!matcher.find()) {
			return null;
		}
		try {
			return new BigDecimal(matcher.group()).setScale(3, RoundingMode.UNNECESSARY);
		}
		catch (ArithmeticException | NumberFormatException exception) {
			return null;
		}
	}

	private static BigDecimal minConfidence(Field... fields) {
		BigDecimal minimum = null;
		for (Field field : fields) {
			if (field == null || field.confidence() == null) {
				continue;
			}
			minimum = minimum == null ? field.confidence() : minimum.min(field.confidence());
		}
		return minimum == null ? null : minimum.setScale(4, RoundingMode.HALF_UP);
	}

	private static BillOcrResultResponse failed(String jobId, boolean timeout) {
		return new BillOcrResultResponse(
			jobId,
			timeout ? BillOcrJobStatus.TIMEOUT : BillOcrJobStatus.FAILED,
			null,
			null,
			null,
			null,
			null,
			timeout ? CommonErrorCode.EXTERNAL_TIMEOUT.code() : BillErrorCode.OCR_FAILED.code(),
			timeout ? OCR_TIMEOUT_MESSAGE : OCR_FAILED_MESSAGE,
			"AN-05"
		);
	}

	@PreDestroy
	void closeExecutor() throws Exception {
		if (ownedExecutor != null) {
			ownedExecutor.close();
		}
	}

	private record ValidatedImage(byte[] bytes, String format) {
	}

	private record OcrJob(Long userId, BillOcrResultResponse response) {

		private static OcrJob pending(Long userId, String jobId) {
			return new OcrJob(userId, new BillOcrResultResponse(
				jobId, BillOcrJobStatus.PENDING, 0, null, null, null, null, null, null, null
			));
		}

		private static OcrJob running(Long userId, String jobId) {
			return new OcrJob(userId, new BillOcrResultResponse(
				jobId, BillOcrJobStatus.RUNNING, 50, null, null, null, null, null, null, null
			));
		}

		private static OcrJob completed(Long userId, BillOcrResultResponse response) {
			return new OcrJob(userId, response);
		}

		private static OcrJob failed(Long userId, BillOcrResultResponse response) {
			return new OcrJob(userId, response);
		}
	}
}
