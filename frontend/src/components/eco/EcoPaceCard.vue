<script setup>
/*
 * WF-06 감축률 카드 (B-4-03 · B-4-06 요약) — 홈 확정 시안(design-system.md 9-2).
 *
 *   progress     ← GET /eco/home 의 `progress` 그대로. cumulativeRate · currentTier · targetTier · tiers[]
 *   goal         ← GET /eco/home 의 `goal`. expectedMileage 만 쓴다
 *   prescription ← GET /eco/monthly-report 의 `prescription`. requiredRate · achievable
 *   period · remainingMonths ← 홈의 `header`
 *
 * ── 화면이 하지 않는 것 ─────────────────────────────────────────────────
 * **구간·페이스를 감축률 숫자로 판정하지 않는다.** 칸 위치는 `currentTier · targetTier`,
 * 회복 가능 여부는 `prescription.achievable` 이다(ecoPace.js). 서버가 판정한 값을 읽기만 한다.
 * 핸들은 칸의 가운데에 선다 — 감축률을 칸 안에서 보간해 위치를 만들지 않는다.
 *
 * ── 이름은 데이터를 따른다 ───────────────────────────────────────────────
 * 시안은 「평균 감축률」이지만 홈 응답의 값은 `cumulativeRate`(탄소 가중 누적)다.
 * 회복 축(월 감축률 평균)과는 편중된 달에 1.75%p 까지 벌어진다. 있는 값을 있는 이름으로 부른다 —
 * 「누적 감축률」. 평균이 필요하면 BE 에 필드를 요청한다(notes/PR124-검토.md).
 *
 * ── 구간 바는 지급 구간과 같은 4칸이다 ─────────────────────────────────
 * 0~5 / 5~10 / 10~15 / 15%+. 목표는 배지가 아니라 목표 칸 위 잉크색 마커 + 칸 링이다.
 * 고정값(목표)에 상태색을 쓰지 않는다 — 배지가 `--tone` 을 따라 같이 변하면 목표가 평가 대상처럼 읽힌다.
 * ⚠️ 기능명세 B-4-03 은 「구간 계단 3칸」이다. 확정 시안이 4칸 바로 바꿨으니 명세(xlsx) 갱신이 필요하다.
 *
 * ── 홈 문구는 요구 감축률을 말하고 지급액을 약속하지 않는다 ─────────────────
 * 상태 문장은 `requiredRate`(남은 달 필요 감축률)로 말한다. 예상 적립(「목표 달성 시 예상 적립」 등 페이스별 캡션)은
 * 지급 축(`expectedMileage`)이라 「예상」 라벨을 이름에 품는다(핵심 규칙 2 · COM-06).
 */
import { computed } from 'vue'

import ecoMark from '@/assets/eco-mileage-mark.png'
import IconCrown from '@/components/ui/icons/IconCrown.vue'
import IconSealCheck from '@/components/ui/icons/IconSealCheck.vue'
import IconWarning from '@/components/ui/icons/IconWarning.vue'
import { formatMileage, formatPercent } from '@/utils/format'
import { SEGMENTS, derivePace, segmentOf } from './ecoPace'

const props = defineProps({
  progress: { type: Object, required: true },
  goal: { type: Object, default: null },
  prescription: { type: Object, default: null },
  /** '2026.04~09' — 홈 `header` 를 뷰가 조립해 넘긴다 */
  period: { type: String, default: '' },
  remainingMonths: { type: Number, default: null },
})
// 하단 링크는 둘뿐이다 — 미션 다시 고르기는 목표 관리(WF-04) 안에서 한다(2026-09-09 수현, WF-08 폐지)
// `demo-result` 는 발표용 임시 입구다(2026-09-10 수현) — 아래 「예상 적립」 캡션 옆 작은 버튼. 서버 상태와 무관하게
// 홈 뷰가 WF-09 결산 모달을 띄운다. 결산이 실제로 도는 흐름이 붙으면 버튼·emit 을 같이 지운다.
defineEmits(['goal', 'report', 'demo-result'])

/*
 * 페이스별 색. 클래스 문자열을 통째로 적어야 Tailwind 가 스캔한다 — 조립하지 않는다.
 * 채움색은 36px 수치·바·핸들에만 쓴다. 16px 이하 글자엔 near·on 이 4.5:1 에 못 미친다.
 */
const TONE = {
  behind: {
    text: 'text-pace-behind',
    fill: 'bg-pace-behind',
    soft: 'bg-pace-behind-soft',
    icon: IconWarning,
  },
  near: {
    text: 'text-pace-near',
    fill: 'bg-pace-near',
    soft: 'bg-pace-near-soft',
    icon: IconWarning,
  },
  on: { text: 'text-pace-on', fill: 'bg-pace-on', soft: 'bg-pace-on-soft', icon: IconSealCheck },
  ahead: {
    text: 'text-pace-ahead',
    fill: 'bg-pace-ahead',
    soft: 'bg-pace-ahead-soft',
    icon: IconCrown,
  },
}

