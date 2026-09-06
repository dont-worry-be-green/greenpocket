<script setup>
/*
 * 실천 항목 한 줄 (C-1-03 · C-2-03 · BN-01 대표 항목 · BN-02 목록 공용)
 *
 * ── 건수 단위를 `rewardUnit` 으로 쓰지 않는다 ────────────────────────────
 * 시안은 행마다 '24건 · 8회 · 3회' 로 갈리지만 서버가 주는 것은 `monthCount`(숫자) 하나뿐이다.
 * `rewardUnit` 은 **단가의 단위**(10원/`건`)라 건수에 붙이면 미래세대실천행동에서
 * '이번 달 0운영계획' 이 된다. 건수는 '건' 으로 통일한다.
 *
 * ── 0건은 빈칸이 아니라 문장이다 ────────────────────────────────────────
 * C-2-03: 실적이 없어도 17개를 전부 내려주고 `monthCount: 0` 은 '아직 실천하지 않았어요' 로
 * 보여준다. 숫자 0 만 두면 고장난 것처럼 읽힌다.
 *
 * `monthlyCapAmount` · `annualCapAmount` 가 null 인 항목은 상한이 아직 확정되지 않은 것이라
 * **표시하지 않는다**(api-spec 12.3 · 결정 10). `capReached` 일 때만 라벨을 단다.
 *
 * ── compact 는 BN-01 용이다 ─────────────────────────────────────────────
 * 미참여 화면의 `featuredItems[]` 에는 `monthCount` 자체가 없다(api-spec 12.1). 그대로 그리면
 * 아직 시작도 안 한 사람에게 '아직 실천하지 않았어요' 를 네 줄 보여주게 된다. 건수와 화살표를
 * 빼고 단가만 남긴다 — 누를 상세도 아직 의미가 없어 버튼이 아니라 상자로 그린다.
 */
import GreenlifeItemIcon from './GreenlifeItemIcon.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconChevronRight from '@/components/ui/icons/IconChevronRight.vue'
import { formatUnitPrice } from '@/utils/format'

const props = defineProps({
  item: { type: Object, required: true },
  compact: { type: Boolean, default: false },
})
defineEmits(['select'])

const countText = () =>
  Number(props.item.monthCount) > 0 ? `이번 달 ${props.item.monthCount}건` : '아직 실천하지 않았어요'
</script>

<template>
  <component
    :is="compact ? 'div' : 'button'"
    :type="compact ? null : 'button'"
    class="flex w-full items-center gap-3 text-left"
    :class="compact ? 'border-divider rounded-md border px-3 py-2.5' : 'py-3'"
    @click="compact || $emit('select', item.itemId)"
  >
    <GreenlifeItemIcon :icon-key="item.iconKey" />

    <span class="text-body-strong text-ink min-w-0 flex-1 truncate">{{ item.name }}</span>

    <GpTag v-if="item.capReached" tone="sub" small>상한 도달</GpTag>

    <span class="text-label text-primary flex-none tabular-nums">
      {{ formatUnitPrice(item.unitPrice, item.rewardUnit) }}
    </span>

    <template v-if="!compact">
      <span class="text-caption text-muted flex-none tabular-nums">{{ countText() }}</span>
      <IconChevronRight :size="16" class="text-muted flex-none" />
    </template>
  </component>
</template>
