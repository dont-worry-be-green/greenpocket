package com.greenpocket.bill.ocr;

public class ClovaOcrClientException extends RuntimeException {

	private final boolean timeout;

	public ClovaOcrClientException(boolean timeout, Throwable cause) {
		super(cause);
		this.timeout = timeout;
	}

	public ClovaOcrClientException(boolean timeout, String message) {
		super(message);
		this.timeout = timeout;
	}

	public boolean isTimeout() {
		return timeout;
	}
}
