<script setup>
/*
 * 실천 항목 한 줄 (C-1-03 · C-2-03 · BN-01 대표 항목 · BN-02 목록 공용 · 결정 C-32)
 *
 * 아이콘 타일 · 이름/단가 · 이번 달 건수, 셋뿐이다. 셰브론은 없다 — 행 전체가 눌리고
 * 오른쪽 건수가 「더 있다」를 이미 말한다(포켓 최근 내역과 같은 규칙).
 *
 * ── 오른쪽은 이번 달 건수다 ─────────────────────────────────────────────
 * 명세 C-2-03 「단가와 이번 달 횟수」 그대로. `monthCount` 는 DECIMAL(3.000)로 오므로 반올림해
 * 정수로 보이고 단위는 `rewardUnit`(건·개·회)을 쓴다. 0이면 회색 「0건」 — 문장(「아직 실천하지
 * 않았어요」)은 행 폭을 잡아먹어 이름이 잘린다. 미실천은 아이콘 타일도 회색으로 떨어진다.
 * 금액은 상세(BN-03)가 「24건 · 240원 적립 예정」으로 보여 준다.
 *
 * `capReached` 일 때만 「상한 도달」 라벨. `monthlyCapAmount` 등 상한 값은 null 이면 표시하지 않는다(결정 10).
 *
 * ── compact 는 BN-01 용이다 ─────────────────────────────────────────────
 * 미참여 화면의 `featuredItems[]` 에는 실적 필드가 없다(api-spec 12.1). 건수 자리를 비우고
 * 단가만 남긴다. 누를 상세도 아직 의미가 없어 버튼이 아니라 상자로 그린다.
 */
import GreenlifeItemIcon from './GreenlifeItemIcon.vue'
import GpTag from '@/components/ui/GpTag.vue'
import { formatNumber } from '@/utils/format'

const props = defineProps({
  item: { type: Object, required: true },
  compact: { type: Boolean, default: false },
})
defineEmits(['select'])

const count = () => Math.round(Number(props.item.monthCount ?? 0))
const hasCount = () => count() > 0
</script>

<template>
  <component
    :is="compact ? 'div' : 'button'"
    :type="compact ? null : 'button'"
    class="flex min-h-[60px] w-full items-center gap-3 border-0 bg-transparent px-0 py-2.5 text-left"
    :class="compact ? '' : 'cursor-pointer'"
    @click="compact || $emit('select', item.itemId)"
  >
    <GreenlifeItemIcon :icon-key="item.iconKey" :muted="!compact && !hasCount()" />

    <span class="min-w-0 flex-1">
      <span class="text-body-strong text-ink block truncate">{{ item.name }}</span>
      <span class="text-caption mt-0.5 block tabular-nums">
        <span class="text-primary-on-soft font-bold">{{ formatNumber(item.unitPrice) }}</span>
        <span class="text-muted">원/{{ item.rewardUnit }}</span>
      </span>
    </span>

    <GpTag v-if="item.capReached" tone="sub" small>상한 도달</GpTag>

    <span
      v-if="!compact"
      class="shrink-0 tabular-nums"
      :class="hasCount() ? 'text-ink' : 'text-muted'"
    >
      <span class="text-list-title">{{ count() }}</span>
      <span class="text-caption font-medium" :class="hasCount() ? 'text-muted' : ''">{{
        hasCount() ? item.rewardUnit : '건'
      }}</span>
    </span>
  </component>
</template>
