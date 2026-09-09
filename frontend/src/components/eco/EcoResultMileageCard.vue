<script setup>
/*
 * 적립된 마일리지 — WF-10 → WF-11 (B-5-02 · B-5-03)
 *
 * ── 돈의 3단계 중 ② ────────────────────────────────────────────────────────
 * `confirmedMileage` 는 **확정됐지만 아직 현금이 아니다.** 실제 현금은 ③ 포켓 입금이다.
 * ⚠️ 왕관 아이콘 · 「확인」 태그 · 「아직 현금이 아니에요 · 전환 신청하기」 캡션은 **수현 결정으로
 * 뺐다**(2026-09-10 · 결정 C-39). 행 하나에 「적립된 마일리지 30,000M ›」만 남는다 — ②라는
 * 설명은 이 행이 데려가는 적립 화면(WF-11)의 「아직 현금이 아니에요」 노티스가 맡는다.
 * 셰브론은 채운 삼각형이 아니라 얇은 `>`(`IconCaretRight`, regular)다 — 포켓 잔액 카드와 같다.
 *
 * ⚠️ **여기서 전환을 실행하지 않는다.** 이 카드는 적립 화면(WF-11)으로 가는 통로일 뿐이고,
 * 전환은 `POST /pocket/conversions` 라 포켓 도메인의 일이다. 동의 없이 전환이 시작되는
 * 길을 만들지 않는다(핵심 규칙 4).
 */
import GpCard from '@/components/ui/GpCard.vue'
import IconCaretRight from '@/components/ui/icons/IconCaretRight.vue'
import { formatMileage } from '@/utils/format'

defineProps({
  mileage: { type: Number, required: true },
  interactive: { type: Boolean, default: true },
})
defineEmits(['open'])
</script>

<template>
  <GpCard>
    <component
      :is="interactive ? 'button' : 'div'"
      :type="interactive ? 'button' : undefined"
      class="flex w-full items-center gap-3 border-0 bg-transparent p-0 text-left"
      :class="interactive ? 'cursor-pointer' : ''"
      @click="interactive && $emit('open')"
    >
      <span class="text-list-title text-ink min-w-0 flex-1 tabular-nums">
        적립된 마일리지 {{ formatMileage(mileage) }}
      </span>

      <IconCaretRight v-if="interactive" :size="16" class="text-icon-off flex-none" />
    </component>
  </GpCard>
</template>
