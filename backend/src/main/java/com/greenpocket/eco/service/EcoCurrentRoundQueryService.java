package com.greenpocket.eco.service;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.eco.repository.EcoRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EcoCurrentRoundQueryService {

	private final EcoRepository ecoRepository;

	public Optional<Long> findCurrentRoundId(Long userId) {
		return ecoRepository.findCurrentRound(userId).map(EcoRepository.EcoRoundSnapshot::id);
	}
}
