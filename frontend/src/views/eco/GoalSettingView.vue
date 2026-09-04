<script setup>
/*
 * 평가 기간 목표 정하기 — WF-04(등록) · WF-05(일부 미등록)
 *
 * **둘은 라우트가 아니라 한 화면의 두 상태다.** `segments[].registered` 가 false 인 요금이
 * 섞여 있으면 그 탭만 안내로 바뀐다. 경로를 나누면 새로고침·뒤로가기에서 상태가 어긋난다.
 *
 * ── 목표 초안은 스토어에 두지 않는다 ─────────────────────────────────────
 * 고른 구간 맵과 미션 체크 집합은 서버 데이터가 아니라 이 화면에서만 살다 죽는 폼 상태다.
 * 저장(`POST|PUT .../goal`)이 성공하면 서버가 기억하고, 다시 들어오면 `goal-form` 이
 * `selectedTier` · `missions[].selected` 로 되돌려 준다.
 *
 * ── 디바운스는 여기 있다 ─────────────────────────────────────────────────
 * 칩·체크를 바꿀 때마다 미리보기를 부르면 요청이 쏟아진다. 250ms 로 묶는다.
 * 스토어가 타이머를 들면 테스트에서 시간을 흘려야 해 검증이 어려워진다(늦게 온 응답을 버리는
 * 시퀀스 가드만 스토어가 맡는다).
 *
 * ── POST 냐 PUT 이냐 ────────────────────────────────────────────────────
 * `store.goalSet` 이 판단한다. 진입 경로로 가르면 새로고침 후 틀린다.
 */
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import EcoGoalSegment from '@/components/eco/EcoGoalSegment.vue'
import EcoGoalSummary from '@/components/eco/EcoGoalSummary.vue'
import EcoMissionPicker from '@/components/eco/EcoMissionPicker.vue'
import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import { useEcoStore } from '@/stores/eco'
import { formatRoundPeriod, formatUtilityType } from '@/utils/format'

const router = useRouter()
const store = useEcoStore()

// 폼 상태 (뷰 로컬)
const tierByUtility = ref({})
const selectedMissionIds = ref([])
const activeUtility = ref(null)

const goalForm = computed(() => store.goalForm)
const segments = computed(() => goalForm.value?.segments ?? [])
const activeSegment = computed(
  () =>
    segments.value.find((segment) => segment.utilityType === activeUtility.value) ??
    segments.value[0] ??
    null,
)

const period = computed(() =>
  goalForm.value ? formatRoundPeriod(goalForm.value.periodStart, goalForm.value.periodEnd) : '',
)

const preview = computed(() => store.goalPreview)
const targetByUtility = computed(
  () => new Map((preview.value?.utilities ?? []).map((item) => [item.utilityType, item])),
)

/** 미등록 요금은 목표를 만들 수 없어 targets 에서 뺀다 (서버는 409 로 막는다) */
const payload = computed(() => ({
  targets: segments.value
    .filter((segment) => segment.registered && tierByUtility.value[segment.utilityType])
    .map((segment) => ({
      utilityType: segment.utilityType,
      tier: tierByUtility.value[segment.utilityType],
    })),
  selectedMissionIds: selectedMissionIds.value,
}))

const canSave = computed(() => payload.value.targets.length > 0 && !store.goalSaveLoading)

// ── 미리보기 ────────────────────────────────────────────────────────────

let previewTimer = null
let firstPreview = true

function requestPreview(delay) {
  window.clearTimeout(previewTimer)
  const roundId = store.roundId
  if (!roundId) return
  previewTimer = window.setTimeout(() => store.fetchGoalPreview(roundId, payload.value), delay)
}
onUnmounted(() => window.clearTimeout(previewTimer))

// 첫 계산만 바로 돌린다. 이후 조작은 250ms 로 묶는다
watch(payload, () => {
  requestPreview(firstPreview ? 0 : 250)
  firstPreview = false
})

// ── 진입 ────────────────────────────────────────────────────────────────

