<script setup>
/*
 * 실천 항목 한 줄 (C-1-03 · C-2-03 · BN-01 대표 항목 · BN-02 목록 공용)
 *
 * ── 오른쪽은 건수가 아니라 이번 달 금액이다 ──────────────────────────────
 * `monthAmount`(= 건수 × 단가, 상한 적용)를 쓴다. 셋 다 서버가 주는 값이지만 금액을 고른 이유:
 *   ① **원화 우선**(핵심 규칙 1). 이 서비스가 먼저 보여주기로 한 것은 원화 영향이다
 *   ② 바로 위 「이번 달 적립 예정 5,540원」과 **단위가 같아** 행을 더하면 그 합계가 된다
 *   ③ 실적이 없을 때 '0원' 한 마디로 끝난다 — '아직 실천하지 않았어요' 는 열일곱 줄 중
 *      대부분에 붙어 정작 실천한 줄을 덮었다
 * 건수는 사라지지 않는다. 항목을 누르면 상세(BN-03)가 '24건 · 240원 적립 예정' 으로 보여준다.
 *
 * ⚠️ `monthCount` 를 쓰지 않는 이유가 하나 더 있다 — `DECIMAL` 이라 3.000 처럼 온다.
 * `monthAmount` 는 정수(원)다.
 *
 * ── 강조는 단가가 아니라 내 실적이다 ────────────────────────────────────
 * 단가(10원/건)는 제도 정보고 금액은 **내가 이번 달에 쌓은 것**이다. 시안은 단가를 초록으로
 * 강조했지만, 둘 다 원화가 된 이상 눈이 먼저 가야 할 쪽은 내 실적이다.
 * 0원 줄은 흐리게 두어 실천한 줄이 먼저 보이게 한다.
 *
 * `monthlyCapAmount` · `annualCapAmount` 가 null 인 항목은 상한이 아직 확정되지 않은 것이라
 * **표시하지 않는다**(api-spec 12.3 · 결정 10). `capReached` 일 때만 라벨을 단다.
 *
 * ── compact 는 BN-01 용이다 ─────────────────────────────────────────────
 * 미참여 화면의 `featuredItems[]` 에는 실적 필드(`monthAmount`)가 없다(api-spec 12.1).
 * 그대로 그리면 아직 시작도 안 한 사람에게 '0원' 을 네 줄 보여주게 된다. 금액과 화살표를
 * 빼고 단가만 남긴다 — 누를 상세도 아직 의미가 없어 버튼이 아니라 상자로 그린다.
 */
import GreenlifeItemIcon from './GreenlifeItemIcon.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconChevronRight from '@/components/ui/icons/IconChevronRight.vue'
import { formatUnitPrice, formatWon } from '@/utils/format'

const props = defineProps({
  item: { type: Object, required: true },
  compact: { type: Boolean, default: false },
})
defineEmits(['select'])

const hasAmount = () => Number(props.item.monthAmount) > 0
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

    <!-- BN-01(compact)에는 실적이 없어 단가가 유일한 숫자다. 거기서는 초록으로 남긴다 -->
    <span
      class="flex-none tabular-nums"
      :class="compact ? 'text-label text-primary' : 'text-caption text-muted'"
    >
      {{ formatUnitPrice(item.unitPrice, item.rewardUnit) }}
    </span>

    <template v-if="!compact">
      <span
        class="text-label flex-none tabular-nums"
        :class="hasAmount() ? 'text-primary' : 'text-muted font-normal'"
      >
        {{ formatWon(item.monthAmount) }}
      </span>
      <IconChevronRight :size="16" class="text-icon-off flex-none" />
    </template>
  </component>
</template>
