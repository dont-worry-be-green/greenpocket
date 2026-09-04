package com.greenpocket.diagnosis.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.greenpocket.diagnosis.entity.RegionLevel;
import com.greenpocket.diagnosis.entity.RegionUtilitySnapshot;
import com.greenpocket.global.type.UtilityType;

public interface RegionUtilitySnapshotRepository extends JpaRepository<RegionUtilitySnapshot, Long> {

	Optional<RegionUtilitySnapshot>
	findFirstByRegionLevelAndSidoCodeAndSigunguCodeAndUtilityTypeAndBaseMonthLessThanEqualAndAvgUsageIsNotNullAndAvgAmountIsNotNullOrderByBaseMonthDesc(
		RegionLevel regionLevel,
		String sidoCode,
		String sigunguCode,
		UtilityType utilityType,
		LocalDate baseMonth
	);
}
