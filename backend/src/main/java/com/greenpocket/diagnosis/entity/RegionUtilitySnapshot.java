package com.greenpocket.diagnosis.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.greenpocket.global.type.UtilityType;

@Getter
@Entity
@Table(name = "region_utility_snapshot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegionUtilitySnapshot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "region_level", nullable = false, length = 10)
	private RegionLevel regionLevel;

	@Column(name = "sido_code", nullable = false, length = 10)
	private String sidoCode;

	@Column(name = "sigungu_code", nullable = false, length = 10)
	private String sigunguCode;

	@Column(name = "base_month", nullable = false)
	private LocalDate baseMonth;

	@Enumerated(EnumType.STRING)
	@Column(name = "utility_type", nullable = false, length = 20)
	private UtilityType utilityType;

	@Column(name = "household_count")
	private Long householdCount;

	@Column(name = "avg_usage", precision = 12, scale = 3)
	private BigDecimal avgUsage;

	@Column(name = "avg_amount")
	private Long avgAmount;

	@Column(name = "source_name", nullable = false, length = 100)
	private String sourceName;

	@Column(name = "extracted_at", nullable = false)
	private LocalDateTime extractedAt;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;
}
