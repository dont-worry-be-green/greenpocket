<script setup>
/*
 * WF-03 목표 미설정 메인 (B-1-05 · B-1-06 · B-1-07 · B-1-08)
 *
 * round 는 GET /eco/rounds/current 응답 그대로다. 필드명을 바꾸지 않는다.
 * baselineDescription("2024·2025년 4~9월 평균")은 서버가 문장으로 준다 — 화면에서 조립하지 않는다.
 *
 * 기준선은 진단 탭(지역 평균)이 아니라 **직전 2년 같은 기간 평균**이다(핵심 비즈니스 규칙 6).
 */
import { computed } from 'vue'

import GpButton from '@/components/ui/GpButton.vue'
import GpCard from '@/components/ui/GpCard.vue'
import IconCheck from '@/components/ui/icons/IconCheck.vue'
import UtilityIcon from './UtilityIcon.vue'
import {
  formatDateTime,
  formatPercent,
  formatUnit,
  formatUsage,
  formatUtilityType,
  formatWon,
} from '@/utils/format'

const props = defineProps({
  round: { type: Object, required: true },
  // GET /eco/home 의 links.movingNotice 를 그대로 받는다.
  // (근거는 GET /eco/status 의 ecoAddress.matchesProfile === false — 누리집 주소가 프로필과 다름)
  showMovingNotice: { type: Boolean, default: true },
  actionLabel: { type: String, default: '평가 기간 목표 정하기' },
})
defineEmits(['set-goal'])

// 등록된 요금만 그린다. 미등록 행은 기준값이 null 이라 금액·비중을 만들 수 없다(B-2-02 · 규칙 8)
const registeredItems = computed(() => props.round.baseline.items.filter((item) => item.registered))

// 총액 0 이면 비중 자체가 성립하지 않아 카드를 통째로 숨긴다 (B-1-06 예외)
const showShareCard = computed(() => props.round.baseline.totalAmount > 0)

const SERIES_COLOR = {
  ELECTRICITY: 'bg-chart-series-1',
  GAS: 'bg-chart-series-2',
  WATER: 'bg-chart-series-3',
}
</script>

<template>
  <div class="space-y-4">
    <div class="text-primary-on-soft flex items-center gap-2 px-3 pt-1">
      <span class="bg-primary text-primary-fg flex size-5 items-center justify-center rounded-full">
        <IconCheck :size="14" />
      </span>
      <p class="text-body-strong m-0">에코마일리지 연동완료</p>
    </div>

    <GpCard title="기준 사용량" badge="6개월">
      <p class="text-body-sm text-muted mt-0 mb-3">{{ round.baselineDescription }}</p>

      <div class="bg-surface-sub rounded-md mb-5 px-4 py-3">
        <p class="text-caption text-muted mt-0 mb-1">
          에코마일리지는 직전 2년 같은 기간 평균과 비교해요 ·
          {{ formatDateTime(round.baselineQueriedAt) }} 조회
        </p>
        <p class="text-caption text-muted m-0">
          작년에 이 집에 살지 않았다면 전입자 사용분이, 신축이면 비슷한 가구가 기준이에요
        </p>
      </div>

      <p class="text-display tabular-nums mt-0 mb-4">
        {{ formatWon(round.baseline.totalAmount) }}
      </p>

      <div v-if="showShareCard" class="mb-5">
        <div class="flex h-(--gp-bar-h) overflow-hidden rounded-sm" aria-hidden="true">
          <span
            v-for="item in registeredItems"
            :key="item.utilityType"
            :class="SERIES_COLOR[item.utilityType]"
            :style="{ width: `${item.shareRate}%` }"
          />
        </div>

        <ul class="mt-3 mb-0 flex list-none flex-wrap gap-x-4 gap-y-1 p-0">
          <li
            v-for="item in registeredItems"
            :key="item.utilityType"
            class="flex items-center gap-1.5"
          >
            <span
              class="size-2.5 rounded-full"
              :class="SERIES_COLOR[item.utilityType]"
              aria-hidden="true"
            />
            <span class="text-caption text-ink-soft">{{
              formatUtilityType(item.utilityType)
            }}</span>
            <span class="text-caption text-ink tabular-nums font-semibold">
              {{ formatPercent(item.shareRate) }}
            </span>
          </li>
        </ul>
      </div>

      <div class="border-divider border-t">
        <div
          v-for="item in registeredItems"
          :key="item.utilityType"
          class="flex min-h-(--gp-row-h) items-center gap-3"
        >
          <UtilityIcon :utility-type="item.utilityType" />
          <p class="text-list-title m-0 min-w-0 flex-1">
            {{ formatUtilityType(item.utilityType) }}
          </p>
          <div class="text-right">
            <p class="text-body-strong tabular-nums m-0">{{ formatWon(item.amount) }}</p>
            <p class="text-caption text-muted tabular-nums m-0">
              {{ formatUsage(item.usage, 0, formatUnit(item.usageUnit)) }}
            </p>
          </div>
        </div>
      </div>
    </GpCard>

    <GpButton @click="$emit('set-goal')">{{ actionLabel }}</GpButton>
  </div>
</template>
