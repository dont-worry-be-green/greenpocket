package com.greenpocket.diagnosis.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.greenpocket.diagnosis.entity.RegionLevel;
import com.greenpocket.diagnosis.entity.RegionUtilitySnapshot;
import com.greenpocket.global.type.UtilityType;

public interface RegionUtilitySnapshotRepository extends JpaRepository<RegionUtilitySnapshot, Long> {

	boolean existsByRegionLevelAndSidoCodeAndSigunguCode(
		RegionLevel regionLevel,
		String sidoCode,
		String sigunguCode
	);

	@Query("""
		select distinct snapshot.sigunguCode
		from RegionUtilitySnapshot snapshot
		where snapshot.regionLevel = :regionLevel
		  and snapshot.sidoCode = :sidoCode
		""")
	List<String> findDistinctSigunguCodes(
		@Param("regionLevel") RegionLevel regionLevel,
		@Param("sidoCode") String sidoCode
	);

	Optional<RegionUtilitySnapshot>
	findFirstByRegionLevelAndSidoCodeAndSigunguCodeAndUtilityTypeAndBaseMonthLessThanEqualAndAvgUsageIsNotNullAndAvgAmountIsNotNullOrderByBaseMonthDesc(
		RegionLevel regionLevel,
		String sidoCode,
		String sigunguCode,
		UtilityType utilityType,
		LocalDate baseMonth
	);
}
