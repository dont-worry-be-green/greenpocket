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

/*
 * ENUM 라벨은 api-spec.md 3절 · schema.sql 과 1:1 이다.
 * 이 화면 말고 쓰는 곳이 없어 `utils/format.js` 에 올리지 않는다.
 */
const HOUSING_TYPES = [
  { value: 'ONE_ROOM', label: '원룸' },
  { value: 'OFFICETEL', label: '오피스텔' },
  { value: 'APARTMENT', label: '아파트' },
  { value: 'MULTI_HOUSE', label: '다세대' },
]
const AREA_BANDS = [
  { value: 'UNDER_10', label: '10평 이하' },
  { value: 'FROM_10_TO_20', label: '10~20평' },
  { value: 'OVER_20', label: '20평 이상' },
]

const router = useRouter()
const store = useOnboardingStore()

// 폼 상태는 스토어가 아니라 뷰 로컬이다. 화면을 건너는 것은 store.user 뿐이다
const sido = ref(null)
const sigungu = ref(null)
const housingType = ref(null)
const areaBand = ref(null)

const canSubmit = computed(
  () =>
    Boolean(sido.value && sigungu.value && housingType.value && areaBand.value) && !store.isLoading,
)

onMounted(() => {
  // 이름을 저장하지 않고 주소창으로 들어온 경우다. 새로고침하면 store.user 가 null 이다
  if (!store.user) {
    router.replace('/onboarding/start')
    return
  }
  store.fetchSidos()
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
        <p class="text-body-sm text-muted mt-2 mb-0">
          같은 지역·같은 조건 가구와 비교하는 데 써요
        </p>
      </div>

      <OnbRegionPicker
        :sidos="store.sidos"
        :sigungus="store.sigungus"
        :sido="sido"
        :sigungu="sigungu"
        :sigungus-loading="store.sigungusLoading"
        @update:sido="selectSido"
        @update:sigungu="sigungu = $event"
      />

      <OnbChoiceChips v-model="housingType" :options="HOUSING_TYPES" label="주거 형태" />
      <OnbRadioList v-model="areaBand" :options="AREA_BANDS" label="평수" />

      <p v-if="store.error" class="text-body-sm text-negative m-0">{{ store.error.message }}</p>

      <!-- 시안대로 CTA 를 하단 고정이 아니라 콘텐츠 흐름 안에 둔다. 폼이 길어 가릴 것이 없다 -->
      <GpButton :disabled="!canSubmit" @click="submit">
        {{ store.isLoading ? '저장하는 중...' : '다음' }}
      </GpButton>
    </div>
  </AppSubLayout>
</template>
