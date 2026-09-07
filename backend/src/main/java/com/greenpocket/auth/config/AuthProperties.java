package com.greenpocket.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "greenpocket.auth")
public record AuthProperties(
	String jwtSecretBase64,
	long accessExpirationSeconds,
	long refreshExpirationSeconds,
	boolean refreshCookieSecure,
	boolean demoEnabled
) {
}
