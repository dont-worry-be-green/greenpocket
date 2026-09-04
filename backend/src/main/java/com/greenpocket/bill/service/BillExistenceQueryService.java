package com.greenpocket.bill.service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.bill.repository.BillExistenceQueryRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillExistenceQueryService {

	private final BillExistenceQueryRepository billExistenceQueryRepository;

	public boolean existsByUserId(Long userId) {
		return billExistenceQueryRepository.existsByUserId(userId);
	}
}
