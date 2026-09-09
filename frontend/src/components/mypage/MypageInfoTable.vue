<script setup>
import GpCard from '@/components/ui/GpCard.vue'

const props = defineProps({
  profile: { type: Object, required: true },
  ecoAddress: { type: Object, default: null },
})

const genderLabel = () => ({ MALE: '남성', FEMALE: '여성' })[props.profile.gender] ?? '-'
const birthDate = () => props.profile.birthDate?.replaceAll('-', '.') ?? '-'
const phoneNumber = () => {
  const value = String(props.profile.phoneNumber ?? '').replace(/\D/g, '')
  if (value.length !== 11) return props.profile.phoneNumber || '-'
  return `${value.slice(0, 3)}-${value.slice(3, 7)}-${value.slice(7)}`
}
</script>

<template>
  <section aria-label="내 정보">
    <GpCard>
      <dl class="divide-divider m-0 divide-y">
        <div
          v-for="row in [
            { label: '생년월일', value: birthDate() },
            { label: '성별', value: genderLabel() },
            { label: '휴대전화', value: phoneNumber() },
            ...(ecoAddress ? [{ label: '주소', value: ecoAddress.label }] : []),
          ]"
          :key="row.label"
          class="flex items-center justify-between gap-3 py-3 first:pt-0 last:pb-0"
        >
          <dt class="text-body text-muted">{{ row.label }}</dt>
          <dd class="text-body-strong text-ink m-0 text-right">{{ row.value }}</dd>
        </div>
      </dl>
    </GpCard>
  </section>
</template>
