package com.greenpocket.user.service;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.user.repository.UserRegionQueryRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRegionQueryService {

	private final UserRegionQueryRepository userRegionQueryRepository;

	public Optional<String> findSidoCode(Long userId) {
		return userRegionQueryRepository.findSidoCodeByUserId(userId);
	}
}
