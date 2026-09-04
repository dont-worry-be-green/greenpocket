package com.greenpocket.pocket.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.greenpocket.pocket.entity.WithdrawalAccount;

public interface WithdrawalAccountRepository extends JpaRepository<WithdrawalAccount, Long> {

	List<WithdrawalAccount> findByUserIdAndIsActiveTrueOrderByIsDefaultDescIdAsc(Long userId);

	Optional<WithdrawalAccount> findByIdAndUserIdAndIsActiveTrue(Long id, Long userId);

	Optional<WithdrawalAccount> findFirstByUserIdAndIsDefaultTrueAndIsActiveTrue(Long userId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
		UPDATE WithdrawalAccount account
		SET account.isDefault = false,
			account.defaultSlot = null
		WHERE account.userId = :userId
		  AND account.isDefault = true
		""")
	int clearDefaultByUserId(@Param("userId") Long userId);
}
