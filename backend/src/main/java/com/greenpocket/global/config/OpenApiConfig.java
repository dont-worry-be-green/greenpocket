package com.greenpocket.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String BEARER_AUTH_SCHEME = "bearerAuth";

	@Bean
	public OpenAPI greenPocketOpenApi() {
		return new OpenAPI()
			.info(new Info()
				.title("GreenPocket API")
				.description("GreenPocket 백엔드 API 명세")
				.version("v1"))
			.components(new Components()
				.addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
					.description("로그인 또는 회원가입 응답의 Access Token")))
			.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
	}
}
