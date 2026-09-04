<script setup>
/*
 * 요금 1종의 목표 구간 — WF-04(등록) · WF-05(미등록)
 *
 * `segment` 는 `GET /eco/rounds/{roundId}/goal-form` 의 `segments[]` 원형이다.
 * `target` 은 `POST .../goal/preview` 의 `utilities[]` 중 같은 `utilityType` 항목이고
 * **없을 수 있다** — 구간을 아직 안 골랐거나 미리보기가 아직 안 왔을 때다.
 *
 * ── 미등록이면 칩을 그리지 않는다 (B-2-02 예외) ──────────────────────────
 * 기준 사용량이 없어 목표 사용량을 만들 수 없고 마일리지 합산에서도 빠진다
 * (`excludedFromCombine`). 사유 문장(`unregisteredReason`)은 DB 에 문장으로 저장된 값이라
 * 서버가 준 것을 그대로 쓴다 — 화면에서 조립하지 않는다.
 * **미션 목록은 그래도 보여준다.** 마일리지에 못 들어갈 뿐 관리비는 줄기 때문이다.
 *
 * 목표 사용량의 소수 자리수는 `target.displayPrecision` 을 쓴다. preview 가 오기 전에는
 * 그 값이 없어서 단위에서 되짚는다(utils/format.js 의 usagePrecision).
 */
import { computed } from 'vue'

import GpBandPicker from '@/components/ui/GpBandPicker.vue'
import GpCard from '@/components/ui/GpCard.vue'
import IconWarning from '@/components/ui/icons/IconWarning.vue'
import UtilityIcon from './UtilityIcon.vue'
import {
  formatUnit,
  formatUsage,
  formatUtilityType,
  formatWon,
  usagePrecision,
} from '@/utils/format'

const props = defineProps({
  segment: { type: Object, required: true },
  tiers: { type: Array, required: true },
  modelValue: { type: String, default: null }, // 고른 tier 코드
  target: { type: Object, default: null }, // preview.utilities[] 의 같은 요금 항목
})
defineEmits(['update:modelValue'])

const name = computed(() => formatUtilityType(props.segment.utilityType))
const unit = computed(() => formatUnit(props.segment.usageUnit))

const precision = computed(
  () => props.target?.displayPrecision ?? usagePrecision(props.segment.usageUnit),
)

const baselineUsageText = computed(() =>
  formatUsage(props.segment.baselineUsage, precision.value, unit.value),
)
</script>

<template>
  <!-- WF-05 · 미등록. 기준값이 없으니 금액·구간 자리를 비워두지 않고 통째로 안내로 바꾼다 -->
  <GpCard v-if="!segment.registered" tone="sub">
    <div class="flex items-start gap-3">
      <span class="text-estimated mt-0.5 flex-none"><IconWarning :size="20" /></span>
      <div class="min-w-0 flex-1">
        <p class="text-list-title m-0">{{ name }}</p>
        <p class="text-caption text-muted mt-1 mb-0">{{ segment.unregisteredReason }}</p>
        <p class="text-caption text-muted mt-2 mb-0">
          등록하지 않아도 실천은 할 수 있어요. 다만 마일리지 합산에는 들어가지 않아요.
        </p>
        <a
          v-if="segment.registerGuideUrl"
          :href="segment.registerGuideUrl"
          target="_blank"
          rel="noopener"
          class="bg-primary-bg text-primary-on-soft text-body-strong mt-3 flex h-(--gp-wbtn-h) items-center justify-center rounded-sm no-underline"
        >
          {{ name }} 등록 안내 보기
        </a>
      </div>
    </div>
  </GpCard>

  <GpCard v-else>
    <div class="flex items-center gap-3">
      <UtilityIcon :utility-type="segment.utilityType" />
      <div class="min-w-0 flex-1">
        <p class="text-list-title m-0">{{ name }} 기준</p>
        <p class="text-caption text-muted tabular-nums mt-0.5 mb-0">
          {{ baselineUsageText }} · 6개월
        </p>
      </div>
      <p class="text-amount tabular-nums m-0">{{ formatWon(segment.baselineAmount) }}</p>
    </div>

    <GpBandPicker
      :model-value="modelValue"
      :tiers="tiers"
      :label="`${name} 감축 목표 구간`"
      @update:model-value="$emit('update:modelValue', $event)"
    />

    <!-- 목표 사용량·절감액은 서버(preview)가 계산한다. 구간을 고르기 전에는 없다 -->
    <div
      v-if="target"
      class="border-divider text-caption mt-4 flex items-center justify-between border-t pt-3"
    >
      <span class="text-muted">
        목표
        <span class="text-ink tabular-nums font-semibold">
          {{ formatUsage(target.targetUsage, precision, unit) }}
        </span>
        까지
      </span>
      <span class="text-ink-soft tabular-nums font-semibold">
        {{ formatWon(target.expectedSaving) }} 덜 내요
      </span>
    </div>
    <p v-else class="text-caption text-muted mt-4 mb-0">구간을 고르면 목표 사용량이 나와요</p>
  </GpCard>
</template>
