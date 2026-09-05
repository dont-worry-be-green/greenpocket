package com.greenpocket.profile.entity;

public enum HousingType {
	ONE_ROOM("원룸"),
	OFFICETEL("오피스텔"),
	APARTMENT("아파트"),
	MULTI_HOUSE("다세대");

	private final String label;

	HousingType(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
