<script setup>
/*
 * 평가 기간 목표 정하기 — WF-04(등록) · WF-05(일부 미등록)
 *
 * **둘은 라우트가 아니라 한 화면의 두 상태다.** `segments[].registered` 가 false 인 요금이
 * 섞여 있으면 목표 카드의 그 행이 「미등록」으로, 그 미션 탭 위에 안내가 붙는다.
 * 경로를 나누면 새로고침·뒤로가기에서 상태가 어긋난다.
 *
 * ── 화면 순서 (2026-09-09 수현, 결정 C-37) ──────────────────────────────
 *   ① 목표 카드(EcoGoalPlanner) — 요금 3종 행 + 0·5·10·15 단계 바 + 합계 + 예상 마일리지
 *   ② 요금 세그먼트 — 아래 미션 카드의 카테고리 전환만 맡는다
 *   ③ 미션 카드(EcoMissionPicker) — 고른 요금의 실천 목록. 미등록이면 위에 EcoGoalSegment 안내
 * 탭으로 요금을 오가며 구간을 고르던 이전 구조는 「탭 → 구간 → 합산 → 다시 탭 → 미션」으로 끊겼다.
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
import { useRoute, useRouter } from 'vue-router'

import EcoGoalPlanner from '@/components/eco/EcoGoalPlanner.vue'
import EcoGoalSegment from '@/components/eco/EcoGoalSegment.vue'
import EcoMissionPicker from '@/components/eco/EcoMissionPicker.vue'
import UtilityIcon from '@/components/eco/UtilityIcon.vue'
import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import { useEcoStore } from '@/stores/eco'
import { formatRoundPeriod, formatUtilityType } from '@/utils/format'
import { currentMonth, seasonsBetween } from '@/utils/month'

const router = useRouter()
const route = useRoute()
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

/*
 * ── 계절이 안 맞는 미션은 보여주지 않는다 (결정 C-34) ─────────────────────
 * 오늘의 실천은 오늘 계절 태그로 미션을 거른다(B-3-05). 9월에 여름 냉방 미션을 고르면 이번 회차
 * 안에서 한 번도 홈에 안 뜬다. 그래서 **오늘부터 회차 끝까지 남은 달의 계절**과 하나라도 맞는
 * 미션(사계절 포함)만 보여준다 — 「오늘 계절」만 보면 10월에 연 10~3월 회차에서 겨울 난방 미션을
 * 못 고른다.
 * 이미 고른 미션은 계절이 안 맞아도 남긴다 — 그래야 해제할 수 있다(C-19 「이미 선택한 미션 유지」).
 * 회차가 끝난 뒤(남은 달 없음)에는 거르지 않는다 — 빈 목록보다 낫다.
 */
const remainingSeasons = computed(() => {
  const form = goalForm.value
  if (!form?.periodEnd) return []
  const today = currentMonth()
  const from = form.periodStart && today < form.periodStart ? form.periodStart : today
  return seasonsBetween(from, form.periodEnd)
})

function inSeason(mission) {
  const tags = Array.isArray(mission.seasonTags) ? mission.seasonTags : []
  if (remainingSeasons.value.length === 0 || tags.length === 0) return true
  return tags.some((tag) => remainingSeasons.value.includes(tag))
}

const visibleSegment = computed(() => {
  const segment = activeSegment.value
  if (!segment) return null
  return {
    ...segment,
    missions: segment.missions.filter(
      (mission) => inSeason(mission) || selectedMissionIds.value.includes(mission.missionId),
    ),
  }
})

const preview = computed(() => store.goalPreview)
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

/*
 * 구간을 하나도 안 고른 상태(`targets: []`)에서는 부르지 않는다 — 서버가 `ECO_TIER_INVALID` 400 을 준다.
 * 미션만 먼저 눌러도 요청이 나가던 버그. 구간을 고르면 그때 첫 미리보기가 나간다.
 */
function requestPreview(delay) {
  window.clearTimeout(previewTimer)
  const roundId = store.roundId
  if (!roundId) return
  if (payload.value.targets.length === 0) {
    // 세 요금 전부 0 — 옛 미리보기가 남아 「30,000M」처럼 보이면 안 된다
    store.goalPreview = null
    return
  }
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
  // 월 리포트 처방이 `?utility=` 로 지목한 요금의 미션 탭을 먼저 연다(WF-07 → WF-04). 없으면 첫 요금
  if (!activeUtility.value) {
    const wanted = form.segments.find((segment) => segment.utilityType === route.query.utility)
    activeUtility.value = wanted?.utilityType ?? form.segments[0]?.utilityType ?? null
  }
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
  <AppSubLayout title="목표 설정" back="/whatif" center-title has-footer>
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
      <EcoGoalPlanner
        v-model="tierByUtility"
        :segments="segments"
        :tiers="goalForm.tiers"
        :preview="preview"
        :loading="store.previewLoading"
      />

      <!--
        요금 세그먼트 — 아래 미션 카드의 카테고리만 바꾼다. 미션 카드 바로 위에 붙여 「이 탭이 저 목록을
        고른다」가 보이게 한다. 진단 탭과 같은 슬라이딩 세그먼트 + 이름 왼쪽 요금색 아이콘(비선택도 색 유지).
      -->
      <div class="!mt-6 space-y-2">
        <!-- 목표 카드와 미션 영역이 다른 일이라는 걸 제목으로 가른다 (2026-09-09 수현) -->
        <div class="mb-3">
          <h2 class="text-section tracking-display m-0">실천 미션 고르기</h2>
          <p class="text-caption text-muted mt-0.5 mb-0">목표를 채울 실천을 요금별로 골라요</p>
        </div>
        <div
          class="bg-track grid grid-cols-3 gap-0.5 rounded-md p-[3px]"
          role="tablist"
          aria-label="실천 미션 요금 종류"
        >
          <button
            v-for="segment in segments"
            :key="segment.utilityType"
            type="button"
            role="tab"
            :aria-selected="segment.utilityType === activeSegment?.utilityType"
            class="ease-standard text-label flex h-10 cursor-pointer items-center justify-center gap-1.5 rounded-sm border-0 whitespace-nowrap transition duration-140"
            :class="
              segment.utilityType === activeSegment?.utilityType
                ? 'bg-surface text-ink font-extrabold shadow-[0_1px_2px_rgb(16_40_28/0.12),0_3px_8px_rgb(16_40_28/0.1)]'
                : 'text-muted bg-transparent font-semibold'
            "
            @click="activeUtility = segment.utilityType"
          >
            <UtilityIcon :utility-type="segment.utilityType" xs />
            {{ formatUtilityType(segment.utilityType) }}
            <span v-if="!segment.registered" class="text-badge text-muted">미등록</span>
          </button>
        </div>

        <template v-if="activeSegment">
          <!-- WF-05 · 미등록 요금이면 사유·등록 안내를 미션 위에 -->
          <EcoGoalSegment v-if="!activeSegment.registered" :segment="activeSegment" />

          <EcoMissionPicker
            v-model:selected-ids="selectedMissionIds"
            :segment="visibleSegment"
            :preview-items="preview?.missions?.items ?? []"
          />
        </template>
      </div>

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
