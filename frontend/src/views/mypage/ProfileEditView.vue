<script setup>
/*
 * 기본 정보 수정 — MY-02 (A-1-06)
 *
 * ── 시안이 없다 ─────────────────────────────────────────────────────────
 * 기능명세서 화면 목록의 MY-02 칸이 "— (ONB-01·02 폼 재사용)" 이다. 그래서 **폼 조각을
 * 새로 만들지 않고 온보딩 것을 그대로 쓴다**(`components/onboarding/`). 도메인을 넘어
 * import 하지만 복제하지 않는 쪽을 골랐다 — 두 벌이 되면 온보딩에서 고른 선택지와 수정
 * 화면의 선택지가 조용히 달라진다. 규칙이 막는 것은 다른 도메인 **수정**이고, 여기서는
 * 읽기만 한다.
 *
 * 지역 목록도 같은 이유로 `useOnboardingStore` 의 것을 쓴다. `GET /meta/regions` 를
 * 마이페이지 스토어에 한 벌 더 두면 같은 목록을 두 곳에서 캐시하게 된다.
 * 저장만 마이페이지 스토어다 — 온보딩의 `saveProfile` 은 `POST` 이고 온보딩 완료 플래그까지
 * 남기므로 여기서 부르면 안 된다.
 *
 * ── 이름도 고칠 수 있다 ─────────────────────────────────────────────────
 * `PUT /profile` 은 `POST` 와 달리 `name` 을 받는다(api-spec 5.3). ONB-01 은 이름만 받는
 * 별도 화면이지만, 여기서는 한 화면에 모아 둔다 — 「기본 정보 수정」 하나로 들어왔는데
 * 이름을 고치러 또 이동해야 하면 수정이 두 화면으로 흩어진다.
 *
 * ── 지역을 바꾸면 확인을 받는다 ─────────────────────────────────────────
 * 진행 중 평가 회차가 있는데 지역이 바뀌면 서버가 `409` + `details.warning` 으로 막는다.
 * 그 문구로 확인을 받고 `confirmBaselineChange: true` 로 다시 부른다(A-1-06 예외).
 * **앱이 알아서 붙이지 않는다** — 사용자 선택을 말없이 바꾸지 않는다(핵심 규칙 9).
 */
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import MypageState from '@/components/mypage/MypageState.vue'
import OnbChoiceChips from '@/components/onboarding/OnbChoiceChips.vue'
import OnbRadioList from '@/components/onboarding/OnbRadioList.vue'
import OnbRegionPicker from '@/components/onboarding/OnbRegionPicker.vue'
import GpButton from '@/components/ui/GpButton.vue'
import GpModal from '@/components/ui/GpModal.vue'
import { useMypageStore } from '@/stores/mypage'
import { useOnboardingStore } from '@/stores/onboarding'
import { AREA_BAND_OPTIONS, HOUSING_TYPE_OPTIONS } from '@/utils/format'

const router = useRouter()
const store = useMypageStore()
const regions = useOnboardingStore()

// 폼 상태는 뷰 로컬이다. 저장에 성공한 것만 서버가 기억한다
const name = ref('')
const sido = ref(null)
const sigungu = ref(null)
const housingType = ref(null)
const areaBand = ref(null)

const bootstrapping = computed(() => !store.profile && !store.profileError)

const canSubmit = computed(
  () =>
    Boolean(name.value.trim() && sido.value && sigungu.value && housingType.value && areaBand.value) &&
    !store.saveLoading,
)

const payload = computed(() => ({
  name: name.value.trim(),
  sidoCode: sido.value?.code,
  sidoName: sido.value?.name,
  sigunguCode: sigungu.value?.code,
  sigunguName: sigungu.value?.name,
  housingType: housingType.value,
  areaBand: areaBand.value,
}))

// ── 진입 ────────────────────────────────────────────────────────────────

