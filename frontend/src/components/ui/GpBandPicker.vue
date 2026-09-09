<script setup>
/*
 * 그린포켓 · 목표 절감률 단계 바 (WF-04)
 *
 * 0 · 5 · 10 · 15 네 눈금만 있는 **단계 선택**이다. 연속 슬라이더가 아니다 — 에코마일리지 인센티브는
 * 계단 함수이고 명세(B-2-03)는 「절감률 직접 입력」을 제외한다. 눈금 사이 값은 존재하지 않는다.
 *
 * 구간은 서버가 준다 — `GET /eco/rounds/{roundId}/goal-form` 의 `tiers`.
 *   [{ tier: 'TIER_10', label: '10~15%', targetRate: 10.000, mileage: 30000 }]
 * **기본값을 두지 않는다.** 구간 데이터는 제도 데이터라 프론트가 굳혀두면 서버 값이 바뀌었을 때
 * 화면만 틀린 숫자를 보여주게 된다 (frontend/AGENTS.md 9절).
 *
 * ── 눈금은 「점」이고 마일리지를 적지 않는다 (결정 C-37) ──────────────────
 * 5·10·15 는 **세 요금을 합친 감축률 R 의 지급 구간**이지 요금별 목표 단위가 아니다. 요금별로
 * 정해지는 값은 구간 하한 r_i 하나뿐이라 서버 `label`(「10~15%」)도 `mileage` 도 쓰지 않고
 * `targetRate` 만 「10%」로 적는다. 마일리지는 합산 요약(EcoGoalSummary) 한 곳에서만 보여준다.
 *
 * 맨 왼쪽 **0 은 「이 요금은 목표를 안 잡는다」**다. v-model 이 null 이 되고 저장 payload 의
 * `targets[]` 에서 빠진다(GoalSettingView). 세 요금 전부 0 이면 저장 버튼이 잠긴다.
 *
 * v-model 은 `tier` 코드 문자열(null 포함)이다. 서버의 `selectedTier` 와 같은 값이라 그대로 왕복한다.
 *
 * 조작 — 눈금 탭 · 트랙 드래그(가까운 눈금으로 스냅) · 키보드 ←→. 접근성은 role="slider" 로
 * 네 단계를 valuenow 로 노출한다.
 */
import { computed, ref } from 'vue'

import { formatPercent } from '@/utils/format'

const props = defineProps({
  modelValue: { type: String, default: null }, // 'TIER_10' · null = 0
  tiers: { type: Array, required: true },
  label: { type: String, default: '목표 절감률' },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

const stops = computed(() => [
  { tier: null, text: '0' },
  ...props.tiers.map((t) => ({ tier: t.tier, text: formatPercent(t.targetRate) })),
])
const last = computed(() => stops.value.length - 1)
const index = computed(() =>
  Math.max(
    0,
    stops.value.findIndex((s) => s.tier === props.modelValue),
  ),
)

/** 눈금 i 의 가로 위치. 양끝 점이 잘리지 않게 안쪽 여백(--pad)을 두고 그 사이를 등분한다 */
const left = (i) => `calc(var(--pad) + (100% - var(--pad) * 2) * ${i / last.value})`

function pick(i) {
  if (props.disabled) return
  const stop = stops.value[Math.min(Math.max(i, 0), last.value)]
  if (stop.tier !== props.modelValue) emit('update:modelValue', stop.tier)
}

const track = ref(null)
let dragging = false

function pickFromPointer(event) {
  const rect = track.value.getBoundingClientRect()
  const pad = 9
  const ratio = (event.clientX - rect.left - pad) / (rect.width - pad * 2)
  pick(Math.round(Math.min(1, Math.max(0, ratio)) * last.value))
}
function onPointerDown(event) {
  if (props.disabled) return
  dragging = true
  pickFromPointer(event)
  // 캡처는 드래그가 트랙 밖으로 나가도 따라오게 하는 보조 장치다. 실패해도 탭 선택은 이미 끝났다
  try {
    event.currentTarget.setPointerCapture(event.pointerId)
  } catch {
    /* 지원하지 않는 포인터 */
  }
}
function onPointerMove(event) {
  if (dragging) pickFromPointer(event)
}
function onPointerUp() {
  dragging = false
}
function onKeydown(event) {
  if (event.key === 'ArrowRight' || event.key === 'ArrowUp') {
    event.preventDefault()
    pick(index.value + 1)
  } else if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') {
    event.preventDefault()
    pick(index.value - 1)
  }
}
</script>

<template>
  <div
    ref="track"
    role="slider"
    :tabindex="disabled ? -1 : 0"
    :aria-label="label"
    :aria-valuemin="0"
    :aria-valuemax="last"
    :aria-valuenow="index"
    :aria-valuetext="stops[index].text"
    :aria-disabled="disabled"
    class="focus-visible:ring-primary/40 relative h-9 touch-none select-none rounded-sm outline-hidden focus-visible:ring-2 [--pad:9px]"
    :class="disabled ? 'cursor-not-allowed opacity-40' : 'cursor-pointer'"
    @pointerdown="onPointerDown"
    @pointermove="onPointerMove"
    @pointerup="onPointerUp"
    @pointercancel="onPointerUp"
    @keydown="onKeydown"
  >
    <!-- 트랙과 채움. 채움은 0 에서 고른 눈금까지 -->
    <span class="bg-track absolute top-[8px] right-(--pad) left-(--pad) h-[3px] rounded-full" />
    <span
      class="bg-primary-soft ease-standard absolute top-[8px] left-(--pad) h-[3px] rounded-full transition-[width] duration-140"
      :style="{ width: `calc((100% - var(--pad) * 2) * ${index / last})` }"
    />

    <!-- 눈금 점 + 라벨. 고른 눈금은 흰 테를 두른 큰 점 -->
    <span
      v-for="(stop, i) in stops"
      :key="stop.text"
      class="absolute top-0 flex -translate-x-1/2 flex-col items-center"
      :style="{ left: left(i) }"
    >
      <span
        class="ease-standard mt-[3px] block rounded-full transition-all duration-140"
        :class="
          i === index
            ? 'bg-primary-soft size-[13px] shadow-[0_0_0_3px_var(--color-surface),0_0_0_4.5px_var(--color-primary-soft)]'
            : i < index
              ? 'bg-primary-soft mt-[5px] size-[9px]'
              : 'bg-track mt-[5px] size-[9px]'
        "
      />
      <span
        class="text-caption-sm tabular-nums mt-auto pt-2"
        :class="i === index ? 'text-primary-on-soft font-bold' : 'text-muted'"
      >
        {{ stop.text }}
      </span>
    </span>
  </div>
</template>
