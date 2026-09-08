<script setup>
import OnbChoiceChips from '@/components/onboarding/OnbChoiceChips.vue'
import OnbRadioList from '@/components/onboarding/OnbRadioList.vue'
import {
  ANNUAL_INCOME_OPTIONS,
  CURRENT_STATUS_OPTIONS,
  HOUSEHOLD_STATUS_OPTIONS,
} from '@/utils/policy'

defineProps({
  currentStatus: { type: String, default: null },
  annualIncomeBand: { type: String, default: null },
  householdStatus: { type: String, default: null },
})
defineEmits(['update:currentStatus', 'update:annualIncomeBand', 'update:householdStatus'])
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
      :model-value="householdStatus"
      :options="HOUSEHOLD_STATUS_OPTIONS"
      label="가구 상태"
      @update:model-value="$emit('update:householdStatus', $event)"
    />
  </div>
</template>
