<script setup>
/*
 * 실천 미션 고르기 — WF-04 (B-3-03 · B-3-04)
 *
 * 미션 목록은 `goal-form` 의 `segments[].missions[]` 에만 있다 — 최상위 `missions[]` 는 없다.
 * 합계 제외 여부는 미션이 아니라 **미리보기가** 판단한다: `POST .../goal/preview` 의
 * `missions.items[]` 가 `counted` 와 `exclusionReason("냉방 겹침 · 합계 제외")` 을 문구까지 준다.
 * 같은 `deviceGroup` 에서 가장 큰 것만 세는 규칙이라 **프론트가 다시 판정하지 않는다.**
 *
 * ── 합계는 **이 요금 하나**만 더한다 (2026-09-10 수현, 결정 C-42) ──────────────
 * 미션 %는 「출처 절감량 ÷ 그 요금의 월 기준 사용량」이라 분모가 요금마다 다르다. 서버의
 * `preview.missions.combinedMissionRate` 는 세 요금 미션을 구분 없이 더한 값이라 쓰지 않고,
 * 지금 보고 있는 세그먼트의 고른 미션 중 `counted` 인 것만 화면에서 더한다(`segment.missions` 는
 * 계절로 걸러진 목록이지만 고른 미션은 계절과 무관하게 남으므로 이 요금의 선택은 전부 들어 있다).
 * 목표 대비도 같은 요금의 구간 하한(`targetRate`)과만 비교한다 — 구간을 안 골랐으면 합계만 보인다.
 * 미등록 요금은 목표를 세울 수 없어 합계를 내지 않는다(B-2-02).
 * 카드 제목은 없다 — 바로 위 섹션 제목 「실천 미션 고르기」와 요금 세그먼트가 이미 무엇인지 말한다.
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import GpDelta from '@/components/ui/GpDelta.vue'
import GpMissionRow from '@/components/ui/GpMissionRow.vue'
import { formatPercent, formatPoint, formatUtilityType } from '@/utils/format'

const props = defineProps({
  segment: { type: Object, required: true },
  selectedIds: { type: Array, required: true },
  // preview.missions.items[] — 미리보기가 오기 전에는 비어 있다
  previewItems: { type: Array, default: () => [] },
  // 이 요금에 고른 구간의 하한(%) — goal-form `tiers[].targetRate`. 안 골랐으면 null
  targetRate: { type: Number, default: null },
})
const emit = defineEmits(['update:selectedIds'])

const itemById = computed(() => new Map(props.previewItems.map((item) => [item.missionId, item])))

const isSelected = (missionId) => props.selectedIds.includes(missionId)

const name = computed(() => formatUtilityType(props.segment.utilityType))

const selectedMissions = computed(() =>
  props.segment.missions.filter((mission) => isSelected(mission.missionId)),
)

/** 이 요금의 고른 미션 합계 — 미리보기가 `counted:false` 로 판정한 겹침 미션은 뺀다 */
const utilitySum = computed(() =>
  selectedMissions.value.reduce((sum, mission) => {
    const item = itemById.value.get(mission.missionId)
    if (item && item.counted === false) return sum
    return sum + (item?.computedRate ?? mission.computedRate ?? 0)
  }, 0),
)

const shortfall = computed(() => {
  if (props.targetRate === null) return null
  return Math.max(props.targetRate - utilitySum.value, 0)
})

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

    <!-- 이 요금의 합계 · 목표 대비. 세 요금을 섞지 않는다 (B-3-04) -->
    <div class="border-divider mt-2 border-t pt-4">
      <p v-if="!segment.registered" class="text-caption text-muted m-0">
        이 요금은 목표를 세울 수 없어 합계를 내지 않아요
      </p>
      <template v-else>
        <div class="flex items-center justify-between gap-3">
          <span class="text-list-title">
            {{ name }} 고른 미션 합계
            <span class="text-caption text-muted ml-1">{{ selectedMissions.length }}개</span>
          </span>
          <span v-if="selectedMissions.length === 0" class="text-caption text-muted">
            아직 고른 미션이 없어요
          </span>
          <GpDelta v-else :value="utilitySum" word="줄일 수 있어요" />
        </div>
        <p
          v-if="targetRate !== null && selectedMissions.length > 0"
          class="text-caption text-muted mt-2 mb-0"
        >
          <template v-if="shortfall === 0"
            >{{ name }} 목표 {{ formatPercent(targetRate) }}를 채웠어요</template
          >
          <template v-else>
            {{ name }} 목표 {{ formatPercent(targetRate) }}까지
            {{ formatPoint(shortfall) }} 모자라요 · 실천을 더 고르거나 구간을 낮춰도 돼요
          </template>
        </p>
      </template>
    </div>
  </GpCard>
</template>
