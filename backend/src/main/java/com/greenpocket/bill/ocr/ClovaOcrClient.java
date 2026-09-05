package com.greenpocket.bill.ocr;

import java.math.BigDecimal;
import java.util.List;

import com.greenpocket.bill.entity.BillType;

public interface ClovaOcrClient {

	Recognition recognize(byte[] image, String format);

	record Recognition(String inferResult, BillType billType, List<Field> fields) {
	}

	record Field(String name, String text, BigDecimal confidence) {
	}
}
