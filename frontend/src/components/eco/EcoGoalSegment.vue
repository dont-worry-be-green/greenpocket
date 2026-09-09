<script setup>
/*
 * 미등록 요금 안내 — WF-05 (B-2-02 예외 · B-2-06)
 *
 * `segment` 는 `GET /eco/rounds/{roundId}/goal-form` 의 `segments[]` 중 `registered: false` 인 항목이다.
 * 미션 탭에서 그 요금을 골랐을 때 미션 목록 위에 놓인다. 구간 선택은 목표 카드(EcoGoalPlanner)로
 * 옮겨 갔고, 거기서는 「미등록 · 합산 제외」 한 줄만 두므로 사유와 등록 안내는 여기서 보여준다.
 *
 * 기준 사용량이 없어 목표 사용량을 만들 수 없고 마일리지 합산에서도 빠진다(`excludedFromCombine`).
 * 사유 문장(`unregisteredReason`)은 DB 에 문장으로 저장된 값이라 서버가 준 것을 그대로 쓴다 —
 * 화면에서 조립하지 않는다. **미션 목록은 그래도 보여준다.** 마일리지에 못 들어갈 뿐 관리비는 줄기 때문이다.
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import IconWarning from '@/components/ui/icons/IconWarning.vue'
import { formatUtilityType } from '@/utils/format'

const props = defineProps({
  segment: { type: Object, required: true },
})

const name = computed(() => formatUtilityType(props.segment.utilityType))
</script>

<template>
  <GpCard tone="sub">
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
</template>
