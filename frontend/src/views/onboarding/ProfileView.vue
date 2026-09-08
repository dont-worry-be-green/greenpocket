<script setup>
/*
 * ONB-02 주거 프로필 (A-1-01 · A-1-02 · A-1-03 · A-1-05)
 *
 * 지역 → 주거 형태 → 평수 순서로 고르고, 넷 다 고르기 전에는 저장 CTA 가 비활성이다.
 * 저장하면 온보딩이 끝나고 홈인 What-if 탭으로 간다(`nextScreen: 'WF-06'` · 결정 C-1).
 *
 * 청년 조건(나이·소득·취업) 화면 ONB-03 은 삭제됐다(결정 B-1). 여기서 받지 않는다.
 *
 * ⚠️ `onMounted` 는 **첫 렌더 뒤에** 돈다. 그 한 틱 동안 `sidos` 는 빈 배열이므로
 * 템플릿이 그 상태를 견뎌야 한다(What-if 에서 흰 화면을 두 번 낸 원인).
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import OnbChoiceChips from '@/components/onboarding/OnbChoiceChips.vue'
import OnbProgress from '@/components/onboarding/OnbProgress.vue'
import OnbRadioList from '@/components/onboarding/OnbRadioList.vue'
import OnbRegionPicker from '@/components/onboarding/OnbRegionPicker.vue'
import GpButton from '@/components/ui/GpButton.vue'
import { useOnboardingStore } from '@/stores/onboarding'
import { AREA_BAND_OPTIONS, HOUSING_TYPE_OPTIONS } from '@/utils/format'

/*
 * ENUM 라벨은 api-spec.md 3절 · schema.sql 과 1:1 이다.
 * 여기 로컬 상수였는데 MY-01(라벨 되돌리기)·MY-02(같은 폼 재사용, A-1-06)가 함께 쓰게 되어
 * `utils/format.js` 로 올렸다. 선택지와 라벨이 갈라지면 두 화면의 문구가 조용히 달라진다.
 */

const router = useRouter()
const store = useOnboardingStore()

// 입력 중인 프로필은 이 화면에서만 관리하고 저장 성공 후 서버 상태를 다시 따른다
const sido = ref(null)
const sigungu = ref(null)
const housingType = ref(null)
const areaBand = ref(null)

const canSubmit = computed(
  () =>
    Boolean(sido.value && sigungu.value && housingType.value && areaBand.value) && !store.isLoading,
)

onMounted(async () => {
  /*
   * 여기서 진입을 막지 않는다. 로그인 여부는 가드가 본다(`router/guards.js`) —
   * 예전에는 스토어의 `user` 가 없으면 ONB-01 로 되돌려 보냈는데, 새로고침하면 그 값이
   * 사라져서 로그인을 마친 사람까지 밀려났다.
   */
  await store.fetchSidos()
  /*
   * 시·도는 고르게 하지 않는다 — MVP 서비스 지역이 서울뿐이라 서버가 1건만 준다(결정 C-15).
   * **코드('11')를 여기 박지 않는다.** 서버가 준 값을 그대로 써야 지역이 늘 때 화면이 따라간다.
   */
  const [only] = store.sidos
  if (only) selectSido(only)
})

function selectSido(item) {
  sido.value = item
  sigungu.value = null
  store.fetchSigungus(item.code)
}

async function submit() {
  if (!canSubmit.value) return

  const saved = await store.saveProfile({
    sidoCode: sido.value.code,
    sidoName: sido.value.name,
    sigunguCode: sigungu.value.code,
    sigunguName: sigungu.value.name,
    housingType: housingType.value,
    areaBand: areaBand.value,
  })

  if (saved) router.replace('/whatif')
}
</script>

<template>
  <!-- 시안에 헤더 제목이 없다. 본문 큰 제목이 그 자리를 대신한다 -->
  <AppSubLayout back="/onboarding/start">
    <OnbProgress :step="2" :total="2" />

    <div class="space-y-6 pt-5">
      <div>
        <h1 class="text-title tracking-display text-ink m-0">어디에 살고 계세요?</h1>
        <p class="text-body-sm text-muted mt-2 mb-0">같은 지역·같은 조건 가구와 비교하는 데 써요</p>
      </div>

      <OnbRegionPicker
        :sido="sido"
        :sigungus="store.sigungus"
        :sigungu="sigungu"
        :sigungus-loading="store.sigungusLoading"
        @update:sigungu="sigungu = $event"
      />

      <OnbChoiceChips v-model="housingType" :options="HOUSING_TYPE_OPTIONS" label="주거 형태" />
      <OnbRadioList v-model="areaBand" :options="AREA_BAND_OPTIONS" label="평수" />

      <p v-if="store.error" class="text-body-sm text-negative m-0">{{ store.error.message }}</p>

      <!-- 시안대로 CTA 를 하단 고정이 아니라 콘텐츠 흐름 안에 둔다. 폼이 길어 가릴 것이 없다 -->
      <GpButton :disabled="!canSubmit" @click="submit">
        {{ store.isLoading ? '저장하는 중...' : '다음' }}
      </GpButton>
    </div>
  </AppSubLayout>
</template>