const pace = computed(() => derivePace(props.progress, props.prescription))
const tone = computed(() => TONE[pace.value])

const currentIndex = computed(() => segmentOf(props.progress.currentTier))
const targetIndex = computed(() => segmentOf(props.progress.targetTier))

/** '9.0%' 를 숫자와 단위로 나눠 크기를 달리 준다 */
const rateParts = computed(() => {
  const text = formatPercent(props.progress.cumulativeRate)
  return text.endsWith('%') ? [text.slice(0, -1), '%'] : [text, '']
})

function segmentClass(index) {
  if (index < currentIndex.value) return tone.value.soft
  if (index === currentIndex.value) return tone.value.fill
  return 'bg-track'
}

function labelClass(index) {
  // 현재 칸이 목표 칸이면 현재 표시가 이긴다
  if (index === currentIndex.value) return `${tone.value.text} font-extrabold`
  if (index === targetIndex.value) return 'text-ink font-bold'
  return ''
}

/** 상태 문장 — 서버 값이 없으면 숫자 없이 말한다. 만들어 넣지 않는다(핵심 규칙 8) */
const status = computed(() => {
  const months = props.remainingMonths
  const required = props.prescription?.requiredRate
  const monthsText = months ? `남은 ${months}개월` : '남은 기간'
  const requiredText = required != null ? `${monthsText} 매달 ${formatPercent(required)} 필요` : ''

  switch (pace.value) {
    case 'ahead': {
      const current = props.progress.tiers?.find((row) => row.state === 'CURRENT')
      return {
        title: '목표를 초과 달성 중이에요',
        sub: current ? `지금 구간이면 ${formatMileage(current.mileage)} 기대 중` : '',
      }
    }
    case 'on':
      return { title: '이대로면 목표 달성 가능해요', sub: '지금 흐름을 유지하면 돼요' }
    case 'behind':
      return {
        title: '이번 목표는 더 이상 어려워요',
        sub: requiredText
          ? `${requiredText} · 고른 실천으로는 닿기 어려워요`
          : `${monthsText}로는 닿기 어려워요`,
      }
    default:
      return {
        title: '아직 부족하지만 회복 가능해요',
        sub: requiredText || `${monthsText} 동안 더 줄여야 해요`,
      }
  }
})

/*
 * 예상 적립(레퍼런스 home_final_reference).
 *   near · on  → 목표 구간 마일리지(`goal.expectedMileage`) — 목표대로 가면 받는 것
 *   ahead      → 지금 구간 마일리지 — 목표를 넘겼으니 목표값이 아니라 지금 구간이다
 *   behind     → 지금 구간 마일리지 — 목표는 닿기 어려우니 목표값을 약속하지 않는다 (0M 이면 0M)
 * 둘 다 서버 `tiers[]` 의 CURRENT 행에서 읽는다. CURRENT 가 없으면(5% 미만) 0 이다.
 */
const expectedMileage = computed(() => {
  if (pace.value === 'near' || pace.value === 'on') return props.goal?.expectedMileage ?? null
  const current = props.progress.tiers?.find((row) => row.state === 'CURRENT')
  return current?.mileage ?? 0
})
/*
 * 캡션은 숫자의 조건을 말한다(2026-09-09 수현). 「예상 에코마일리지 적립」만 적으면 누적 0% 옆의
 * 10,000M 이 약속처럼 읽힌다 — near 는 목표를 채웠을 때, on 은 이대로 갈 때, ahead·behind 는 지금 구간이다.
 */
const mileageCaption = computed(() => {
  switch (pace.value) {
    case 'on':
      return '이대로면 예상 적립'
    case 'ahead':
    case 'behind':
      return '지금 구간 예상 적립'
    default:
      return '목표 달성 시 예상 적립'
  }
})
const mileageParts = computed(() => {
  const text = formatMileage(expectedMileage.value)
  return text.endsWith('M') ? [text.slice(0, -1), 'M'] : [text, '']
})
</script>

