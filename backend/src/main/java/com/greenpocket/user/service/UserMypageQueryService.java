package com.greenpocket.user.service;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.user.repository.UserMypageQueryRepository;
import com.greenpocket.user.repository.UserMypageQueryRepository.UserMypageSnapshot;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserMypageQueryService {

	private final UserMypageQueryRepository userMypageQueryRepository;

	public Optional<UserMypageSnapshot> findMypageUser(Long userId) {
		return userMypageQueryRepository.findByUserId(userId);
	}
}
