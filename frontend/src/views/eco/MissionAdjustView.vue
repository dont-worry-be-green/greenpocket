<script setup>
/*
 * 실천 다시 고르기 — WF-08 (B-3-08 · B-4-09)
 *
 * ── ⚠️ 화면은 요금 하나인데 저장은 회차 전량이다 ────────────────────────────
 * `PUT .../missions` 의 `selectedMissionIds` 는 **회차에서 고른 미션 전부**다
 * (api-spec.md 10.5 예시에 전기 13·15·16 과 가스 31·44 가 섞여 있다).
 * 이 화면은 전기만 보여주므로, 보이는 것만 보내면 **수도·도시가스 선택이 조용히 사라진다.**
 * 그래서 `GET .../goal` 로 다른 요금의 선택을 먼저 받아 두고 합쳐서 보낸다.
 *
 * ── 합계는 서버가 센다 ──────────────────────────────────────────────────
 * 체크할 때마다 `POST .../goal/preview` 를 부른다(250ms 디바운스). 같은 기기군 중복 제외 규칙을
 * 화면이 다시 구현하면 WF-04 와 다른 숫자가 나온다. `mission-adjust` 의 `preview` 는
 * **저장된 선택 기준**이라 체크를 따라 움직이지 않는다 — 출발점으로만 쓴다.
 *
 * ── 구간 하향은 제안만 한다 ──────────────────────────────────────────────
 * `tierDowngrade.suggest` 가 true 여도 여기서 구간을 바꾸지 않는다(핵심 규칙 9).
 * 목표 정하기 화면으로 보낼 뿐이다.
 */
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import EcoTierDowngradeNotice from '@/components/eco/EcoTierDowngradeNotice.vue'
import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import GpCard from '@/components/ui/GpCard.vue'
import GpDelta from '@/components/ui/GpDelta.vue'
import GpMissionRow from '@/components/ui/GpMissionRow.vue'
import { useEcoStore } from '@/stores/eco'
import { formatMonth, formatPercent, formatUtilityType } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const store = useEcoStore()

/** 쿼리 키는 `utility` 다. 응답 필드 `utilityType` 과 이름이 다르다 */
const utility = computed(() => route.query.utility ?? 'ELECTRICITY')

const adjust = computed(() => store.missionAdjust)

// 이 요금에서 체크한 미션 (뷰 로컬 폼 상태)
const selectedIds = ref([])

/** 다른 요금에서 고른 것. 저장할 때 여기에 합쳐야 사라지지 않는다 */
const otherSelectedIds = computed(() =>
  (store.goal?.missions ?? [])
    .filter((mission) => mission.utilityType !== utility.value)
    .map((mission) => mission.missionId),
)

const mergedIds = computed(() => [...otherSelectedIds.value, ...selectedIds.value])

/** 이미 걸어 둔 구간. preview 는 targets 를 같이 받아야 계산한다 */
const targets = computed(() =>
  (store.goal?.utilities ?? [])
    .filter((item) => item.targetTier)
    .map((item) => ({ utilityType: item.utilityType, tier: item.targetTier })),
)

/** 체크를 따라 움직이는 합계. 미리보기가 오기 전에는 저장된 값(`preview.currentRate`)을 보인다 */
const combinedRate = computed(
  () => store.goalPreview?.missions?.combinedMissionRate ?? adjust.value?.preview?.currentRate ?? 0,
)
const coversRequired = computed(
  () => adjust.value !== null && combinedRate.value >= adjust.value.requiredRate,
)

const previewItemById = computed(
  () => new Map((store.goalPreview?.missions?.items ?? []).map((item) => [item.missionId, item])),
)

const isSelected = (missionId) => selectedIds.value.includes(missionId)

function toggle(missionId, checked) {
  const next = selectedIds.value.filter((id) => id !== missionId)
  if (checked) next.push(missionId)
  selectedIds.value = next
}

/** 추천을 한 번에 반영한다. 고르는 것은 사용자가 누른 뒤에만 일어난다(핵심 규칙 9) */
function applyRecommended() {
  const recommended = (adjust.value?.missions ?? [])
    .filter((mission) => mission.recommended)
    .map((mission) => mission.missionId)
  selectedIds.value = [...new Set([...selectedIds.value, ...recommended])]
}

// ── 미리보기 ────────────────────────────────────────────────────────────

let previewTimer = null
function requestPreview(delay) {
  window.clearTimeout(previewTimer)
  const roundId = store.roundId
  if (!roundId || !store.goal) return
  previewTimer = window.setTimeout(
    () =>
      store.fetchGoalPreview(roundId, {
        targets: targets.value,
        selectedMissionIds: mergedIds.value,
      }),
    delay,
  )
}
onUnmounted(() => window.clearTimeout(previewTimer))
watch(mergedIds, () => requestPreview(250))

// ── 진입 ────────────────────────────────────────────────────────────────

/** 서버가 준 현재 선택으로 초안을 채운다 */
watch(adjust, (data) => {
  if (!data) return
  selectedIds.value = data.missions.filter((mission) => mission.selected).map((m) => m.missionId)
  requestPreview(0)
})

