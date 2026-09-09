<script setup>
/*
 * 목표 카드 — WF-04 · WF-05 (B-2-02 ~ B-2-07, 결정 C-37)
 * (`EcoGoalCard` 는 WF-06 홈의 목표 요약 카드라 이름을 달리했다)
 *
 * 요금 3종을 **한 카드에 세로로** 놓고 각 행에서 0·5·10·15 단계 바로 절감률을 고른다. 행 오른쪽은
 * 그 절감률로 덜 내는 요금, 카드 아래는 줄어드는 요금 합계와 예상 마일리지다(EcoGoalSummary).
 * 탭으로 요금을 오가며 구간을 고르던 이전 구조는 「탭 → 구간 → 아래 합산 → 다시 탭 → 미션」으로
 * 흐름이 끊겨서 바꿨다(2026-09-09 수현). 탭은 미션 카테고리 전환만 맡는다.
 *
 * `segments` 는 `GET /eco/rounds/{roundId}/goal-form` 의 `segments[]`, `preview` 는
 * `POST .../goal/preview` 응답 원형이다. **금액·목표 사용량은 전부 서버 값이다.** 단계 바를 움직인
 * 직후 미리보기가 오기 전까지는 이전 값이 남아 있고, `loading` 으로 흐려진다.
 *
 * v-model 은 `{ [utilityType]: tier | null }` 맵이다. 행 하나가 바뀌면 맵을 새로 만들어 올린다.
 *
 * ── 미등록 요금 (WF-05 · B-2-06) ─────────────────────────────────────────
 * 기준값이 없어 단계 바를 그리지 않고 「미등록」 배지 + 「합산 제외」만 둔다. 사유·등록 안내는
 * 미션 탭 쪽(EcoGoalSegment)에서 보여준다 — 목표 카드가 안내문으로 길어지지 않게.
 */
import { computed } from 'vue'

import GpBandPicker from '@/components/ui/GpBandPicker.vue'
import GpCard from '@/components/ui/GpCard.vue'
import GpTag from '@/components/ui/GpTag.vue'
import EcoGoalSummary from './EcoGoalSummary.vue'
import UtilityIcon from './UtilityIcon.vue'
import {
  formatUnit,
  formatUsage,
  formatUtilityType,
  formatWon,
  usagePrecision,
} from '@/utils/format'

const props = defineProps({
  segments: { type: Array, required: true },
  tiers: { type: Array, required: true },
  modelValue: { type: Object, required: true }, // { ELECTRICITY: 'TIER_10', GAS: null, ... }
  preview: { type: Object, default: null },
  loading: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

const targetByUtility = computed(
  () => new Map((props.preview?.utilities ?? []).map((item) => [item.utilityType, item])),
)

function setTier(utilityType, tier) {
  emit('update:modelValue', { ...props.modelValue, [utilityType]: tier })
}

function targetUsageText(segment) {
  const target = targetByUtility.value.get(segment.utilityType)
  if (!target) return ''
  const precision = target.displayPrecision ?? usagePrecision(segment.usageUnit)
  return `${formatUsage(target.targetUsage, precision, formatUnit(segment.usageUnit))}까지`
}
</script>

<template>
  <GpCard title="얼마나 줄일까요">
    <div class="divide-divider border-divider divide-y border-t">
      <div
        v-for="segment in segments"
        :key="segment.utilityType"
        class="grid grid-cols-[minmax(0,5.25rem)_1fr_minmax(0,5.5rem)] items-center gap-3 py-3.5"
      >
        <!-- 왼쪽 · 요금 이름 + 기준 요금 -->
        <div class="min-w-0">
          <div class="flex items-center gap-1.5">
            <UtilityIcon :utility-type="segment.utilityType" xs />
            <span class="text-body-strong text-ink">{{
              formatUtilityType(segment.utilityType)
            }}</span>
          </div>
          <p v-if="segment.registered" class="text-caption-sm text-muted tabular-nums mt-1 mb-0">
            {{ formatWon(segment.baselineAmount) }}
          </p>
          <GpTag v-else small class="mt-1">미등록</GpTag>
        </div>

        <!-- 가운데 · 0·5·10·15 단계 바 -->
        <GpBandPicker
          v-if="segment.registered"
          :model-value="modelValue[segment.utilityType] ?? null"
          :tiers="tiers"
          :label="`${formatUtilityType(segment.utilityType)} 목표 절감률`"
          @update:model-value="setTier(segment.utilityType, $event)"
        />
        <p v-else class="text-caption text-muted m-0">
          {{ segment.unregisteredReason || '기준 사용량이 없어 목표를 세울 수 없어요' }}
        </p>

        <!-- 오른쪽 · 덜 내는 요금 (서버 preview 값) -->
        <div class="text-right" :class="loading ? 'opacity-60' : ''">
          <template v-if="!segment.registered">
            <span class="text-caption text-muted">합산 제외</span>
          </template>
          <template v-else-if="targetByUtility.get(segment.utilityType)">
            <p class="text-body-strong text-ink tabular-nums m-0">
              {{ formatWon(targetByUtility.get(segment.utilityType).expectedSaving) }}
            </p>
            <p class="text-caption-sm text-muted tabular-nums mt-0.5 mb-0">
              {{ targetUsageText(segment) }}
            </p>
          </template>
          <template v-else>
            <p class="text-body-strong text-icon-off m-0">0원</p>
          </template>
        </div>
      </div>
    </div>

    <EcoGoalSummary :combined="preview?.combined ?? null" :loading="loading" />
  </GpCard>
</template>
