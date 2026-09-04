package com.greenpocket.bill.entity;

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
@Table(name = "utility_monthly_record")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UtilityMonthlyRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "record_source", nullable = false)
	private RecordSource recordSource;

	@Column(name = "billing_month", nullable = false)
	private LocalDate billingMonth;

	@Enumerated(EnumType.STRING)
	@Column(name = "utility_type", nullable = false)
	private UtilityType utilityType;

	@Enumerated(EnumType.STRING)
	@Column(name = "bill_type")
	private BillType billType;

	@Column(name = "amount", nullable = false)
	private Long amount;

	@Column(name = "usage_value", nullable = false, precision = 12, scale = 3)
	private BigDecimal usageValue;

	@Enumerated(EnumType.STRING)
	@Column(name = "usage_unit", nullable = false)
	private UsageUnit usageUnit;

	@Enumerated(EnumType.STRING)
	@Column(name = "input_source", nullable = false)
	private InputSource inputSource;

	@Column(name = "confidence", precision = 5, scale = 4)
	private BigDecimal confidence;

	@Enumerated(EnumType.STRING)
	@Column(name = "record_status", nullable = false)
	private RecordStatus recordStatus;

	@Column(name = "registered_at", insertable = false, updatable = false)
	private LocalDateTime registeredAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	private UtilityMonthlyRecord(
		Long userId,
		LocalDate billingMonth,
		UtilityType utilityType,
		BillType billType,
		Long amount,
		BigDecimal usageValue,
		UsageUnit usageUnit,
		InputSource inputSource,
		BigDecimal confidence,
		RecordStatus recordStatus
	) {
		this.userId = userId;
		this.recordSource = RecordSource.BILL;
		this.billingMonth = billingMonth;
		this.utilityType = utilityType;
		this.billType = billType;
		this.amount = amount;
		this.usageValue = usageValue;
		this.usageUnit = usageUnit;
		this.inputSource = inputSource;
		this.confidence = confidence;
		this.recordStatus = recordStatus;
	}

	public static UtilityMonthlyRecord createBill(
		Long userId,
		LocalDate billingMonth,
		UtilityType utilityType,
		BillType billType,
		Long amount,
		BigDecimal usageValue,
		UsageUnit usageUnit,
		InputSource inputSource,
		BigDecimal confidence,
		RecordStatus recordStatus
	) {
		return new UtilityMonthlyRecord(
			userId,
			billingMonth,
			utilityType,
			billType,
			amount,
			usageValue,
			usageUnit,
			inputSource,
			confidence,
			recordStatus
		);
	}
}
