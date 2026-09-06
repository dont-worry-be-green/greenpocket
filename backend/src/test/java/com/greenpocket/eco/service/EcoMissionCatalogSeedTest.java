package com.greenpocket.eco.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class EcoMissionCatalogSeedTest {

	private static final Pattern MISSION_CODE = Pattern.compile("\\('((?:ELEC|GAS|WATER)_[A-Z0-9_]+)'");

	@Test
	void containsRequiredMissionCountForEachUtilityWithoutDuplicateCodes() throws IOException {
		String sql = new ClassPathResource("db/seed/mission_catalog.sql")
			.getContentAsString(StandardCharsets.UTF_8);
		Matcher matcher = MISSION_CODE.matcher(sql);
		HashSet<String> codes = new HashSet<>();
		int electricityCount = 0;
		int gasCount = 0;
		int waterCount = 0;

		while (matcher.find()) {
			String code = matcher.group(1);
			assertThat(codes.add(code)).as("중복 미션 코드: %s", code).isTrue();
			if (code.startsWith("ELEC_")) {
				electricityCount++;
			}
			else if (code.startsWith("GAS_")) {
				gasCount++;
			}
			else {
				waterCount++;
			}
		}

		assertThat(electricityCount).isBetween(6, 9);
		assertThat(gasCount).isBetween(6, 9);
		assertThat(waterCount).isBetween(6, 9);
		assertThat(codes).hasSize(electricityCount + gasCount + waterCount);
		assertThat(sql).contains("ON DUPLICATE KEY UPDATE");
	}
}