/** 저장된 목표가 있으면 서버가 되돌려 준 값으로 초안을 채운다 */
watch(goalForm, (form) => {
  if (!form) return
  tierByUtility.value = Object.fromEntries(
    form.segments.map((segment) => [segment.utilityType, segment.selectedTier]),
  )
  selectedMissionIds.value = form.segments.flatMap((segment) =>
    segment.missions.filter((mission) => mission.selected).map((mission) => mission.missionId),
  )
  if (!activeUtility.value) activeUtility.value = form.segments[0]?.utilityType ?? null
})

async function load() {
  // 홈을 거치지 않고 바로 들어올 수 있다. roundId 를 회차 조회로도 채운다
  if (!store.roundId) await store.fetchCurrentRound()
  if (!store.roundId) return
  await store.fetchGoalForm(store.roundId)
}
onMounted(load)

// ── 저장 ────────────────────────────────────────────────────────────────

async function save() {
  const roundId = store.roundId
  if (!roundId) return
  const saved = await store.saveGoal(roundId, payload.value)
  if (saved) router.push('/whatif')
}
</script>

<template>
  <AppSubLayout title="평가 기간 목표 정하기" back="/whatif" has-footer>
    <p v-if="period" class="text-caption text-muted mt-0 mb-4">
      {{ period }} · 직전 2년 같은 기간 평균과 비교해요
    </p>

    <!-- 로딩·실패·빈 결과를 남기지 않는다 (COM-08) -->
    <p v-if="store.isLoading && !goalForm" class="text-caption text-muted py-10 text-center">
      목표 정보를 불러오는 중이에요
    </p>

    <div v-else-if="!goalForm" class="py-10 text-center">
      <p class="text-caption text-muted mt-0 mb-4">
        {{ store.error?.message || '목표 정보를 불러오지 못했어요' }}
      </p>
      <GpButton variant="pill" size="pill" @click="load">다시 시도</GpButton>
    </div>

    <div v-else class="space-y-4">
      <!-- 요금 3종 전환. 한 화면에 세 벌을 쌓으면 스크롤이 너무 길어진다 -->
      <div class="bg-surface-sub flex gap-1 rounded-md p-1" role="tablist">
        <button
          v-for="segment in segments"
          :key="segment.utilityType"
          type="button"
          role="tab"
          :aria-selected="segment.utilityType === activeSegment?.utilityType"
          class="ease-standard text-label flex-1 cursor-pointer rounded-sm border-0 py-2 font-semibold transition-colors duration-140"
          :class="
            segment.utilityType === activeSegment?.utilityType
              ? 'bg-surface text-ink'
              : 'bg-transparent text-muted'
          "
          @click="activeUtility = segment.utilityType"
        >
          {{ formatUtilityType(segment.utilityType) }}
          <span v-if="!segment.registered" class="text-muted font-normal"> · 미등록</span>
        </button>
      </div>

      <template v-if="activeSegment">
        <EcoGoalSegment
          v-model="tierByUtility[activeSegment.utilityType]"
          :segment="activeSegment"
          :tiers="goalForm.tiers"
          :target="targetByUtility.get(activeSegment.utilityType) ?? null"
        />

        <EcoMissionPicker
          v-model:selected-ids="selectedMissionIds"
          :segment="activeSegment"
          :preview-items="preview?.missions?.items ?? []"
          :summary="preview?.missions ?? null"
        />
      </template>

      <EcoGoalSummary
        :combined="preview?.combined ?? null"
        :utilities="preview?.utilities ?? []"
        :loading="store.previewLoading"
      />

      <p v-if="store.goalSaveError" class="text-caption text-negative mt-0 mb-0">
        {{ store.goalSaveError.message }}
      </p>
    </div>

    <template #footer>
      <div
        class="bg-canvas border-divider fixed inset-x-0 bottom-0 z-20 mx-auto max-w-(--gp-viewport-w) border-t px-(--gp-gutter) pt-3 pb-[max(12px,env(safe-area-inset-bottom))]"
      >
        <GpButton :disabled="!canSave" @click="save">
          {{ store.goalSaveLoading ? '저장하는 중이에요' : '목표 저장하기' }}
        </GpButton>
      </div>
    </template>
  </AppSubLayout>
</template>
