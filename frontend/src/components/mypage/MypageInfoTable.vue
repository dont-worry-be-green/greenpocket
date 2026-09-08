<script setup>
/*
 * 기본 정보 표 — MY-01 (E-1-01)
 *
 * 주거 형태와 평수는 시안대로 **한 행**이다('원룸 · 10평 이하'). 조립은 `formatHousing` 이 한다.
 * 지역과 주거 형태만 기본으로 보이고, 맞춤 지원금용 추가 정보는 같은 행 디자인으로 펼친다.
 * 값이 아직 없는 항목도 펼친 영역에서 `-`로 표시해 정보 구조를 항상 확인할 수 있게 한다.
 */
import { ref } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import IconChevronDown from '@/components/ui/icons/IconChevronDown.vue'
import { formatHousing } from '@/utils/format'

const props = defineProps({
  profile: { type: Object, required: true },
})

const expanded = ref(false)

const valueOrDash = (value) => value || '-'

function formatBirthDate(value) {
  if (!value) return '-'
  return /^\d{4}-\d{2}-\d{2}$/.test(value) ? value.replaceAll('-', '.') : value
}

function formatInterests(value) {
  if (!Array.isArray(value) || !value.length) return '-'
  return value.slice(0, 3).join(' · ')
}

const primaryRows = () => [
  { label: '지역', value: `${props.profile.sidoName} ${props.profile.sigunguName}` },
  { label: '주거 형태', value: formatHousing(props.profile.housingType, props.profile.areaBand) },
]

const detailRows = () => [
  { label: '생년월일', value: formatBirthDate(props.profile.birthDate) },
  { label: '현재 상태', value: valueOrDash(props.profile.currentStatus) },
  { label: '연소득 구간', value: valueOrDash(props.profile.annualIncomeBand) },
  {
    label: '가구·주거 상황',
    value: valueOrDash(props.profile.householdHousingSituation),
  },
  { label: '관심 분야', value: formatInterests(props.profile.interestAreas) },
]

const visibleRows = () =>
  expanded.value ? [...primaryRows(), ...detailRows()] : primaryRows()
</script>

<template>
  <section aria-label="기본 정보">
    <GpCard>
      <dl id="mypage-basic-info" class="divide-divider m-0 divide-y">
        <div
          v-for="row in visibleRows()"
          :key="row.label"
          class="flex items-start justify-between gap-3 py-3 first:pt-0"
        >
          <dt class="text-body text-muted shrink-0">{{ row.label }}</dt>
          <dd class="text-body-strong text-ink m-0 break-keep text-right">{{ row.value }}</dd>
        </div>
      </dl>

      <button
        type="button"
        class="text-body-sm text-primary-on-soft mx-auto flex cursor-pointer items-center gap-1 border-0 bg-transparent pt-3 font-semibold"
        aria-controls="mypage-basic-info"
        :aria-expanded="expanded"
        @click="expanded = !expanded"
      >
        {{ expanded ? '접기' : '더 보기' }}
        <IconChevronDown
          :size="14"
          class="transition-transform duration-200"
          :class="expanded ? 'rotate-180' : ''"
        />
      </button>
    </GpCard>
  </section>
</template>
