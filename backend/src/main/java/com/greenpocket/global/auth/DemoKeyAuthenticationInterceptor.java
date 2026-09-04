package com.greenpocket.global.auth;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;

@Component
@RequiredArgsConstructor
public class DemoKeyAuthenticationInterceptor implements HandlerInterceptor {

	public static final String DEMO_KEY_HEADER = "X-Demo-Key";
	public static final String CURRENT_USER_ID_ATTRIBUTE = DemoKeyAuthenticationInterceptor.class.getName()
		+ ".currentUserId";

	private static final String USERS_PATH = "/api/v1/users";
	private static final String META_PATH = "/api/v1/meta";
	private static final String DEMO_RESET_PATH = "/api/v1/demo/reset";

	private final DemoUserLookup demoUserLookup;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (isPublicRequest(request)) {
			return true;
		}

		String demoKey = request.getHeader(DEMO_KEY_HEADER);
		if (!isUuidV4(demoKey)) {
			throw unauthenticatedDemoKey();
		}

		Long userId = demoUserLookup.findUserIdByDemoKey(demoKey)
			.orElseThrow(DemoKeyAuthenticationInterceptor::unauthenticatedDemoKey);
		request.setAttribute(CURRENT_USER_ID_ATTRIBUTE, userId);
		return true;
	}

	private boolean isPublicRequest(HttpServletRequest request) {
		String path = request.getRequestURI().substring(request.getContextPath().length());
		String method = request.getMethod();

		if (HttpMethod.POST.matches(method) && USERS_PATH.equals(path)) {
			return true;
		}
		if (HttpMethod.GET.matches(method) && (META_PATH.equals(path) || path.startsWith(META_PATH + "/"))) {
			return true;
		}
		return HttpMethod.POST.matches(method) && DEMO_RESET_PATH.equals(path);
	}

	private boolean isUuidV4(String value) {
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

	private static BusinessException unauthenticatedDemoKey() {
		return new BusinessException(CommonErrorCode.UNAUTHENTICATED_DEMO_KEY);
	}
}
