<script setup>
/*
 * 기본 정보 표 — MY-01 (E-1-01)
 *
 * ⚠️ **나이·소득 구간·취업 상태 행이 없다.** 시안(image 464)에는 있지만 결정 B-1 로 제거됐고
 * `GET /mypage` 응답에도 그 필드가 없다(api-spec 14.1 "나이·소득 구간·취업 상태는 응답에 없습니다").
 * 시안을 보고 되살리지 않는다.
 *
 * 주거 형태와 평수는 시안대로 **한 행**이다('원룸 · 10평 이하'). 조립은 `formatHousing` 이 한다.
 */
import GpCard from '@/components/ui/GpCard.vue'
import IconPencil from '@/components/ui/icons/IconPencil.vue'
import { formatHousing } from '@/utils/format'

const props = defineProps({
  profile: { type: Object, required: true },
})
defineEmits(['edit'])

const rows = () => [
  { label: '이름', value: props.profile.name },
  { label: '지역', value: `${props.profile.sidoName} ${props.profile.sigunguName}` },
  { label: '주거 형태', value: formatHousing(props.profile.housingType, props.profile.areaBand) },
]
</script>

<template>
  <section>
    <header class="mb-2 flex items-center justify-between px-1">
      <h2 class="text-body-strong text-muted m-0">기본 정보</h2>
      <button
        type="button"
        class="text-body-strong text-primary-on-soft flex cursor-pointer items-center gap-1 border-0 bg-transparent p-1"
        @click="$emit('edit')"
      >
        <IconPencil :size="16" />
        수정
      </button>
    </header>

    <GpCard>
      <dl class="divide-divider m-0 divide-y">
        <div
          v-for="row in rows()"
          :key="row.label"
          class="flex items-center justify-between gap-3 py-3 first:pt-0 last:pb-0"
        >
          <dt class="text-body text-muted">{{ row.label }}</dt>
          <dd class="text-body-strong text-ink m-0 truncate text-right">{{ row.value }}</dd>
        </div>
      </dl>
    </GpCard>
  </section>
</template>
