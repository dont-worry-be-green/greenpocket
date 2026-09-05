<script setup>
/*
 * 온보딩 · 단일 선택 칩 (A-1-02 주거 형태 · A-1-03 평수 구간)
 *
 * 두 그룹이 라벨과 선택지만 다르고 동작이 같아 **하나로 쓴다.**
 * `GpBandPicker` 를 재사용하지 않는 이유는 그쪽이 `formatMileage(t.mileage)` 를 템플릿에
 * 박고 있어 목표 구간 전용이기 때문이다.
 *
 * v-model 은 ENUM 코드 문자열이다(`APARTMENT` · `OVER_20`). 서버로 그대로 보낸다.
 */
defineProps({
  modelValue: { type: String, default: null },
  /** [{ value: 'APARTMENT', label: '아파트' }] */
  options: { type: Array, required: true },
  label: { type: String, required: true },
})
defineEmits(['update:modelValue'])
</script>

<template>
  <div>
    <span class="text-body-strong text-muted mb-3 block">{{ label }}</span>
    <div class="flex flex-wrap gap-2" role="radiogroup" :aria-label="label">
      <button
        v-for="option in options"
        :key="option.value"
        type="button"
        role="radio"
        :aria-checked="option.value === modelValue"
        class="ease-standard text-body-strong min-h-11 cursor-pointer rounded-full border-0 px-4 transition-colors duration-140"
        :class="
          option.value === modelValue
            ? 'bg-primary-bg text-primary-on-soft shadow-[inset_0_0_0_2px_var(--color-primary)]'
            : 'bg-surface text-ink-soft'
        "
        @click="$emit('update:modelValue', option.value)"
      >
        {{ option.label }}
      </button>
    </div>
  </div>
</template>
