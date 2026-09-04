package com.greenpocket.eco.service;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.eco.repository.EcoMileageQueryRepository;
import com.greenpocket.eco.repository.EcoMileageQueryRepository.ConfirmedMileageRoundSnapshot;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EcoMileageQueryService {

	private final EcoMileageQueryRepository ecoMileageQueryRepository;

	public List<ConfirmedMileageRoundSnapshot> findConfirmedMileageRounds(Long userId) {
		return ecoMileageQueryRepository.findConfirmedMileageRounds(userId);
	}

	public Optional<ConfirmedMileageRoundSnapshot> findConfirmedMileageRound(Long userId, Long roundId) {
		return findConfirmedMileageRounds(userId).stream()
			.filter(round -> round.roundId().equals(roundId))
			.findFirst();
	}
}
