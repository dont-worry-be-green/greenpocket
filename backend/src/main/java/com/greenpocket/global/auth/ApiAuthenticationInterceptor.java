package com.greenpocket.global.auth;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.greenpocket.auth.config.AuthProperties;
import com.greenpocket.auth.service.JwtTokenService;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;

@Component
@RequiredArgsConstructor
public class ApiAuthenticationInterceptor implements HandlerInterceptor {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenService jwtTokenService;
	private final AuthProperties properties;
	private final DemoUserLookup demoUserLookup;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (properties.demoEnabled() && isPublicDemoRequest(request)) {
			return true;
		}

		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (StringUtils.hasText(authorization)) {
			if (!authorization.startsWith(BEARER_PREFIX)) {
				throw unauthenticated();
			}
			Long userId = jwtTokenService.verifyAndGetUserId(authorization.substring(BEARER_PREFIX.length()));
			request.setAttribute(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, userId);
			return true;
		}

		if (properties.demoEnabled()) {
			String demoKey = request.getHeader(DemoKeyAuthenticationInterceptor.DEMO_KEY_HEADER);
			if (isUuidV4(demoKey)) {
				Long userId = demoUserLookup.findUserIdByDemoKey(demoKey)
					.orElseThrow(ApiAuthenticationInterceptor::unauthenticatedDemoKey);
				request.setAttribute(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, userId);
				return true;
			}
			if (StringUtils.hasText(demoKey)) {
				throw unauthenticatedDemoKey();
			}
		}

		throw unauthenticated();
	}

	private static boolean isPublicDemoRequest(HttpServletRequest request) {
		if (!HttpMethod.POST.matches(request.getMethod())) {
			return false;
		}
		String path = request.getRequestURI().substring(request.getContextPath().length());
		return "/api/v1/users".equals(path) || "/api/v1/demo/reset".equals(path);
	}

	private static boolean isUuidV4(String value) {
		if (!StringUtils.hasText(value)) {
			return false;
		}
		try {
			UUID uuid = UUID.fromString(value);
			return uuid.version() == 4 && uuid.toString().equalsIgnoreCase(value);
		}
		catch (IllegalArgumentException exception) {
			return false;
		}
	}

	private static BusinessException unauthenticated() {
		return new BusinessException(CommonErrorCode.UNAUTHENTICATED);
	}

	private static BusinessException unauthenticatedDemoKey() {
		return new BusinessException(CommonErrorCode.UNAUTHENTICATED_DEMO_KEY);
	}
}
