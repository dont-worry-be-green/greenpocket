package com.greenpocket.global.auth;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;

@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> parameterType = parameter.getParameterType();
		return parameter.hasParameterAnnotation(CurrentUserId.class)
			&& (parameterType == Long.class || parameterType == long.class);
	}

	@Override
	public Object resolveArgument(
		MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory
	) {
		Object userId = webRequest.getAttribute(
			DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE,
			NativeWebRequest.SCOPE_REQUEST
		);
		if (userId instanceof Long currentUserId) {
			return currentUserId;
		}
		throw new BusinessException(CommonErrorCode.UNAUTHENTICATED_DEMO_KEY);
	}
}
