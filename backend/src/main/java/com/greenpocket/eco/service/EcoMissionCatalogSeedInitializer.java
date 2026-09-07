package com.greenpocket.eco.service;

import javax.sql.DataSource;

import java.nio.charset.StandardCharsets;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EcoMissionCatalogSeedInitializer implements ApplicationRunner {

	private static final String SEED_PATH = "db/seed/mission_catalog.sql";

	private final DataSource dataSource;

	@Override
	public void run(ApplicationArguments args) {
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
			new ClassPathResource(SEED_PATH)
		);
		populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
		populator.execute(dataSource);
	}
}
