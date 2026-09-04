package com.greenpocket.pocket.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.greenpocket.pocket.entity.PocketTransaction;
import com.greenpocket.pocket.entity.TransactionDirection;
import com.greenpocket.pocket.entity.TransactionStatus;
import com.greenpocket.pocket.entity.TransactionType;

public interface PocketTransactionRepository extends JpaRepository<PocketTransaction, Long> {

	Optional<PocketTransaction> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

	boolean existsByTransactionCode(String transactionCode);

	Page<PocketTransaction> findByUserIdAndTransactionType(
		Long userId,
		TransactionType transactionType,
		Pageable pageable
	);

	@Query("""
		SELECT COALESCE(SUM(tx.amount), 0)
		FROM PocketTransaction tx
		WHERE tx.userId = :userId
		  AND tx.transactionStatus = :status
		  AND tx.direction = :direction
		""")
	Long sumAmount(
		@Param("userId") Long userId,
		@Param("status") TransactionStatus status,
		@Param("direction") TransactionDirection direction
	);

	@Query("""
		SELECT COALESCE(SUM(tx.amount), 0)
		FROM PocketTransaction tx
		WHERE tx.userId = :userId
		  AND tx.transactionStatus = :status
		  AND tx.direction = :direction
		  AND (tx.completedAt < :completedAt
		       OR (tx.completedAt = :completedAt AND tx.id <= :transactionId))
		""")
	Long sumAmountUntil(
		@Param("userId") Long userId,
		@Param("status") TransactionStatus status,
		@Param("direction") TransactionDirection direction,
		@Param("completedAt") LocalDateTime completedAt,
		@Param("transactionId") Long transactionId
	);
}
