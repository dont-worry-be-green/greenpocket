package com.greenpocket.eco.service;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.eco.repository.EcoRepository;
import com.greenpocket.eco.entity.RoundStatus;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EcoCurrentRoundQueryService {

	private final EcoRepository ecoRepository;

	public Optional<Long> findCurrentRoundId(Long userId) {
		return ecoRepository.findCurrentRound(userId).map(EcoRepository.EcoRoundSnapshot::id);
	}

	public Optional<CurrentRoundLink> findCurrentRoundLink(Long userId) {
		return ecoRepository.findCurrentRound(userId)
			.map(round -> new CurrentRoundLink(round.id(), round.goalSetAt() != null));
	}

	public Optional<Long> findGoalActiveRoundId(Long userId) {
		return ecoRepository.findCurrentRound(userId)
			.filter(round -> round.roundStatus() == RoundStatus.GOAL_SET
				|| round.roundStatus() == RoundStatus.IN_PROGRESS)
			.map(EcoRepository.EcoRoundSnapshot::id);
	}

	public record CurrentRoundLink(Long roundId, boolean goalSet) {
	}
}
