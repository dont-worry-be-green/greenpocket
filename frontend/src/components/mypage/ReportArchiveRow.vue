<script setup>
/*
 * 리포트 한 줄 — MY-04 (E-2-01)
 *
 * ── 「보기」만 있고 「다운로드」는 없다 ────────────────────────────────────
 * 시안은 다운로드 아이콘을 그렸지만 `downloadable` 은 **항상 false** 다 — 월별 리포트
 * 자동 생성·다운로드(E-2-02)는 P2 다. 없는 기능을 아이콘으로 암시하지 않는다.
 *
 * ── 어디로 가는지는 서버가 정한다 ────────────────────────────────────────
 * `targetScreen` + `targetParams` 로 대상 화면을 찾아간다(MVP 범위가 "목록 + 대상 화면 라우팅"
 * 까지다). 갈 곳을 화면이 타입으로 추론하지 않는다 — 라우팅 규칙은 뷰의 표 하나에 모여 있다.
 * 갈 곳이 없는 타입이면 뷰가 `disabled` 를 넘겨 버튼을 잠근다.
 */
import GpButton from '@/components/ui/GpButton.vue'
import { formatDotDate, formatMonthOnly } from '@/utils/format'

defineProps({
  report: { type: Object, required: true },
  disabled: { type: Boolean, default: false },
})
defineEmits(['open'])
</script>

<template>
  <li class="bg-surface flex items-center gap-3 rounded-lg px-4 py-3.5">
    <!-- 왼쪽 월 배지. 연도는 그룹 제목이 이미 말하고 있다 -->
    <span class="text-list-title text-primary-on-soft w-9 flex-none tabular-nums">
      {{ formatMonthOnly(report.yearMonth) }}
    </span>

    <div class="min-w-0 flex-1">
      <p class="text-body-strong text-ink m-0 truncate">{{ report.type === 'MONTHLY_DIAGNOSIS' ? `${formatMonthOnly(report.yearMonth)} 월간 리포트` : report.title }}</p>
      <p class="text-caption text-muted mt-0.5 mb-0 tabular-nums">
        {{ formatDotDate(report.createdAt) }} 생성
      </p>
    </div>

    <GpButton variant="pill" size="pill" :disabled="disabled" @click="$emit('open', report)">
      보기
    </GpButton>
  </li>
</template>
