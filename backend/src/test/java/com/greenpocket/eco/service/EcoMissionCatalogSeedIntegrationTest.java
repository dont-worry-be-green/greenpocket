package com.greenpocket.eco.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
class EcoMissionCatalogSeedIntegrationTest {

	@Autowired
	private JdbcClient jdbcClient;

	@Test
	void loadsKoreanMissionTextAsUtf8() {
		MissionText mission = jdbcClient.sql("""
				SELECT title, evidence_text, source_org
				FROM mission_catalog
				WHERE mission_code = 'ELEC_AC_TEMP_26'
				""")
			.query(MissionText.class)
			.single();

		assertThat(mission.title()).isEqualTo("냉방 온도 26℃로 맞추기");
		assertThat(mission.evidenceText()).isEqualTo("1℃당 냉방 전력 7% · 하루 0.41kWh");
		assertThat(mission.sourceOrg()).isEqualTo("산업통상자원부·한국에너지공단");
	}

	private record MissionText(
		String title,
		String evidenceText,
		String sourceOrg
	) {
	}
}
