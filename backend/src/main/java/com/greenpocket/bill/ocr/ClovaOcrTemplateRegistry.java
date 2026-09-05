package com.greenpocket.bill.ocr;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.greenpocket.bill.entity.BillType;

@Component
public class ClovaOcrTemplateRegistry {

	private final Map<Long, BillType> billTypeByTemplateId;

	public ClovaOcrTemplateRegistry(
		@Value("${clova.ocr.management-template-id:}") String managementTemplateId,
		@Value("${clova.ocr.electricity-template-id:}") String electricityTemplateId,
		@Value("${clova.ocr.water-template-id:}") String waterTemplateId,
		@Value("${clova.ocr.gas-template-id:}") String gasTemplateId
	) {
		Map<Long, BillType> mappings = new LinkedHashMap<>();
		register(mappings, managementTemplateId, BillType.MANAGEMENT);
		register(mappings, electricityTemplateId, BillType.ELECTRICITY);
		register(mappings, waterTemplateId, BillType.WATER);
		register(mappings, gasTemplateId, BillType.GAS);
		this.billTypeByTemplateId = Collections.unmodifiableMap(new LinkedHashMap<>(mappings));
	}

	public List<Long> templateIds() {
		return List.copyOf(billTypeByTemplateId.keySet());
	}

	public BillType resolve(Long templateId) {
		return templateId == null ? null : billTypeByTemplateId.get(templateId);
	}

	private static void register(Map<Long, BillType> mappings, String rawTemplateId, BillType billType) {
		Long templateId = parseTemplateId(rawTemplateId, billType);
		if (templateId == null) {
			return;
		}
		BillType previous = mappings.putIfAbsent(templateId, billType);
		if (previous != null) {
			throw new IllegalArgumentException(
				"CLOVA OCR template ID " + templateId + " is configured for both " + previous + " and " + billType
			);
		}
	}

	private static Long parseTemplateId(String rawTemplateId, BillType billType) {
		if (rawTemplateId == null || rawTemplateId.isBlank()) {
			return null;
		}
		try {
			return Long.valueOf(rawTemplateId.trim());
		}
		catch (NumberFormatException exception) {
			throw new IllegalArgumentException(
				"CLOVA OCR " + billType + " template ID must be a number",
				exception
			);
		}
	}
}
