<script setup>
import OnbChoiceChips from '@/components/onboarding/OnbChoiceChips.vue'
import OnbRadioList from '@/components/onboarding/OnbRadioList.vue'
import {
  ANNUAL_INCOME_OPTIONS,
  CURRENT_STATUS_OPTIONS,
  EDUCATION_STATUS_OPTIONS,
  POLICY_CATEGORY_OPTIONS,
} from '@/utils/policy'

const props = defineProps({
  currentStatus: { type: String, default: null },
  annualIncomeBand: { type: String, default: null },
  educationStatus: { type: String, default: null },
  interestCategories: { type: Array, default: () => [] },
})
const emit = defineEmits([
  'update:currentStatus',
  'update:annualIncomeBand',
  'update:educationStatus',
  'update:interestCategories',
])

const interestOptions = POLICY_CATEGORY_OPTIONS.filter((option) => option.value)

function toggleInterest(value) {
  const selected = props.interestCategories.includes(value)
  if (selected) {
    emit(
      'update:interestCategories',
      props.interestCategories.filter((item) => item !== value),
    )
    return
  }
  if (props.interestCategories.length < 2) {
    emit('update:interestCategories', [...props.interestCategories, value])
  }
}
</script>

<template>
  <div class="space-y-7">
    <OnbChoiceChips
      :model-value="currentStatus"
      :options="CURRENT_STATUS_OPTIONS"
      label="현재 상태"
      @update:model-value="$emit('update:currentStatus', $event)"
    />
    <OnbRadioList
      :model-value="annualIncomeBand"
      :options="ANNUAL_INCOME_OPTIONS"
      label="연소득 구간"
      @update:model-value="$emit('update:annualIncomeBand', $event)"
    />
    <OnbChoiceChips
      :model-value="educationStatus"
      :options="EDUCATION_STATUS_OPTIONS"
      label="학력 상태"
      @update:model-value="$emit('update:educationStatus', $event)"
    />
    <div>
      <span class="text-body-strong text-muted mb-1 block">관심 분야</span>
      <span class="text-caption text-muted mb-3 block"
        >추천 순서를 정하는 데 사용할 분야를 최대 2개 선택해 주세요.</span
      >
      <div class="flex flex-wrap gap-2" role="group" aria-label="관심 분야">
        <button
          v-for="option in interestOptions"
          :key="option.value"
          type="button"
          :aria-pressed="interestCategories.includes(option.value)"
          :disabled="!interestCategories.includes(option.value) && interestCategories.length >= 2"
          class="ease-standard text-body-strong min-h-11 cursor-pointer rounded-full border px-3.5 transition-colors duration-140 disabled:cursor-not-allowed disabled:opacity-40"
          :class="
            interestCategories.includes(option.value)
              ? 'bg-primary border-primary text-on-primary'
              : 'bg-surface border-border text-ink-soft'
          "
          @click="toggleInterest(option.value)"
        >
          {{ option.label }}
        </button>
      </div>
    </div>
  </div>
</template>
