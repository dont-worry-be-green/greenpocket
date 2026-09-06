<script setup>
/*
 * WF-07 남은 달 처방 (B-4-07 ③ · B-4-08)
 *
 * `prescription` 은 GET /eco/monthly-report 의 `prescription` 그대로다.
 * 1·2·3 단계 가이드가 `cause` 의 숫자를 함께 쓰기 때문에 그것도 받는다 — 두 블록이
 * 같은 응답 안에 있어 화면이 조립해도 서버 값과 어긋나지 않는다.
 *
 * ⚠️ `requiredRate` 는 **증감이 아니다.** 「남은 달마다 이만큼 줄여야 한다」는 목표치라
 * GpDelta 에 넘기면 "줄었어요" 가 붙어 이미 줄인 것처럼 읽힌다 → formatPercent 로 그린다.
 *
 * ⚠️ `remainingMonths === 0` 이면 서버가 `requiredRate: null` 을 준다(0 나눗셈 금지).
 * 남은 달이 없으니 처방이 아니라 결과를 기다리는 안내를 띄운다.
 *
 * ⚠️ **단계를 세 개로 맞추려고 문장을 지어내지 않는다.** 각 단계의 숫자는 전부 응답에서 온다.
 * 근거가 없는 단계는 그냥 빠진다 — 시안의 「여름 냉방」 같은 계절 문장은 응답에 계절이 없어
 * 넣지 않았다(AGENTS 3절 · 핵심 규칙 8).
 *
 * `assumption` 은 **가정을 밝히는 문장**이다(핵심 규칙 7). "달마다 12%" 만 보이면
 * 나머지 요금이 지금 속도를 유지한다는 전제가 숨는다.
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import { formatPercent, formatUtilityType } from '@/utils/format'

const props = defineProps({
  prescription: { type: Object, required: true },
  // 단계 가이드가 쓰는 온실가스 비중·달성 여부. 없으면 그 단계만 빠진다
  cause: { type: Object, default: null },
  // 회차 전체 목표(`result.targetRate`). 초록 박스의 "6개월 합쳐 …" 에 쓴다
  targetRate: { type: Number, default: null },
})

const hasRemaining = computed(
  () => props.prescription.remainingMonths > 0 && props.prescription.requiredRate !== null,
)

/** '8·9월' */
const monthsLabel = computed(() =>
  (props.prescription.remainingMonthLabels ?? []).map((month) => `${month}월`).join('·'),
)

/** 서버가 밝힌 가정. 여러 요금이 와도 문장은 같은 전제를 말하므로 첫 줄만 보여준다 */
const assumption = computed(() => props.prescription.requiredByUtility?.[0]?.assumption ?? '')

const rows = computed(() => props.cause?.byUtility ?? [])

const steps = computed(() => {
  const list = []

  // ① 어디부터 — 서버가 고른 조정 대상과 그 요금의 온실가스 비중
  const focus = props.prescription.adjustTargetUtility
  if (focus) {
    const share = rows.value.find((row) => row.utilityType === focus)?.carbonSharePercent
    const name = formatUtilityType(focus)
    list.push(
      share == null
        ? `${name}부터 손대요.`
        : `${name}부터 손대요. 합산의 ${formatPercent(share)}를 차지해서 여기가 가장 크게 움직여요.`,
    )
  }

  // ② 지금 고른 실천으로 남은 몫을 덮는지 (achievable — 확정 표현과 가능성 문구를 가른다)
  const selected = props.prescription.selectedMissionRate
  if (selected != null) {
    list.push(
      `지금 고른 실천으로는 ${formatPercent(selected)} 줄이기. ` +
        (props.prescription.achievable
          ? '남은 몫을 덮을 수 있어요.'
          : '실천을 더 고르면 여유가 생겨요.'),
    )
  }

  // ③ 이미 목표를 넘긴 요금은 손대지 않아도 된다
  const done = rows.value.filter((row) => row.achieved).map((row) => formatUtilityType(row.utilityType))
  if (done.length) list.push(`${done.join('·')}는 이미 목표를 넘겼어요. 하던 대로만 하면 돼요.`)

  return list
})
</script>

<template>
  <GpCard :title="`남은 ${prescription.remainingMonths}달, 이렇게 하면 돼요`">
    <template v-if="hasRemaining">
      <div class="bg-positive-bg rounded-md px-4 py-3.5">
        <p class="text-caption text-on-positive mt-0 mb-0.5">{{ monthsLabel }}에 달마다</p>
        <p class="text-amount text-on-positive m-0 tabular-nums">
          {{ formatPercent(prescription.requiredRate) }}씩 줄이면
        </p>
        <p v-if="targetRate != null" class="text-caption text-on-positive mt-0.5 mb-0">
          6개월 합쳐 {{ formatPercent(targetRate) }} 목표를 지키는 속도예요
        </p>
      </div>

      <p v-if="assumption" class="text-caption text-muted mt-2 mb-0">{{ assumption }}</p>

      <ol v-if="steps.length" class="mt-4 mb-0 list-none space-y-2.5 p-0">
        <li v-for="(step, index) in steps" :key="step" class="flex items-start gap-2.5">
          <span
            class="bg-primary text-on-primary text-caption-sm mt-0.5 flex size-5 flex-none items-center justify-center rounded-full font-bold"
            aria-hidden="true"
          >
            {{ index + 1 }}
          </span>
          <span class="text-caption text-ink-soft min-w-0 flex-1">{{ step }}</span>
        </li>
      </ol>
    </template>

    <!-- 남은 달이 없다. 처방할 것이 없으니 숫자를 만들지 않는다 (핵심 규칙 8) -->
    <p v-else class="text-body text-ink-soft m-0">
      평가 기간이 끝났어요. 확정 결과가 나오면 알려드릴게요.
    </p>
  </GpCard>
</template>
