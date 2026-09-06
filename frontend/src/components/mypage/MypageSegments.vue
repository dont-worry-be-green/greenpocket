<script setup>
/*
 * 밑줄 세그먼트 탭 — MY-03(전체·전기·수도·가스) · MY-04(월별·ECO) 공용
 *
 * `ui/GpTabBar` 는 **하단 탭바**라 다른 것이다. 화면 안에서 목록을 가르는 탭은 여기다.
 * `ui/` 로 올리지 않은 이유는 `MypageState` 와 같다 — 넷이 공유하는 파일을 늘리지 않는다.
 *
 * 배지 건수(`count`)는 **서버가 준 값만** 넘긴다. `null` 이면 배지를 그리지 않는다 —
 * 화면에서 목록 길이로 세면 필터·페이징이 걸린 뒤 실제 건수와 어긋난다(A-2-12 완료 조건).
 */
defineProps({
  /** [{ key, label, count? }] */
  items: { type: Array, required: true },
  modelValue: { type: String, default: '' },
})
defineEmits(['update:modelValue'])
</script>

<template>
  <div class="border-divider flex border-b" role="tablist">
    <button
      v-for="item in items"
      :key="item.key"
      type="button"
      role="tab"
      :aria-selected="item.key === modelValue"
      class="ease-standard text-body-strong -mb-px flex flex-1 cursor-pointer items-center justify-center gap-1 border-0 border-b-2 bg-transparent pb-2.5 transition-colors duration-140"
      :class="
        item.key === modelValue
          ? 'border-primary text-primary-on-soft'
          : 'border-transparent text-muted'
      "
      @click="$emit('update:modelValue', item.key)"
    >
      {{ item.label }}
      <span v-if="item.count != null" class="text-caption tabular-nums">{{ item.count }}</span>
    </button>
  </div>
</template>