<template>
  <section class="bg-surface rounded-card shadow-card px-4 pt-[18px] pb-1.5">
    <div class="flex items-center justify-between gap-2.5">
      <h2 class="text-section tracking-title text-ink m-0">누적 감축률</h2>
      <span
        v-if="period"
        class="bg-surface-sub text-caption-sm text-ink-soft rounded-full px-2.5 py-[5px] font-bold tabular-nums"
      >
        {{ period }}
      </span>
    </div>

    <p class="text-display-lg tracking-display mt-2 mb-0 tabular-nums" :class="tone.text">
      {{ rateParts[0] }}<span class="text-section font-extrabold">{{ rateParts[1] }}</span>
    </p>

    <!-- 목표 마커. 고정값이라 잉크색이다 -->
    <div class="mt-4 grid grid-cols-4">
      <span
        class="text-badge text-ink flex flex-col items-center gap-[3px] justify-self-center"
        :style="{ gridColumn: targetIndex + 1 }"
      >
        목표
        <svg width="9" height="5" viewBox="0 0 9 5" aria-hidden="true">
          <path d="M0 0h9L4.5 5z" fill="currentColor" />
        </svg>
      </span>
    </div>

    <!-- 구간 바 4칸. 지나온 칸은 연하게, 현재 칸은 채움, 목표 칸은 링 -->
    <div class="relative mt-[7px]">
      <ul
        class="m-0 grid h-(--gp-seg-bar-h) list-none grid-cols-4 gap-1 p-0"
        aria-label="마일리지 구간"
      >
        <li
          v-for="(segment, index) in SEGMENTS"
          :key="segment.key"
          class="rounded-xs"
          :class="[
            segmentClass(index),
            index === targetIndex ? 'shadow-[inset_0_0_0_1.5px_rgb(22_32_27/0.3)]' : '',
          ]"
        >
          <span class="sr-only">
            {{ segment.label }} · {{ formatMileage(segment.mileage) }}
            {{ index === currentIndex ? '· 지금' : '' }}{{ index === targetIndex ? '· 목표' : '' }}
          </span>
        </li>
      </ul>
      <span
        class="absolute -top-[3.5px] size-4 -translate-x-1/2 rounded-full shadow-[0_0_0_3px_#fff,0_1px_3px_rgb(16_40_28/0.22)]"
        :class="tone.fill"
        :style="{ left: `${(currentIndex + 0.5) * 25}%` }"
        aria-hidden="true"
      />
    </div>
    <div class="text-caption-sm text-muted mt-2.5 grid grid-cols-4 text-center tabular-nums">
      <span v-for="(segment, index) in SEGMENTS" :key="segment.key" :class="labelClass(index)">
        {{ segment.label }}
      </span>
    </div>

    <!-- 상태. 배경색 없이 아이콘과 글자만 -->
    <div class="border-divider mt-3.5 flex items-start gap-2.5 border-t pt-3">
      <component :is="tone.icon" :size="22" class="mt-0.5 shrink-0" :class="tone.text" />
      <div class="min-w-0 flex-1">
        <p class="text-list-title tracking-title text-ink m-0 text-balance">{{ status.title }}</p>
        <p v-if="status.sub" class="text-caption text-muted mt-1 mb-0">{{ status.sub }}</p>
      </div>
    </div>

    <!-- 예상 적립. 캡션 위 · 값 아래 2줄 -->
    <div class="mt-3.5 flex items-center gap-2.5 pt-3 pb-0.5">
      <img :src="ecoMark" alt="에코마일리지" class="h-[17px] w-auto shrink-0" />
      <div class="flex min-w-0 flex-col gap-px">
        <span class="flex items-center gap-1.5">
          <span class="text-caption text-muted font-semibold">{{ mileageCaption }}</span>
          <!-- 발표용 임시 버튼. 결산 모달(WF-09)이 이 카드를 덮으므로 가려지는 자리에 둔다 -->
          <button
            type="button"
            class="bg-surface-sub text-badge text-ink-soft cursor-pointer rounded-full border-0 px-2 py-[3px] font-bold"
            @click="$emit('demo-result')"
          >
            결산 보기
          </button>
        </span>
        <span class="text-amount tracking-display text-ink tabular-nums">
          {{ mileageParts[0]
          }}<span class="text-list-title text-ink-soft">{{ mileageParts[1] }}</span>
        </span>
      </div>
    </div>
    <!-- 시차 규칙(핵심 규칙 10). 화면 숫자와 누리집 확정값이 다른 이유를 미리 밝힌다 -->
    <p class="text-caption-sm text-muted mt-1.5 mb-0">
      검침 확정분은 2~3개월 뒤에 반영돼요 · 평가가 끝나야 확정돼요
    </p>

    <nav class="border-divider -mx-4 mt-3 flex border-t" aria-label="바로가기">
      <button
        type="button"
        class="text-caption text-muted flex-1 cursor-pointer border-0 bg-transparent py-[13px] font-bold"
        @click="$emit('goal')"
      >
        목표 관리
      </button>
      <button
        type="button"
        class="text-caption text-muted border-divider flex-1 cursor-pointer border-0 border-l bg-transparent py-[13px] font-bold"
        @click="$emit('report')"
      >
        월 리포트
      </button>
    </nav>
  </section>
</template>
