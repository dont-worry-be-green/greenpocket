package com.greenpocket.bill.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.greenpocket.bill.entity.BillType;

class ClovaOcrTemplateRegistryTest {

	@Test
	void mapsConfiguredTemplateIdsToBillTypes() {
		ClovaOcrTemplateRegistry registry = new ClovaOcrTemplateRegistry(
			"43341", "43345", "43347", "43348"
		);

		assertThat(registry.templateIds()).containsExactly(43341L, 43345L, 43347L, 43348L);
		assertThat(registry.resolve(43341L)).isEqualTo(BillType.MANAGEMENT);
		assertThat(registry.resolve(43345L)).isEqualTo(BillType.ELECTRICITY);
		assertThat(registry.resolve(43347L)).isEqualTo(BillType.WATER);
		assertThat(registry.resolve(43348L)).isEqualTo(BillType.GAS);
		assertThat(registry.resolve(99999L)).isNull();
	}

	@Test
	void ignoresBlankIdsAndRejectsInvalidOrDuplicateIds() {
		ClovaOcrTemplateRegistry blankRegistry = new ClovaOcrTemplateRegistry("", " ", null, "");

		assertThat(blankRegistry.templateIds()).isEmpty();
		assertThatThrownBy(() -> new ClovaOcrTemplateRegistry("not-a-number", "", "", ""))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("MANAGEMENT");
		assertThatThrownBy(() -> new ClovaOcrTemplateRegistry("43341", "43341", "", ""))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("configured for both");
	}
}
