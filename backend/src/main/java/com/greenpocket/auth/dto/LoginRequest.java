package com.greenpocket.auth.dto;

public record LoginRequest(
	String email,
	String password
) {
}
