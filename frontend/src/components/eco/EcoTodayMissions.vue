<script setup>
/*
 * WF-06 오늘의 실천 (B-3-05 · B-3-06)
 *
 * `data` 는 GET /eco/rounds/{roundId}/missions/today 그대로다.
 *
 * ⚠️ **GpMissionRow 를 쓰지 않는다.** 그 컴포넌트는 `computedRate` · `evidenceText` ·
 * `calculationBasis` 를 전제하는데 오늘의 실천 응답에는 다섯 필드밖에 없다
 * (`missionId` `title` `utilityType` `difficulty` `completed`). 억지로 끼우면 근거 자리가 빈다.
 * 그래서 여기서 간단한 행을 직접 그리고 **components/ui/ 에는 추가하지 않는다.**
 *
 * ⚠️ 체크는 토글 1건이 아니라 **하루치 전량**을 올린다(PUT mission-logs/{date}).
 * 그래서 `change` 로 완료 목록 전체를 넘긴다.
 *
 * `emptyReason` 이 있는 응답도 **200 정상이다**(핵심 규칙 8). 서버는 코드만 주고 문구는 화면이 만든다.
 * 진행 수(3/5)는 화면이 세지 않고 서버가 준 `completedCount` 를 쓴다 — 저장이 실패하면
 * 화면이 센 숫자만 앞서간다.
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import GpTag from '@/components/ui/GpTag.vue'
import { formatDifficulty, formatUtilityType } from '@/utils/format'

const props = defineProps({
  data: { type: Object, default: null },
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['change'])

const SEASON_LABEL = { SPRING: '봄', SUMMER: '여름', AUTUMN: '가을', WINTER: '겨울' }

/** 서버는 코드로 준다. 문장은 화면 몫이다 */
const EMPTY_MESSAGE = {
  NO_GOAL: '평가 기간 목표를 정하면 오늘 할 실천이 생겨요.',
  NO_MISSION: '목표를 정할 때 고른 실천 중 오늘 계절에 맞는 것이 없어요.',
}

const missions = computed(() => props.data?.missions ?? [])

const emptyMessage = computed(() => {
  if (!props.data?.emptyReason) return ''
  return EMPTY_MESSAGE[props.data.emptyReason] ?? '오늘 할 실천이 없어요.'
})

const seasonLabel = computed(() => SEASON_LABEL[props.data?.season] ?? '')

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
    <template v-if="data" #action>
      <span class="text-list-title tabular-nums">
        {{ data.completedCount }}<span class="text-muted">/{{ data.totalCount }}</span>
      </span>
    </template>

    <p v-if="seasonLabel && missions.length" class="text-caption text-muted mt-0 mb-2">
      고른 실천 중 {{ seasonLabel }}에 맞는 것만 보여드려요
    </p>

    <ul v-if="missions.length" class="border-divider m-0 list-none border-t p-0">
      <li v-for="mission in missions" :key="mission.missionId" class="border-divider border-b last:border-b-0">
        <label
          class="flex min-h-(--gp-row-h) cursor-pointer items-center gap-3 py-2"
          :class="saving ? 'opacity-60' : ''"
        >
          <input
            type="checkbox"
            class="size-(--gp-checkbox) accent-primary shrink-0 cursor-pointer"
            :checked="mission.completed"
            :disabled="saving"
            @change="toggle(mission)"
          />
          <span
            class="text-body-strong min-w-0 flex-1"
            :class="mission.completed ? 'text-muted line-through' : 'text-ink'"
          >
            {{ mission.title }}
          </span>
          <span class="flex shrink-0 items-center gap-1">
            <!-- 요금 색은 GpTag 의 tone 에 없다. 같은 모양의 칩을 여기서 그린다 -->
            <span
              class="text-caption-sm inline-flex h-(--gp-tag-sm-h) items-center rounded-xs px-1.5 font-semibold"
              :class="UTILITY_TONE[mission.utilityType]"
            >
              {{ formatUtilityType(mission.utilityType) }}
            </span>
            <GpTag small>{{ formatDifficulty(mission.difficulty) }}</GpTag>
          </span>
        </label>
      </li>
    </ul>

    <p v-else-if="emptyMessage" class="text-body text-ink-soft m-0">{{ emptyMessage }}</p>
    <p v-else class="text-body text-muted m-0">불러오는 중이에요…</p>
  </GpCard>
</template>
