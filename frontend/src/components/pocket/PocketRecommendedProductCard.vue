<script setup>
/*
 * 포켓 홈 「추천 상품」 카드 (D-4-01 · 결정 C-31)
 *
 * `product` 는 GET /pocket/recommended-product 그대로다.
 *
 * ── 배지·버튼이 없다 ────────────────────────────────────────────────────
 * 카드 제목은 「최근 내역」과 같은 격의 h2 「추천 상품」이고, 상품 행(로고 · 이름 · 한 줄 · ›)이
 * **통째로 눌려** 상세(PK-09)로 간다. 신청 버튼은 상세에만 있다 — 홈에서 바로 외부로 보내면
 * 그린포켓이 가입을 처리하는 것처럼 보인다(D-4-01).
 * 조건 요약·정보 기준일·안내 문구도 상세에서만 보여 준다.
 *
 * 우대 미션 칩은 KB 상품 조건 안내이지 그린포켓 미션과 무관하다 — 그렇게 표현하지 않는다(결정 C-21).
 */
import BankLogo from '@/components/pocket/BankLogo.vue'
import GpCard from '@/components/ui/GpCard.vue'
import IconCaretRight from '@/components/ui/icons/IconCaretRight.vue'

defineProps({
  product: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  error: { type: Object, default: null },
})

defineEmits(['view', 'retry'])
</script>

<template>
  <GpCard title="추천 상품">
    <div
      v-if="loading"
      class="bg-skeleton h-16 animate-pulse rounded-lg"
      aria-label="추천 상품 불러오는 중"
    />

    <div v-else-if="error" class="flex min-h-12 items-center gap-3">
      <p class="text-caption text-muted m-0 min-w-0 flex-1">추천 상품을 불러오지 못했어요</p>
      <button
        type="button"
        class="text-caption text-ink-soft shrink-0 cursor-pointer border-0 bg-transparent p-2 font-semibold"
        @click="$emit('retry')"
      >
        다시 시도
      </button>
    </div>

    <template v-else-if="product">
      <button
        type="button"
        class="flex w-full cursor-pointer items-center gap-3 border-0 bg-transparent p-0 text-left"
        aria-label="추천 금융 상품"
        @click="$emit('view')"
      >
        <BankLogo bank-name="KB국민은행" :size="48" />
        <span class="min-w-0 flex-1">
          <strong class="text-section tracking-title text-ink block truncate">{{
            product.name
          }}</strong>
          <span
            v-if="product.recommendation?.title"
            class="text-caption text-muted mt-0.5 block truncate"
          >
            {{ product.recommendation.title }}
          </span>
        </span>
        <IconCaretRight :size="16" class="text-icon-off shrink-0" />
      </button>

      <ul
        v-if="product.preferentialMissions?.length"
        class="mt-3.5 flex list-none flex-wrap gap-1.5 p-0"
      >
        <li
          v-for="mission in product.preferentialMissions"
          :key="mission"
          class="bg-primary-bg text-primary-on-soft text-caption-sm rounded-full px-2.5 py-1 font-bold"
        >
          {{ mission }}
        </li>
      </ul>
    </template>
  </GpCard>
</template>
