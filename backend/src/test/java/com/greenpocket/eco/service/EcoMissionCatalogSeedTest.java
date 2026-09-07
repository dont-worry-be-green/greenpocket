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

		assertThat(electricityCount).isBetween(9, 12);
		assertThat(gasCount).isBetween(9, 12);
		assertThat(waterCount).isBetween(9, 12);
		assertThat(codes).hasSize(electricityCount + gasCount + waterCount);
		assertThat(sql).contains("ON DUPLICATE KEY UPDATE");
		assertThat(sql).contains("난이도는 비용이나 주거 설비 변경이 아니라");
		assertThat(sql).doesNotContain(
			"노후 난방배관 청소하기",
			"고효율 보일러로 교체하기",
			"온수 온도 55℃에서 40℃로 낮추기",
			"절수형 샤워헤드 사용하기",
			"수도꼭지에 절수기 달기",
			"변기 수조에 절수기 설치하기",
			"절수형 변기로 바꾸기"
		);
	}
}
