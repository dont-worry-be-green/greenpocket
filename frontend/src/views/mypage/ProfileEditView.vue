<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import MypageState from '@/components/mypage/MypageState.vue'
import PolicyPreferenceForm from '@/components/mypage/PolicyPreferenceForm.vue'
import GpButton from '@/components/ui/GpButton.vue'
import GpCard from '@/components/ui/GpCard.vue'
import { useMypageStore } from '@/stores/mypage'

const router = useRouter()
const store = useMypageStore()

const currentStatus = ref(null)
const annualIncomeBand = ref(null)
const educationStatus = ref(null)
const interestCategories = ref([])

const bootstrapping = computed(() => !store.policyPreferences && !store.profileError)
const canSubmit = computed(
  () =>
    Boolean(currentStatus.value && annualIncomeBand.value && educationStatus.value) &&
    interestCategories.value.length >= 1 &&
    interestCategories.value.length <= 2 &&
    !store.saveLoading,
)

async function load() {
  const data = await store.fetchPolicyPreferences()
  if (!data) return
  currentStatus.value = data.currentStatus
  annualIncomeBand.value = data.annualIncomeBand
  educationStatus.value = data.educationStatus
  interestCategories.value = [...(data.interestCategories ?? [])]
}

async function submit() {
  if (!canSubmit.value) return
  const saved = await store.savePolicyPreferences({
    currentStatus: currentStatus.value,
    annualIncomeBand: annualIncomeBand.value,
    educationStatus: educationStatus.value,
    interestCategories: interestCategories.value,
  })
  if (saved) router.replace('/mypage')
}

onMounted(load)
</script>

<template>
  <AppSubLayout
    title="추천 조건 설정"
    subtitle="저장한 조건은 직접 바꾸기 전까지 계속 유지돼요"
    back="/mypage"
  >
    <MypageState :loading="bootstrapping" :error="store.profileError" @retry="load">
      <div v-if="store.policyPreferences" class="space-y-6">
        <GpCard
          title="바꿀 수 없는 정보"
          caption="본인인증과 에코마일리지 연동에서 가져온 정보예요."
        >
          <dl class="divide-divider m-0 divide-y">
            <div class="flex justify-between gap-3 py-3 first:pt-0">
              <dt class="text-body text-muted">생년월일</dt>
              <dd class="text-body-strong text-ink m-0">
                {{ store.policyPreferences.birthDate?.replaceAll('-', '.') ?? '-' }}
              </dd>
            </div>
            <div class="flex justify-between gap-3 py-3 last:pb-0">
              <dt class="text-body text-muted">거주지역</dt>
              <dd class="text-body-strong text-ink m-0 text-right">
                {{ store.policyPreferences.ecoAddress?.label ?? '에코마일리지 미연동' }}
              </dd>
            </div>
          </dl>
        </GpCard>

        <PolicyPreferenceForm
          v-model:current-status="currentStatus"
          v-model:annual-income-band="annualIncomeBand"
          v-model:education-status="educationStatus"
          v-model:interest-categories="interestCategories"
        />

        <p v-if="store.saveError" class="text-body-sm text-negative m-0">
          {{ store.saveError.message }}
        </p>

        <GpButton :disabled="!canSubmit" @click="submit">
          {{ store.saveLoading ? '저장하는 중...' : '저장하고 추천받기' }}
        </GpButton>
      </div>
    </MypageState>
  </AppSubLayout>
</template>
