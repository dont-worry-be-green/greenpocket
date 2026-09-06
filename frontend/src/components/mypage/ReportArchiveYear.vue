<script setup>
/*
 * 연도 그룹 — MY-04 (E-2-01 "연도별 목록(최신순) · 더 보기")
 *
 * 「더 보기」는 **페이징이 아니라 이 그룹 안의 접기**다. 시안에서 더 보기가 2026년 그룹 안에
 * 있고 그 아래에 2025년 그룹이 이어진다 — 다음 페이지를 불러오는 버튼이라면 연도 그룹 뒤에
 * 있어야 한다. 목록은 이미 다 받아 두었으므로 여기서 API 를 다시 부르지 않는다
 * (`GreenlifeItemList` 의 「17개 전체 보기」와 같은 방식).
 */
import { computed, ref } from 'vue'

import GpButton from '@/components/ui/GpButton.vue'
import IconChevronDown from '@/components/ui/icons/IconChevronDown.vue'

import ReportArchiveRow from './ReportArchiveRow.vue'

const props = defineProps({
  year: { type: String, required: true },
  reports: { type: Array, required: true },
  /** 갈 곳이 없는 리포트 ID 집합. 뷰가 라우팅 표로 판정해 넘긴다 */
  unroutableIds: { type: Array, default: () => [] },
  collapsedAfter: { type: Number, default: 4 },
})
defineEmits(['open'])

const expanded = ref(false)

const collapsible = computed(() => props.reports.length > props.collapsedAfter)
const visible = computed(() =>
  collapsible.value && !expanded.value ? props.reports.slice(0, props.collapsedAfter) : props.reports,
)
</script>

<template>
  <section>
    <h2 class="text-section tracking-display text-ink mt-0 mb-3 px-1">{{ year }}년</h2>

    <ul class="m-0 list-none space-y-2 p-0">
      <ReportArchiveRow
        v-for="report in visible"
        :key="report.reportId"
        :report="report"
        :disabled="unroutableIds.includes(report.reportId)"
        @open="$emit('open', $event)"
      />
    </ul>

    <div v-if="collapsible && !expanded" class="mt-3 flex justify-center">
      <GpButton variant="pill" size="pill" @click="expanded = true">
        더 보기
        <IconChevronDown :size="12" class="ml-1" />
      </GpButton>
    </div>
  </section>
</template>
