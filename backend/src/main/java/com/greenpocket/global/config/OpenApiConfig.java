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

	private static final String DEMO_KEY_SCHEME = "demoKey";

	@Bean
	public OpenAPI greenPocketOpenApi() {
		return new OpenAPI()
			.info(new Info()
				.title("GreenPocket API")
				.description("GreenPocket 백엔드 API 명세")
				.version("v1"))
			.components(new Components()
				.addSecuritySchemes(DEMO_KEY_SCHEME, new SecurityScheme()
					.type(SecurityScheme.Type.APIKEY)
					.in(SecurityScheme.In.HEADER)
					.name("X-Demo-Key")
					.description("데모 사용자를 식별하는 UUID v4 키")))
			.addSecurityItem(new SecurityRequirement().addList(DEMO_KEY_SCHEME));
	}
}
