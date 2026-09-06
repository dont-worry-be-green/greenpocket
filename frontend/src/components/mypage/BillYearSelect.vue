<script setup>
/*
 * 연도 필터 — MY-03 (A-2-12 "탭과 연도 필터 조합")
 *
 * ⚠️ **연도 목록은 API 에 없다.** `GET /bills` 는 `year` 를 받기만 하고 고를 수 있는 연도를
 * 내려주지 않는다. 그래서 **받아 둔 목록의 청구 월에서 연도를 뽑는다**(뷰가 넘겨준다).
 * 올해를 상수로 박지 않는 이유 — 데모 데이터가 어느 연도에 있느냐에 따라 선택지가 달라져야 한다.
 *
 * 한계: 연도를 고른 상태에서는 그 연도 목록만 보이므로 선택지를 다시 뽑을 수 없다. 그래서
 * 뷰가 **전체 연도로 조회했을 때 뽑은 목록을 그대로 들고 있는다.** MVP 데이터(14건)는 한 페이지에
 * 다 들어와서 이 방식으로 충분하다.
 */
import { ref } from 'vue'

import GpModal from '@/components/ui/GpModal.vue'
import IconCheck from '@/components/ui/icons/IconCheck.vue'
import IconChevronDown from '@/components/ui/icons/IconChevronDown.vue'

const props = defineProps({
  /** 고를 수 있는 연도 문자열 배열. 최신순 */
  years: { type: Array, default: () => [] },
  /** null 이면 전체 연도 */
  modelValue: { type: String, default: null },
})
const emit = defineEmits(['update:modelValue'])

const open = ref(false)

function select(year) {
  open.value = false
  emit('update:modelValue', year)
}

const label = () => (props.modelValue ? `${props.modelValue}년` : '전체 연도')
</script>

<template>
  <div>
    <button
      type="button"
      class="bg-surface border-border text-body text-ink-soft flex h-(--gp-wbtn-h) cursor-pointer items-center gap-1.5 rounded-full border px-3.5"
      aria-haspopup="listbox"
      @click="open = true"
    >
      {{ label() }}
      <IconChevronDown :size="12" class="text-icon-off" />
    </button>

    <GpModal :open="open" title="연도 선택" @close="open = false">
      <ul class="m-0 list-none p-0" role="listbox" aria-label="연도">
        <li v-for="option in [null, ...years]" :key="option ?? 'ALL'">
          <button
            type="button"
            role="option"
            :aria-selected="option === modelValue"
            class="text-body flex min-h-12 w-full cursor-pointer items-center rounded-md border-0 bg-transparent px-3 text-left"
            :class="option === modelValue ? 'text-primary-on-soft font-semibold' : 'text-ink'"
            @click="select(option)"
          >
            <span class="flex-1">{{ option ? `${option}년` : '전체 연도' }}</span>
            <IconCheck v-if="option === modelValue" :size="18" />
          </button>
        </li>
      </ul>
    </GpModal>
  </div>
</template>
