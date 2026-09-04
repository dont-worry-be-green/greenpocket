package com.greenpocket.user.service;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.user.repository.UserPocketQueryRepository;
import com.greenpocket.user.repository.UserPocketQueryRepository.UserPocketSnapshot;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPocketQueryService {

	private final UserPocketQueryRepository userPocketQueryRepository;

	public Optional<UserPocketSnapshot> findPocket(Long userId) {
		return userPocketQueryRepository.findByUserId(userId);
	}
}
