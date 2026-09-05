package com.greenpocket.bill.ocr;

import java.math.BigDecimal;
import java.util.List;

public interface ClovaOcrClient {

	Recognition recognize(byte[] image, String format);

	record Recognition(String inferResult, List<Field> fields) {
	}

	record Field(String name, String text, BigDecimal confidence) {
	}
}