async function load() {
  // 홈을 거치지 않고 바로 들어올 수 있다
  if (!store.roundId) await store.fetchCurrentRound()
  const roundId = store.roundId
  if (!roundId) return
  // 목표 정하기 화면이 남긴 미리보기가 있으면 첫 화면에 남의 숫자가 잠깐 뜬다
  store.goalPreview = null
  await store.fetchGoal(roundId)
  await store.fetchMissionAdjust(roundId, {
    utility: utility.value,
    ...(route.query.month ? { month: route.query.month } : {}),
  })
}
onMounted(load)
watch(utility, load)

// ── 저장 ────────────────────────────────────────────────────────────────

async function save() {
  const roundId = store.roundId
  if (!roundId) return
  const saved = await store.saveSelectedMissions(roundId, mergedIds.value)
  if (saved) router.push('/whatif')
}
</script>

<template>
  <AppSubLayout title="실천 다시 고르기" back="/whatif/report" has-footer>
    <!-- 로딩·실패·빈 결과를 남기지 않는다 (COM-08) -->
    <p v-if="store.isLoading && !adjust" class="text-caption text-muted py-10 text-center">
      실천 목록을 불러오는 중이에요
    </p>

    <div v-else-if="!adjust" class="py-10 text-center">
      <p class="text-caption text-muted mt-0 mb-4">
        {{ store.error?.message || '실천 목록을 불러오지 못했어요' }}
      </p>
      <GpButton variant="pill" size="pill" @click="load">다시 시도</GpButton>
    </div>

    <div v-else class="space-y-4 pt-1">
      <!-- 고를 때 기대한 값과 실제로 나온 값. 이 간격이 이 화면의 출발점이다 -->
      <GpCard
        :title="`${formatUtilityType(adjust.utilityType)}, 기대와 결과`"
        :caption="`${formatMonth(adjust.reportMonth)} 기준이에요`"
      >
        <div class="flex gap-3">
          <div class="bg-surface-sub flex-1 rounded-md p-3">
            <span class="text-caption text-muted block">고를 때 기대</span>
            <span class="text-list-title mt-1 block">
              {{ formatPercent(adjust.comparison.selectedExpectedRate) }}
            </span>
          </div>
          <div class="bg-surface-sub flex-1 rounded-md p-3">
            <span class="text-caption text-muted block">실제 결과</span>
            <GpDelta :value="adjust.comparison.actualRate" size="sm" :show-word="false" class="mt-1" />
          </div>
        </div>
        <p class="text-caption text-muted mt-3 mb-0">
          남은 달에 {{ formatUtilityType(adjust.utilityType) }} 혼자
          {{ formatPercent(adjust.requiredRate) }}가 필요해요. {{ adjust.requiredAssumption }}.
        </p>
      </GpCard>

      <EcoTierDowngradeNotice
        :downgrade="adjust.tierDowngrade"
        @edit-goal="router.push('/whatif/goal')"
      />

      <GpCard
        title="실천 다시 고르기"
        caption="추천은 이미 고른 것과 기기군이 겹치지 않는 실천이에요"
      >
        <template #action>
          <button
            type="button"
            class="text-label text-primary-on-soft cursor-pointer border-0 bg-transparent p-0"
            @click="applyRecommended"
          >
            추천 반영
          </button>
        </template>

        <div class="border-divider divide-divider divide-y border-t">
          <GpMissionRow
            v-for="mission in adjust.missions"
            :key="mission.missionId"
            :mission="mission"
            :model-value="isSelected(mission.missionId)"
            :counted="previewItemById.get(mission.missionId)?.counted ?? true"
            :exclusion-reason="previewItemById.get(mission.missionId)?.exclusionReason ?? ''"
            :recommended="mission.recommended"
            @update:model-value="toggle(mission.missionId, $event)"
          />
        </div>
      </GpCard>

      <GpCard title="고른 실천 합계">
        <div class="flex items-center justify-between gap-3">
          <span class="text-body text-ink-soft">회차 전체 기준</span>
          <GpDelta :value="combinedRate" word="줄일 수 있어요" />
        </div>
        <p class="text-caption mt-2 mb-0" :class="coversRequired ? 'text-on-positive' : 'text-muted'">
          <template v-if="coversRequired">
            남은 달에 필요한 {{ formatPercent(adjust.requiredRate) }}를 덮을 수 있어요
          </template>
          <template v-else>
            남은 달에 필요한 {{ formatPercent(adjust.requiredRate) }}에는 아직 모자라요
          </template>
        </p>
      </GpCard>

      <p v-if="store.missionAdjustSaveError" class="text-caption text-negative mt-0 mb-0">
        {{ store.missionAdjustSaveError.message }}
      </p>
    </div>

    <template #footer>
      <div
        class="bg-canvas border-divider fixed inset-x-0 bottom-0 z-20 mx-auto max-w-(--gp-viewport-w) border-t px-(--gp-gutter) pt-3 pb-[max(12px,env(safe-area-inset-bottom))]"
      >
        <GpButton :disabled="!adjust || store.missionAdjustSaveLoading" @click="save">
          {{ store.missionAdjustSaveLoading ? '저장하는 중이에요' : '실천 저장하기' }}
        </GpButton>
      </div>
    </template>
  </AppSubLayout>
</template>
