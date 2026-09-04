<script setup>
/*
 * WF-02 사용량 불러오는 중 (B-1-03)
 *
 * utilities 는 GET /eco/link/{linkJobId} 의 utilityStatus 형태다 —
 * [{ utilityType, status }] 3건 고정(B-2-02 세그먼트 3개 고정 규칙).
 * status 는 PENDING · RUNNING · SUCCEEDED.
 *
 * TIMEOUT · PARTIAL 분기는 폴링을 붙일 때 함께 만든다. 시안에 없고,
 * 재시도 버튼은 실제 jobId 가 있어야 의미가 있다(B-1-03 예외).
 */
import GpCard from '@/components/ui/GpCard.vue'
import IconCheck from '@/components/ui/icons/IconCheck.vue'
import UtilityIcon from './UtilityIcon.vue'
import { formatUtilityType } from '@/utils/format'

defineProps({
  utilities: { type: Array, required: true },
})

const STATUS_LABEL = {
  PENDING: '기다리는 중',
  RUNNING: '불러오는 중',
  SUCCEEDED: '불러왔어요',
}
</script>

<template>
  <div class="space-y-4">
    <GpCard>
      <div
        v-for="(item, index) in utilities"
        :key="item.utilityType"
        class="flex min-h-(--gp-row-h) items-center gap-3"
        :class="{ 'border-divider border-t': index > 0 }"
      >
        <UtilityIcon :utility-type="item.utilityType" />
        <p class="text-list-title m-0 min-w-0 flex-1">{{ formatUtilityType(item.utilityType) }}</p>
        <p class="text-body-sm text-muted m-0">{{ STATUS_LABEL[item.status] }}</p>

        <!-- 완료가 아니면 체크를 그리지 않는다 (핵심 비즈니스 규칙 11) -->
        <span v-if="item.status === 'SUCCEEDED'" class="text-primary flex size-5 justify-center">
          <IconCheck :size="18" />
        </span>
        <span
          v-else
          class="border-track border-t-primary size-5 animate-spin rounded-full border-2"
          :aria-label="STATUS_LABEL[item.status]"
          role="status"
        />
      </div>
    </GpCard>

    <GpCard tone="sub">
      <p class="text-caption text-muted m-0">
        에코마일리지 누리집에서 작년 같은 달 사용량을 가져오고 있어요. 20초쯤 걸려요.
      </p>
    </GpCard>
  </div>
</template>
