package com.greenpocket.global.config;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.greenpocket.global.auth.ApiAuthenticationInterceptor;
import com.greenpocket.global.auth.CurrentUserIdArgumentResolver;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

	private final ApiAuthenticationInterceptor apiAuthenticationInterceptor;
	private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(apiAuthenticationInterceptor)
			.addPathPatterns("/api/v1/**")
			.excludePathPatterns(
				"/api/v1/auth/**",
				"/api/v1/meta/**"
			);
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(currentUserIdArgumentResolver);
	}
}
