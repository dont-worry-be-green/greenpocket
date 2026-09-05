package com.greenpocket.profile.entity;

public enum AreaBand {
	UNDER_10("10평 이하"),
	FROM_10_TO_20("10~20평"),
	OVER_20("20평 이상");

	private final String label;

	AreaBand(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