async function load() {
  const [profile] = await Promise.all([store.fetchProfile(), regions.fetchSidos()])
  if (!profile) return

  name.value = profile.name ?? ''
  housingType.value = profile.housingType
  areaBand.value = profile.areaBand

  /*
   * 시·도는 고르게 하지 않는다 — 서버가 1건만 준다(결정 C-15). ONB-02 와 같은 규칙이고
   * **코드('11')를 화면에 박지 않는다.** 저장된 시·도가 목록에 없으면 서버가 준 첫 항목을 쓴다.
   */
  const list = regions.sidos
  sido.value = list.find((item) => item.code === profile.sidoCode) ?? list[0] ?? null
  if (sido.value) await regions.fetchSigungus(sido.value.code)

  // 저장된 자치구를 목록에서 되찾아 고른 상태로 만든다. 없으면 비워 두고 다시 고르게 한다
  sigungu.value = regions.sigungus.find((item) => item.code === profile.sigunguCode) ?? null
}
onMounted(load)

// ── 저장 ────────────────────────────────────────────────────────────────

/*
 * `confirmBaselineChange` 는 **재요청에서만** 붙는다. 폼 값은 그대로이므로 payload 를
 * 따로 보관하지 않는다 — 보관하면 모달이 열린 동안 폼을 고쳐도 옛 값이 저장된다.
 */
async function submit(confirmBaselineChange = false) {
  if (!canSubmit.value) return

  const saved = await store.saveProfile(
    confirmBaselineChange ? { ...payload.value, confirmBaselineChange: true } : payload.value,
  )

  // 실패했으면 스토어가 saveError 나 baselineWarning 중 하나를 채운다. 화면은 그것만 본다
  if (saved) router.replace('/mypage')
}

function cancelWarning() {
  store.dismissBaselineWarning()
}

// 목록이 늦게 도착해도 저장된 자치구가 고른 상태가 되게 한다
watch(
  () => regions.sigungus,
  (list) => {
    if (sigungu.value || !store.profile) return
    sigungu.value = list.find((item) => item.code === store.profile.sigunguCode) ?? null
  },
)
</script>

<template>
  <AppSubLayout title="기본 정보 수정" back="/mypage">
    <MypageState :loading="bootstrapping" :error="store.profileError" @retry="load">
      <div class="space-y-6 pt-2">
        <div>
          <label for="my-profile-name" class="text-body-strong text-muted mb-3 block">이름</label>
          <input
            id="my-profile-name"
            v-model="name"
            type="text"
            maxlength="20"
            class="bg-surface border-border text-body text-ink min-h-14 w-full rounded-lg border px-4"
            placeholder="이름을 입력해 주세요"
          />
        </div>

        <OnbRegionPicker
          :sido="sido"
          :sigungus="regions.sigungus"
          :sigungu="sigungu"
          :sigungus-loading="regions.sigungusLoading"
          @update:sigungu="sigungu = $event"
        />

        <OnbChoiceChips v-model="housingType" :options="HOUSING_TYPE_OPTIONS" label="주거 형태" />
        <OnbRadioList v-model="areaBand" :options="AREA_BAND_OPTIONS" label="평수" />

        <p v-if="store.saveError" class="text-body-sm text-negative m-0">
          {{ store.saveError.message }}
        </p>

        <GpButton :disabled="!canSubmit" @click="submit()">
          {{ store.saveLoading ? '저장하는 중...' : '저장하기' }}
        </GpButton>
      </div>
    </MypageState>

    <!--
      지역 변경 경고 (A-1-06 예외). 반드시 골라야 하는 모달이라 백드롭·ESC 로 닫지 않는다 —
      기준이 바뀐다는 사실을 지나치면 진행 중 평가가 말없이 다른 기준으로 채점된다.
    -->
    <GpModal
      :open="Boolean(store.baselineWarning)"
      title="지역을 바꿀까요?"
      align="center"
      :dismissible="false"
      @close="cancelWarning"
    >
      <p class="text-body text-ink-soft mt-0 mb-5">{{ store.baselineWarning }}</p>
      <div class="space-y-2">
        <GpButton :disabled="store.saveLoading" @click="submit(true)">
          {{ store.saveLoading ? '저장하는 중...' : '그대로 바꾸기' }}
        </GpButton>
        <GpButton variant="ghost" size="wide" class="w-full" @click="cancelWarning">
          그만두기
        </GpButton>
      </div>
    </GpModal>
  </AppSubLayout>
</template>
