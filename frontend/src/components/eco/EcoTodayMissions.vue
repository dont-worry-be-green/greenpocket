<script setup>
/*
 * WF-06 오늘의 실천 (B-3-05 · B-3-06) — 홈 확정 시안(design-system.md 9-3).
 *
 * `data` 는 GET /eco/rounds/{roundId}/missions/today 그대로다.
 *
 * ── 행은 「체크 왼쪽 · 요금 배지 · 미션명 · 오른쪽 비움」 ─────────────────
 * 20px 요금 아이콘은 그 크기에서 불꽃과 물방울이 안 구별됐다. 글자 배지(전기·도시가스·수도)가
 * 구분을 맡고 색은 그 글자가 무슨 계열인지 설명만 한다(COM-07). 4px 색 세로선은 색이 두 번
 * 들어가 시끄러워서 뺐고, 왼쪽 앵커는 체크 원이 넘겨받았다 — 완료된 것이 왼쪽에 정렬돼
 * 「2/5」가 목록만 봐도 읽히고, 행 전체가 버튼이라는 것도 분명해진다.
 * 정렬 기준선은 배지 글자가 아니라 배지 블록의 왼쪽 모서리다.
 *
 * ⚠️ **GpMissionRow 를 쓰지 않는다.** 그 컴포넌트는 `computedRate` · `evidenceText` ·
 * `calculationBasis` 를 전제하는데 오늘의 실천 응답에는 다섯 필드밖에 없다.
 * ⚠️ **난이도 배지를 달지 않는다.** 오늘 할지 말지는 목표를 정할 때 이미 고른 결과다.
 * ⚠️ 행별 금액을 두지 않는다 — 응답에 없다. 합계는 하단 한 줄이 들고, 그 값은
 *    목표 조회의 `expectedSavingAmount`(목표를 다 지켰을 때 월 절감액, 원)다. 「약」을 붙이고
 *    「아껴요」로 닫는다 — 바로 위 카드의 「예상 에코마일리지 적립」과 다른 돈이다(핵심 규칙 2·3).
 *
 * 체크는 토글 1건이 아니라 **하루치 전량**을 올린다(PUT mission-logs/{date}). `change` 로 완료
 * 목록 전체를 넘긴다. 진행 수는 헤더 헤드라인(「N개가 남았어요」)이 서버 `completedCount` 로 말한다.
 * `emptyReason` 이 있는 응답도 **200 정상이다**(핵심 규칙 8). 서버는 코드만 주고 문구는 화면이 만든다.
 * 목록은 고른 미션 전부다 — 계절로 거르지 않는다(결정 C-35). 계절 한정 미션은 「여름 전용」 칩으로만 알린다.
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import IconCheck from '@/components/ui/icons/IconCheck.vue'
import IconChevronRight from '@/components/ui/icons/IconChevronRight.vue'
import { formatSeasonTags, formatUtilityType, formatWon } from '@/utils/format'

const props = defineProps({
  data: { type: Object, default: null },
  saving: { type: Boolean, default: false },
  /** GET /eco/rounds/{roundId}/goal 의 `expectedSavingAmount`(원). 없으면 합계 줄을 그리지 않는다 */
  expectedSavingAmount: { type: Number, default: null },
})
const emit = defineEmits(['change'])

/** 서버는 코드로 준다. 문장은 화면 몫이다 */
const EMPTY_MESSAGE = {
  NO_GOAL: '평가 기간 목표를 정하면 오늘 할 실천이 생겨요.',
  NO_MISSION: '목표를 정할 때 고른 실천이 없어요. 실천을 골라 주세요.',
}

const missions = computed(() => props.data?.missions ?? [])

const emptyMessage = computed(() => {
  if (!props.data?.emptyReason) return ''
  return EMPTY_MESSAGE[props.data.emptyReason] ?? '오늘 할 실천이 없어요.'
})

/* 요금 배지. 글자색은 AA(4.83 · 6.10 · 6.11), 바탕은 흰 카드 대비 1.1 안팎으로 아주 옅다 */
const UTILITY_TONE = {
  ELECTRICITY: 'bg-elec-bg text-elec',
  GAS: 'bg-gas-bg text-gas',
  WATER: 'bg-water-bg text-water',
}

function toggle(mission) {
  const next = missions.value
    .filter((item) => (item.missionId === mission.missionId ? !item.completed : item.completed))
    .map((item) => item.missionId)
  emit('change', next)
}
</script>

<template>
  <GpCard title="오늘의 실천">
    <!-- 「달력 보기」— 실천 달력 화면은 보류 중이다(2026-09-09). 갈 곳이 생기면 버튼으로 바꾼다 -->
    <template #action>
      <span
        class="text-caption text-muted inline-flex items-center gap-0.5 font-bold"
        aria-disabled="true"
      >
        달력 보기
        <IconChevronRight :size="11" aria-hidden="true" />
      </span>
    </template>

    <ul v-if="missions.length" class="divide-divider -mt-1 m-0 list-none divide-y p-0">
      <li v-for="mission in missions" :key="mission.missionId">
        <button
          type="button"
          role="checkbox"
          :aria-checked="mission.completed"
          :disabled="saving"
          class="flex min-h-[46px] w-full cursor-pointer items-center gap-3 border-0 bg-transparent py-[9px] text-left disabled:cursor-progress disabled:opacity-60"
          @click="toggle(mission)"
        >
          <span
            class="ease-standard flex size-(--gp-checkbox) shrink-0 items-center justify-center rounded-full border-[1.8px] transition-colors duration-140"
            :class="
              mission.completed
                ? 'bg-control-on border-control-on text-on-primary'
                : 'border-control-border bg-surface text-transparent'
            "
            aria-hidden="true"
          >
            <IconCheck :size="11" />
          </span>
          <span class="flex min-w-0 flex-1 flex-col items-start">
            <span
              class="inline-flex items-center gap-1"
              :class="mission.completed ? 'opacity-50' : ''"
            >
              <span
                class="text-badge tracking-normal inline-flex items-center rounded-xs px-[5px] py-0.5"
                :class="UTILITY_TONE[mission.utilityType]"
              >
                {{ formatUtilityType(mission.utilityType) }}
              </span>
              <!-- 계절 한정 미션(결정 C-35). 계절로 거르지 않고 알리기만 한다 -->
              <span
                v-if="formatSeasonTags(mission.seasonTags)"
                class="text-badge tracking-normal bg-surface-sub text-muted inline-flex items-center rounded-xs px-[5px] py-0.5"
              >
                {{ formatSeasonTags(mission.seasonTags) }} 전용
              </span>
            </span>
            <span
              class="text-body-strong tracking-body mt-1 block w-full truncate"
              :class="mission.completed ? 'text-muted' : 'text-ink'"
            >
              {{ mission.title }}
            </span>
          </span>
        </button>
      </li>
    </ul>

    <p v-else-if="emptyMessage" class="text-body text-ink-soft m-0">{{ emptyMessage }}</p>
    <p v-else class="text-body text-muted m-0">불러오는 중이에요…</p>

    <p
      v-if="missions.length && expectedSavingAmount != null"
      class="border-divider text-caption text-ink-soft mt-[13px] mb-0 border-t pt-[13px]"
    >
      목표대로 다 지키면 월 약
      <b class="text-primary-on-soft font-bold tabular-nums">{{
        formatWon(expectedSavingAmount)
      }}</b
      >을 아껴요
    </p>
  </GpCard>
</template>
