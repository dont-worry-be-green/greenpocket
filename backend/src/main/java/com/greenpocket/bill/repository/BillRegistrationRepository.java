package com.greenpocket.bill.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.greenpocket.bill.entity.RecordSource;
import com.greenpocket.bill.entity.UtilityMonthlyRecord;
import com.greenpocket.global.type.UtilityType;

public interface BillRegistrationRepository extends JpaRepository<UtilityMonthlyRecord, Long> {

	Optional<UtilityMonthlyRecord> findFirstByUserIdAndRecordSourceOrderByBillingMonthDescIdDesc(
		Long userId,
		RecordSource recordSource
	);

	Optional<UtilityMonthlyRecord> findByIdAndUserIdAndRecordSource(
		Long id,
		Long userId,
		RecordSource recordSource
	);

	List<UtilityMonthlyRecord> findByUserIdAndRecordSourceAndBillingMonth(
		Long userId,
		RecordSource recordSource,
		LocalDate billingMonth
	);

	List<UtilityMonthlyRecord> findByUserIdAndRecordSourceAndBillingMonthAndUtilityTypeIn(
		Long userId,
		RecordSource recordSource,
		LocalDate billingMonth,
		Collection<UtilityType> utilityTypes
	);
}
