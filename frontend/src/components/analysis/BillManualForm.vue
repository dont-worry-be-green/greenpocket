<script setup>
import { computed, reactive, ref } from 'vue'

import GpButton from '@/components/ui/GpButton.vue'
import IconDrop from '@/components/ui/icons/IconDrop.vue'
import IconFlame from '@/components/ui/icons/IconFlame.vue'
import IconLightning from '@/components/ui/icons/IconLightning.vue'
import { useAnalysisStore } from '@/stores/analysis'

const props = defineProps({
  billingMonth: { type: String, required: true },
  initialBillType: { type: String, default: 'MANAGEMENT' },
})
const emit = defineEmits(['complete'])

const BILL_TYPES = [
  { value: 'MANAGEMENT', label: '관리비' },
  { value: 'ELECTRICITY', label: '전기' },
  { value: 'WATER', label: '수도' },
  { value: 'GAS', label: '도시가스' },
]
const UTILITIES = [
  { value: 'ELECTRICITY', label: '전기', unit: 'kWh', icon: IconLightning, tone: 'text-elec' },
  { value: 'WATER', label: '수도', unit: 'm3', icon: IconDrop, tone: 'text-water' },
  { value: 'GAS', label: '도시가스', unit: 'm3', icon: IconFlame, tone: 'text-gas' },
]

const store = useAnalysisStore()
const selectedBillType = ref(props.initialBillType)
const selectedMonth = ref(props.billingMonth)
const values = reactive({
  ELECTRICITY: { amount: '', usage: '' },
  WATER: { amount: '', usage: '' },
  GAS: { amount: '', usage: '' },
})
const submitted = ref(false)

const visibleUtilities = computed(() =>
  selectedBillType.value === 'MANAGEMENT'
    ? UTILITIES
    : UTILITIES.filter((item) => item.value === selectedBillType.value),
)
const isValid = computed(() =>
  visibleUtilities.value.every((item) => {
    const value = values[item.value]
    return value.amount !== '' && value.usage !== '' && Number(value.amount) >= 0 && Number(value.usage) >= 0
  }),
)

function keepInteger(event, utilityType) {
  values[utilityType].amount = event.target.value.replace(/\D/g, '')
}

function keepDecimal(event, utilityType) {
  values[utilityType].usage = event.target.value.replace(/[^\d.]/g, '').replace(/(\..*)\./g, '$1')
}

function selectBillType(type) {
  selectedBillType.value = type
  submitted.value = false
}

function submit() {
  submitted.value = true
  if (!isValid.value) return

  const draft = {
    billingMonth: selectedMonth.value,
    billType: selectedBillType.value,
    inputSource: 'MANUAL',
    items: visibleUtilities.value.map((item) => ({
      utilityType: item.value,
      amount: Number(values[item.value].amount),
      usage: Number(values[item.value].usage),
      usageUnit: item.unit,
      confidence: null,
    })),
  }
  store.saveBillDraft(draft)
  emit('complete', draft)
}
</script>

<template>
  <div>
    <fieldset class="mb-6 border-0 p-0">
      <legend class="text-body-strong text-muted mb-3">고지서 종류</legend>
      <div class="grid grid-cols-4 gap-2">
        <button
          v-for="type in BILL_TYPES"
          :key="type.value"
          type="button"
          class="min-h-11 rounded-full border text-label"
          :class="
            selectedBillType === type.value
              ? 'border-primary bg-primary text-on-primary'
              : 'border-control-border text-muted bg-transparent'
          "
          @click="selectBillType(type.value)"
        >
          {{ type.label }}
        </button>
      </div>
    </fieldset>

    <label class="mb-7 block">
      <span class="text-body-strong text-muted mb-3 block">청구 월</span>
      <span class="bg-surface flex min-h-16 items-center rounded-lg px-5">
        <input
          v-model="selectedMonth"
          type="month"
          class="text-list-title text-ink min-w-0 flex-1 border-0 bg-transparent outline-hidden"
          aria-label="청구 월"
        />
      </span>
    </label>

    <div class="space-y-4">
      <section
        v-for="utility in visibleUtilities"
        :key="utility.value"
        class="bg-surface rounded-lg p-5"
      >
        <h2 class="text-section text-ink mt-0 mb-4 flex items-center gap-2">
          <component :is="utility.icon" :size="20" :class="utility.tone" />
          {{ utility.label }}
        </h2>
        <div class="grid grid-cols-2 gap-3">
          <label class="bg-canvas rounded-md px-4 py-3">
            <span class="text-caption text-muted block">청구 금액</span>
            <span class="mt-1 flex items-end gap-1">
              <input
                :value="values[utility.value].amount"
                inputmode="numeric"
                placeholder="입력"
                class="text-button text-ink placeholder:text-control-off min-w-0 flex-1 border-0 bg-transparent outline-hidden"
                :aria-label="`${utility.label} 청구 금액`"
                @input="keepInteger($event, utility.value)"
              />
              <span class="text-caption text-muted">원</span>
            </span>
          </label>
          <label class="bg-canvas rounded-md px-4 py-3">
            <span class="text-caption text-muted block">사용량</span>
            <span class="mt-1 flex items-end gap-1">
              <input
                :value="values[utility.value].usage"
                inputmode="decimal"
                placeholder="입력"
                class="text-button text-ink placeholder:text-control-off min-w-0 flex-1 border-0 bg-transparent outline-hidden"
                :aria-label="`${utility.label} 사용량`"
                @input="keepDecimal($event, utility.value)"
              />
              <span class="text-caption text-muted">{{ utility.unit === 'm3' ? '㎥' : utility.unit }}</span>
            </span>
          </label>
        </div>
      </section>
    </div>

    <p v-if="submitted && !isValid" class="text-caption text-negative mt-3 mb-0" role="alert">
      청구 금액과 사용량을 모두 입력해 주세요.
    </p>

    <div class="bg-canvas fixed inset-x-0 bottom-0 z-10 mx-auto max-w-(--gp-viewport-w) px-(--gp-gutter) py-4">
      <GpButton @click="submit">등록하기</GpButton>
    </div>
  </div>
</template>
