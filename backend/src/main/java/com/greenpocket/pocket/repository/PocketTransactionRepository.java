package com.greenpocket.pocket.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.greenpocket.pocket.entity.PocketTransaction;
import com.greenpocket.pocket.entity.TransactionDirection;
import com.greenpocket.pocket.entity.TransactionSourceType;
import com.greenpocket.pocket.entity.TransactionStatus;
import com.greenpocket.pocket.entity.TransactionType;

public interface PocketTransactionRepository extends JpaRepository<PocketTransaction, Long> {

	Optional<PocketTransaction> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

	Optional<PocketTransaction> findByUserIdAndIdempotencyKeyAndTransactionType(
		Long userId,
		String idempotencyKey,
		TransactionType transactionType
	);

	Optional<PocketTransaction> findByIdAndUserId(Long id, Long userId);

	Optional<PocketTransaction> findByUserIdAndSourceTypeAndSourceKey(
		Long userId,
		TransactionSourceType sourceType,
		String sourceKey
	);

	boolean existsByTransactionCode(String transactionCode);

	boolean existsByUserId(Long userId);

	Page<PocketTransaction> findByUserIdAndTransactionType(
		Long userId,
		TransactionType transactionType,
		Pageable pageable
	);

	@Query(
		value = """
			SELECT tx
			FROM PocketTransaction tx
			WHERE tx.userId = :userId
			  AND (:direction IS NULL OR tx.direction = :direction)
			  AND (:type IS NULL OR tx.transactionType = :type)
			ORDER BY COALESCE(tx.completedAt, tx.requestedAt) DESC, tx.id DESC
			""",
		countQuery = """
			SELECT COUNT(tx)
			FROM PocketTransaction tx
			WHERE tx.userId = :userId
			  AND (:direction IS NULL OR tx.direction = :direction)
			  AND (:type IS NULL OR tx.transactionType = :type)
			"""
	)
	Page<PocketTransaction> findTransactions(
		@Param("userId") Long userId,
		@Param("direction") TransactionDirection direction,
		@Param("type") TransactionType type,
		Pageable pageable
	);

	List<PocketTransaction> findTop2ByUserIdAndDirectionAndTransactionStatusOrderByCompletedAtDescIdDesc(
		Long userId,
		TransactionDirection direction,
		TransactionStatus transactionStatus
	);

	List<PocketTransaction> findTop3ByUserIdAndTransactionTypeOrderByRequestedAtDescIdDesc(
		Long userId,
		TransactionType transactionType
	);

	@Query("""
		SELECT tx.sourceKey
		FROM PocketTransaction tx
		WHERE tx.userId = :userId
		  AND tx.sourceType = :sourceType
		  AND tx.transactionStatus <> :retryableStatus
		""")
	List<String> findBlockingSourceKeys(
		@Param("userId") Long userId,
		@Param("sourceType") TransactionSourceType sourceType,
		@Param("retryableStatus") TransactionStatus retryableStatus
	);

	boolean existsByUserIdAndTransactionTypeAndRequestedAtGreaterThanEqualAndRequestedAtLessThan(
		Long userId,
		TransactionType transactionType,
		LocalDateTime startInclusive,
		LocalDateTime endExclusive
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
		  AND tx.transactionType = :type
		""")
	Long sumAmountByType(
		@Param("userId") Long userId,
		@Param("status") TransactionStatus status,
		@Param("direction") TransactionDirection direction,
		@Param("type") TransactionType type
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
