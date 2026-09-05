package com.greenpocket.user.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import tools.jackson.databind.ObjectMapper;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class SeoulRegionCatalog {

	private static final String RESOURCE_PATH = "data/seoul-regions.json";
	private static final int SEOUL_SIGUNGU_COUNT = 25;

	private final Catalog catalog;
	private final Map<String, Sigungu> sigunguByCode;

	public SeoulRegionCatalog(ObjectMapper objectMapper) {
		this.catalog = load(objectMapper);
		this.sigunguByCode = index(catalog);
	}

	public Sido sido() {
		return new Sido(catalog.sidoCode(), catalog.sidoName());
	}

	public boolean supportsSido(String sidoCode) {
		return catalog.sidoCode().equals(sidoCode);
	}

	public List<Sigungu> sigungu() {
		return catalog.sigungu();
	}

	public Optional<Sigungu> findSigungu(String sidoCode, String sigunguCode) {
		if (!supportsSido(sidoCode)) {
			return Optional.empty();
		}
		return Optional.ofNullable(sigunguByCode.get(sigunguCode));
	}

	private static Catalog load(ObjectMapper objectMapper) {
		try (InputStream inputStream = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
			return objectMapper.readValue(inputStream, Catalog.class);
		}
		catch (IOException exception) {
			throw new IllegalStateException("서울 행정구역 목록을 읽지 못했습니다.", exception);
		}
	}

	private static Map<String, Sigungu> index(Catalog catalog) {
		if (!"11".equals(catalog.sidoCode()) || catalog.sigungu() == null
			|| catalog.sigungu().size() != SEOUL_SIGUNGU_COUNT) {
			throw new IllegalStateException("서울 행정구역 목록은 25개 자치구여야 합니다.");
		}
		Map<String, Sigungu> indexed = new LinkedHashMap<>();
		for (Sigungu sigungu : catalog.sigungu()) {
			if (sigungu.code() == null || sigungu.name() == null
				|| indexed.putIfAbsent(sigungu.code(), sigungu) != null) {
				throw new IllegalStateException("서울 행정구역 목록에 잘못된 값이 있습니다.");
			}
		}
		return Collections.unmodifiableMap(indexed);
	}

	private record Catalog(
		String sourceName,
		String sourceUrl,
		String referenceDate,
		String sidoCode,
		String sidoName,
		List<Sigungu> sigungu
	) {
	}

	public record Sido(String code, String name) {
	}

	public record Sigungu(String code, String name) {
	}
}
