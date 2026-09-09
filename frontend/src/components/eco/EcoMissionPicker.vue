<script setup>
/*
 * 실천 미션 고르기 — WF-04 (B-3-03 · B-3-04)
 *
 * 미션 목록은 `goal-form` 의 `segments[].missions[]` 에만 있다 — 최상위 `missions[]` 는 없다.
 * 합계 제외 여부는 미션이 아니라 **미리보기가** 판단한다: `POST .../goal/preview` 의
 * `missions.items[]` 가 `counted` 와 `exclusionReason("냉방 겹침 · 합계 제외")` 을 문구까지 준다.
 * 같은 `deviceGroup` 에서 가장 큰 것만 세는 규칙이라 **프론트가 다시 판정하지 않는다.**
 *
 * ── 합계·목표 대비 문구는 두지 않는다 (2026-09-09 수현) ──────────────────
 * 미션 %는 「출처 절감량 ÷ 그 요금의 월 기준 사용량」이라 분모가 요금마다 다르다. 서버의
 * `preview.missions.combinedMissionRate` 는 세 요금 미션을 구분 없이 더한 값이라 뜻이 없고, 요금별 합을
 * 화면에서 더해 보여주는 안도 검토했지만 **고르는 데만 집중**하도록 뺐다. 이 카드는 미션 목록뿐이다.
 * 제목도 없다 — 바로 위 섹션 제목 「실천 미션 고르기」와 요금 세그먼트가 이미 무엇인지 말한다.
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import GpMissionRow from '@/components/ui/GpMissionRow.vue'

const props = defineProps({
  segment: { type: Object, required: true },
  selectedIds: { type: Array, required: true },
  // preview.missions.items[] — 미리보기가 오기 전에는 비어 있다
  previewItems: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:selectedIds'])

const itemById = computed(() => new Map(props.previewItems.map((item) => [item.missionId, item])))

const isSelected = (missionId) => props.selectedIds.includes(missionId)

function toggle(missionId, checked) {
  const next = props.selectedIds.filter((id) => id !== missionId)
  if (checked) next.push(missionId)
  emit('update:selectedIds', next)
}
</script>

<template>
  <GpCard>
    <div class="divide-divider -my-2 divide-y">
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
  </GpCard>
</template>
