<script setup>
/*
 * 실천 미션 고르기 — WF-04 (B-3-03 · B-3-04)
 *
 * 미션 목록은 `goal-form` 의 `segments[].missions[]` 에만 있다 — 최상위 `missions[]` 는 없다.
 * 합계 제외 여부는 미션이 아니라 **미리보기가** 판단한다: `POST .../goal/preview` 의
 * `missions.items[]` 가 `counted` 와 `exclusionReason("냉방 겹침 · 합계 제외")` 을 문구까지 준다.
 * 같은 `deviceGroup` 에서 가장 큰 것만 세는 규칙이라 **프론트가 다시 판정하지 않는다.**
 *
 * ⚠️ `summary`(= `preview.missions`)는 **회차 전체 합계**다. 지금 보고 있는 요금 하나가 아니라
 * 고른 미션 전부를 더한 값이라, 탭을 옮겨도 같은 숫자가 나온다. 문구를 "고른 실천 합계"로 둔 이유다.
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import GpDelta from '@/components/ui/GpDelta.vue'
import GpMissionRow from '@/components/ui/GpMissionRow.vue'
import { formatPoint, formatUtilityType } from '@/utils/format'

const props = defineProps({
  segment: { type: Object, required: true },
  selectedIds: { type: Array, required: true },
  // preview.missions.items[] — 미리보기가 오기 전에는 비어 있다
  previewItems: { type: Array, default: () => [] },
  // preview.missions — combinedMissionRate · shortfallPoint · meetsTarget
  summary: { type: Object, default: null },
})
const emit = defineEmits(['update:selectedIds'])

const itemById = computed(
  () => new Map(props.previewItems.map((item) => [item.missionId, item])),
)

const isSelected = (missionId) => props.selectedIds.includes(missionId)

function toggle(missionId, checked) {
  const next = props.selectedIds.filter((id) => id !== missionId)
  if (checked) next.push(missionId)
  emit('update:selectedIds', next)
}
</script>

<template>
  <GpCard
    :title="`${formatUtilityType(segment.utilityType)}, 이렇게 줄여요`"
    caption="절감률은 추정치예요. 숫자를 누르지 않아도 근거가 행 안에 있어요"
  >
    <div class="border-divider divide-divider divide-y border-t">
      <GpMissionRow
        v-for="mission in segment.missions"
        :key="mission.missionId"
        :mission="mission"
        :model-value="isSelected(mission.missionId)"
        :counted="itemById.get(mission.missionId)?.counted ?? true"
        :exclusion-reason="itemById.get(mission.missionId)?.exclusionReason ?? ''"
        :rate-cap="segment.missionRateCap"
        @update:model-value="toggle(mission.missionId, $event)"
      />
    </div>

    <div v-if="summary" class="border-divider mt-4 border-t pt-4">
      <div class="flex items-center justify-between gap-3">
        <span class="text-list-title">고른 실천 합계</span>
        <GpDelta :value="summary.combinedMissionRate" word="줄일 수 있어요" />
      </div>
      <p class="text-caption mt-2 mb-0" :class="summary.meetsTarget ? 'text-on-positive' : 'text-muted'">
        <template v-if="summary.meetsTarget">
          고른 실천만으로 목표 감축률을 채웠어요
        </template>
        <template v-else>
          목표까지 {{ formatPoint(summary.shortfallPoint) }} 모자라요. 실천을 더 고르거나 구간을
          낮춰도 돼요
        </template>
      </p>
    </div>
  </GpCard>
</template>
