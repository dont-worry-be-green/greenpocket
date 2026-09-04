<script setup>
/*
 * WF-07 남은 달 처방 (B-4-08)
 *
 * `prescription` 은 GET /eco/monthly-report 의 `prescription` 그대로다.
 *
 * ⚠️ `requiredRate` 는 **증감이 아니다.** 「남은 달마다 이만큼 줄여야 한다」는 목표치라
 * GpDelta 에 넘기면 "줄었어요" 가 붙어 이미 줄인 것처럼 읽힌다 → formatPercent 로 그린다.
 *
 * ⚠️ `remainingMonths === 0` 이면 서버가 `requiredRate: null` 을 준다(0 나눗셈 금지).
 * 남은 달이 없으니 처방이 아니라 결과를 기다리는 안내를 띄운다.
 *
 * `requiredByUtility[].assumption` 은 **가정을 밝히는 문장**이다(핵심 규칙 7).
 * "전기 16.2%" 만 보이면 나머지 요금이 지금 속도를 유지한다는 전제가 숨는다.
 */
import { computed } from 'vue'

import GpButton from '@/components/ui/GpButton.vue'
import GpCard from '@/components/ui/GpCard.vue'
import GpTag from '@/components/ui/GpTag.vue'
import { formatPercent, formatUtilityType } from '@/utils/format'

const props = defineProps({
  prescription: { type: Object, required: true },
})
defineEmits(['adjust'])

const hasRemaining = computed(
  () => props.prescription.remainingMonths > 0 && props.prescription.requiredRate !== null,
)

const monthsLabel = computed(() =>
  (props.prescription.remainingMonthLabels ?? []).map((month) => `${month}월`).join('·'),
)

const byUtility = computed(() => props.prescription.requiredByUtility ?? [])

const adjustLabel = computed(() =>
  props.prescription.adjustTargetUtility
    ? `${formatUtilityType(props.prescription.adjustTargetUtility)} 실천 다시 고르기`
    : '실천 다시 고르기',
)
</script>

<template>
  <GpCard title="남은 달에 필요한 만큼">
    <template v-if="hasRemaining">
      <p class="text-body text-ink-soft mt-0 mb-3">
        남은 {{ monthsLabel }} 두 달 동안 달마다
        <span class="text-ink font-semibold">{{ formatPercent(prescription.requiredRate) }}</span>
        씩 줄이면 목표에 닿아요.
      </p>

      <!-- 요금 하나가 혼자 떠안는 몫. 탄소 비중으로 나눈 값이라 전체보다 크다 -->
      <div
        v-for="row in byUtility"
        :key="row.utilityType"
        class="bg-surface-sub mb-2 rounded-md p-3 last:mb-0"
      >
        <div class="flex items-baseline justify-between gap-2">
          <span class="text-list-title">{{ formatUtilityType(row.utilityType) }} 혼자 줄인다면</span>
          <span class="text-list-title">{{ formatPercent(row.requiredRate) }}</span>
        </div>
        <p v-if="row.assumption" class="text-caption text-muted mt-1 mb-0">{{ row.assumption }}</p>
      </div>

      <div class="border-divider mt-4 border-t pt-3">
        <div class="flex items-baseline justify-between gap-2">
          <span class="text-body text-ink-soft">지금 고른 실천 합계</span>
          <span class="text-list-title">{{ formatPercent(prescription.selectedMissionRate) }}</span>
        </div>
        <p class="text-caption mt-2 mb-0" :class="prescription.achievable ? 'text-on-positive' : 'text-muted'">
          <template v-if="prescription.achievable">
            지금 고른 실천을 지키면 남은 몫을 덮을 수 있어요
          </template>
          <template v-else>
            지금 고른 실천만으로는 모자라요. 실천을 더 고르면 남은 몫을 덮을 수 있어요
          </template>
        </p>
        <p class="mt-2 mb-0">
          <GpTag tone="estimated" small>예상</GpTag>
        </p>
      </div>

      <div class="mt-4">
        <GpButton variant="pill" size="pill" @click="$emit('adjust', prescription.adjustTargetUtility)">
          {{ adjustLabel }}
        </GpButton>
      </div>
    </template>

    <!-- 남은 달이 없다. 처방할 것이 없으니 숫자를 만들지 않는다 (핵심 규칙 8) -->
    <p v-else class="text-body text-ink-soft m-0">
      평가 기간이 끝났어요. 확정 결과가 나오면 알려드릴게요.
    </p>
  </GpCard>
</template>
