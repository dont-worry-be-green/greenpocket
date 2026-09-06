<script setup>
/*
 * 프로그램 소개 카드 (C-1-01 · BN-01) — `GET /greenlife/status` 의 `programInfo` 를 그린다.
 *
 * ── 「미참여」 배지에 돈 색을 쓰지 않는다 ───────────────────────────────
 * 시안은 주황이지만 `confirmed`(앰버)·`estimated`(회색 아웃라인)는 COM-06 의 **돈 3단계** 라벨
 * 색이다. 참여 여부는 돈이 아니므로 그 체계를 빌려오면 뜻이 섞인다. `negative`(빨강)는 오류로
 * 읽혀 과하다 — 아직 시작하지 않았을 뿐이다. 중립인 `sub` 로 둔다.
 *
 * ── 일러스트 ────────────────────────────────────────────────────────────
 * 시안의 그림은 아직 에셋이 없다. 빈 상자를 남기면 깨진 것처럼 보이므로 브랜드 아이콘으로
 * 자리를 채워 둔다. `#illustration` 슬롯에 넣으면 그대로 대체된다.
 */
import GpCard from '@/components/ui/GpCard.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconPlant from '@/components/ui/icons/IconPlant.vue'
import { formatWon } from '@/utils/format'

defineProps({
  programInfo: { type: Object, required: true },
})
</script>

<template>
  <GpCard>
    <div class="flex items-start gap-4">
      <span
        class="bg-primary-bg text-primary flex size-20 flex-none items-center justify-center rounded-lg"
      >
        <slot name="illustration">
          <IconPlant :size="40" />
        </slot>
      </span>

      <div class="min-w-0 flex-1">
        <div class="flex items-start justify-between gap-2">
          <h2 class="text-section tracking-display text-ink m-0">{{ programInfo.name }}</h2>
          <GpTag tone="sub" small>미참여</GpTag>
        </div>

        <p class="text-body-sm text-ink-soft mt-2 mb-0">
          일상 속 친환경 활동으로 포인트를 받을 수 있어요
        </p>

        <p class="text-caption text-muted mt-3 mb-0">
          {{ programInfo.itemCount }}가지 실천 · 연간 최대
          <span class="text-body-strong text-ink">{{ formatWon(programInfo.annualLimit) }}</span>
        </p>
      </div>
    </div>
  </GpCard>
</template>
