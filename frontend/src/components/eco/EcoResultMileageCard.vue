<script setup>
/*
 * 적립된 마일리지 — WF-10 → WF-11 (B-5-02 · B-5-03)
 *
 * ── 돈의 3단계 중 ② ────────────────────────────────────────────────────────
 * `confirmedMileage` 는 **확정됐지만 아직 현금이 아니다.** `확인` 태그를 달고
 * "아직 현금이 아니에요" 를 함께 적는다(핵심 규칙 2). 실제 현금은 ③ 포켓 입금이다.
 *
 * ⚠️ **여기서 전환을 실행하지 않는다.** 이 카드는 적립 화면(WF-11)으로 가는 통로일 뿐이고,
 * 전환은 `POST /pocket/conversions` 라 포켓 도메인의 일이다. 동의 없이 전환이 시작되는
 * 길을 만들지 않는다(핵심 규칙 4).
 */
import GpCard from '@/components/ui/GpCard.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconChevronRight from '@/components/ui/icons/IconChevronRight.vue'
import IconCrown from '@/components/ui/icons/IconCrown.vue'
import { formatMileage } from '@/utils/format'

defineProps({
  mileage: { type: Number, required: true },
})
defineEmits(['open'])
</script>

<template>
  <GpCard>
    <button
      type="button"
      class="flex w-full cursor-pointer items-center gap-3 border-0 bg-transparent p-0 text-left"
      @click="$emit('open')"
    >
      <span
        class="bg-confirmed-bg text-confirmed flex size-10 flex-none items-center justify-center rounded-full"
        aria-hidden="true"
      >
        <IconCrown :size="20" />
      </span>

      <span class="min-w-0 flex-1">
        <span class="flex items-center gap-1.5">
          <GpTag tone="confirmed" small>확인</GpTag>
          <span class="text-list-title text-ink">
            적립된 마일리지 {{ formatMileage(mileage) }}
          </span>
        </span>
        <span class="text-caption text-muted mt-0.5 block">
          아직 현금이 아니에요 · 전환 신청하기
        </span>
      </span>

      <IconChevronRight :size="18" class="text-icon-off flex-none" />
    </button>
  </GpCard>
</template>
