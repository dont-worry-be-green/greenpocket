<script setup>
import GpTag from '@/components/ui/GpTag.vue'
import IconChevronRight from '@/components/ui/icons/IconChevronRight.vue'
import {
  formatApplicationStatus,
  formatPolicyCategory,
  formatPolicyDate,
  policyMatchMeta,
} from '@/utils/policy'

const props = defineProps({
  policy: { type: Object, required: true },
  compact: { type: Boolean, default: false },
})
defineEmits(['select'])

const match = () => policyMatchMeta(props.policy.matchStatus)
</script>

<template>
  <button
    type="button"
    class="bg-surface border-border w-full cursor-pointer rounded-lg border p-4 text-left"
    @click="$emit('select', policy)"
  >
    <div class="flex items-start justify-between gap-3">
      <div class="flex min-w-0 flex-wrap gap-1.5">
        <GpTag tone="primary" small>{{ formatPolicyCategory(policy.category) }}</GpTag>
        <GpTag v-if="policy.subCategory" small>{{ policy.subCategory }}</GpTag>
      </div>
      <IconChevronRight :size="18" class="text-icon-off mt-0.5 flex-none" />
    </div>

    <h3 class="text-list-title text-ink mt-3 mb-0 break-keep">{{ policy.title }}</h3>
    <p
      v-if="policy.supportSummary && !compact"
      class="text-body-sm text-ink-soft mt-2 mb-0 line-clamp-3"
    >
      {{ policy.supportSummary }}
    </p>

    <div class="border-divider mt-3 flex flex-wrap items-center justify-between gap-2 border-0 border-t pt-3">
      <GpTag :tone="match().tone" small>{{ match().label }}</GpTag>
      <span class="text-caption text-muted">
        {{ formatApplicationStatus(policy.applicationStatus) }} ·
        {{ formatPolicyDate(policy.applicationEndDate) }}
      </span>
    </div>
  </button>
</template>
