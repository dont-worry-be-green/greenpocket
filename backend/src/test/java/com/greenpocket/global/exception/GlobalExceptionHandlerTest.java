package com.greenpocket.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

	@Test
	void mapsMultipartLimitToBillImageTooLarge() {
		var response = new GlobalExceptionHandler().handleMaxUploadSizeExceeded(
			new MaxUploadSizeExceededException(10L * 1024 * 1024)
		);

		assertThat(response.getStatusCode().value()).isEqualTo(413);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().error().code()).isEqualTo("IMAGE_TOO_LARGE");
		assertThat(response.getBody().error().field()).isEqualTo("image");
	}
}
