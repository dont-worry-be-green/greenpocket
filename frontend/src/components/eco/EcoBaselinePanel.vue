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
})
defineEmits(['set-goal'])

// 등록된 요금만 그린다. 미등록 행은 기준값이 null 이라 금액·비중을 만들 수 없다(B-2-02 · 규칙 8)
const registeredItems = computed(() => props.round.baseline.items.filter((item) => item.registered))

// 총액 0 이면 비중 자체가 성립하지 않아 카드를 통째로 숨긴다 (B-1-06 예외)
const showShareCard = computed(() => props.round.baseline.totalAmount > 0)

const largestShareLabel = computed(() =>
  formatUtilityType(props.round.baseline.largestShareUtility),
)

const SERIES_COLOR = {
  ELECTRICITY: 'bg-chart-series-1',
  GAS: 'bg-chart-series-2',
  WATER: 'bg-chart-series-3',
}
</script>

<template>
  <div class="space-y-5">
    <GpCard title="기준 사용량" badge="6개월">
      <p class="text-caption text-muted mt-0 mb-1">{{ round.baselineDescription }}</p>
      <p class="text-amount-hero tabular-nums mt-0 mb-4">
        {{ formatWon(round.baseline.totalAmount) }}
      </p>

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

      <p class="text-caption text-muted border-divider mt-0 mb-1 border-t pt-4">
        에코마일리지는 직전 2년 같은 기간 평균과 비교해요 ·
        {{ formatDateTime(round.baselineQueriedAt) }} 조회
      </p>
      <p class="text-caption text-muted m-0">
        작년에 이 집에 살지 않았다면 전입자 사용분이, 신축이면 비슷한 가구가 기준이에요
      </p>
    </GpCard>

    <GpCard v-if="showShareCard" title="어디부터 줄일까요" caption="기준 요금에서 차지한 몫이에요">
      <div class="flex h-(--gp-bar-h) overflow-hidden rounded-sm" aria-hidden="true">
        <span
          v-for="item in registeredItems"
          :key="item.utilityType"
          :class="SERIES_COLOR[item.utilityType]"
          :style="{ width: `${item.shareRate}%` }"
        />
      </div>

      <ul class="mt-3 mb-0 flex list-none flex-wrap gap-x-4 gap-y-1 p-0">
        <li v-for="item in registeredItems" :key="item.utilityType" class="flex items-center gap-1.5">
          <span class="size-2.5 rounded-full" :class="SERIES_COLOR[item.utilityType]" aria-hidden="true" />
          <span class="text-caption text-ink-soft">{{ formatUtilityType(item.utilityType) }}</span>
          <span class="text-caption text-ink-soft tabular-nums font-semibold">
            {{ formatPercent(item.shareRate) }}
          </span>
        </li>
      </ul>

      <p class="text-body text-ink-soft border-divider mt-4 mb-0 border-t pt-4">
        <strong class="text-ink font-semibold">{{ largestShareLabel }}가 가장 커요.</strong><br />
        같은 %를 줄여도 {{ largestShareLabel }}에서 줄이는 쪽이 요금이 더 많이 내려가요.
      </p>
    </GpCard>

    <!-- B-1-08 이사 안내 -->
    <p v-if="showMovingNotice" class="text-caption text-muted m-0 px-1">
      이사했다면 <RouterLink to="/mypage" class="text-primary-on-soft underline">마이페이지</RouterLink>에서
      주소를 바꿔주세요. 바꾸지 않으면 지금 살지 않는 집의 사용량과 비교돼요.
    </p>

    <GpButton @click="$emit('set-goal')">평가 기간 목표 정하기</GpButton>
  </div>
</template>
