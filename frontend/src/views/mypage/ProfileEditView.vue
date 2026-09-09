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
  <AppSubLayout title="추천 조건 설정" back="/mypage" center-title>
    <MypageState :loading="bootstrapping" :error="store.profileError" @retry="load">
      <div v-if="store.policyPreferences" class="space-y-6">
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
