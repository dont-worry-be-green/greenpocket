<script setup>
/*
 * 온보딩 · 지역 선택 (A-1-01 · ONB-02)
 *
 * 시·도를 먼저 고르고 시·군·구를 고른다. **시·도 선택 전에는 시·군·구 버튼을 비활성화한다.**
 * 시·도를 바꾸면 고른 시·군·구를 비운다 — 다른 시도의 구가 남아 있으면 그대로 저장된다.
 *
 * 목록은 서버가 준다(`GET /meta/regions`). **기본값을 두지 않는다.**
 * `hasRegionAverage` 가 false 인 지역에는 A-1-01 의 안내 문구를 띄운다.
 */
import { ref } from 'vue'

import GpModal from '@/components/ui/GpModal.vue'
import IconCheck from '@/components/ui/icons/IconCheck.vue'
import IconChevronRight from '@/components/ui/icons/IconChevronRight.vue'
import IconInfo from '@/components/ui/icons/IconInfo.vue'

const props = defineProps({
  sidos: { type: Array, default: () => [] },
  sigungus: { type: Array, default: () => [] },
  sido: { type: Object, default: null },
  sigungu: { type: Object, default: null },
  sigungusLoading: { type: Boolean, default: false },
})
const emit = defineEmits(['update:sido', 'update:sigungu'])

const openLevel = ref(null) // 'SIDO' | 'SIGUNGU' | null

function selectSido(item) {
  openLevel.value = null
  if (item.code === props.sido?.code) return
  emit('update:sido', item)
}

function selectSigungu(item) {
  openLevel.value = null
  emit('update:sigungu', item)
}
</script>

<template>
  <div>
    <span class="text-body-strong text-muted mb-3 block">지역</span>

    <div class="space-y-2">
      <button
        type="button"
        class="bg-surface border-border text-body flex min-h-14 w-full items-center rounded-lg border px-4 text-left"
        aria-haspopup="listbox"
        @click="openLevel = 'SIDO'"
      >
        <span class="flex-1" :class="sido ? 'text-ink' : 'text-disabled-text'">
          {{ sido ? sido.name : '시 · 도' }}
        </span>
        <IconChevronRight :size="18" class="text-icon-off" />
      </button>

      <button
        type="button"
        :disabled="!sido"
        class="bg-surface border-border text-body flex min-h-14 w-full items-center rounded-lg border px-4 text-left disabled:bg-disabled-bg disabled:cursor-not-allowed"
        aria-haspopup="listbox"
        @click="openLevel = 'SIGUNGU'"
      >
        <span class="flex-1" :class="sigungu ? 'text-ink' : 'text-disabled-text'">
          {{ sigungu ? sigungu.name : '시 · 군 · 구' }}
        </span>
        <IconChevronRight :size="18" class="text-icon-off" />
      </button>
    </div>

    <!-- 데이터 없음은 오류가 아니다(핵심 규칙 8). 빈 목록도 안내로 그린다 -->
    <p
      v-if="sido && !sigungusLoading && sigungus.length === 0"
      class="text-caption text-muted bg-surface-sub mt-3 mb-0 flex gap-2 rounded-md p-3"
    >
      <IconInfo :size="16" class="text-icon-off mt-0.5 shrink-0" aria-hidden="true" />
      <span>
        {{ sido.name }}의 시·군·구 목록은 아직 준비되지 않았어요. 지금은 서울특별시만 고를 수
        있어요.
      </span>
    </p>
    <p
      v-else-if="sigungu && !sigungu.hasRegionAverage"
      class="text-caption text-muted bg-surface-sub mt-3 mb-0 flex gap-2 rounded-md p-3"
    >
      <IconInfo :size="16" class="text-icon-off mt-0.5 shrink-0" aria-hidden="true" />
      <span>비교 자료가 없는 지역은 더 넓은 범위의 평균을 쓰고, 그 범위를 화면에 표시해요.</span>
    </p>

    <GpModal :open="openLevel === 'SIDO'" title="시 · 도 선택" @close="openLevel = null">
      <ul
        class="m-0 max-h-[50vh] list-none overflow-y-auto p-0"
        role="listbox"
        aria-label="시 · 도"
      >
        <li v-for="item in sidos" :key="item.code">
          <button
            type="button"
            role="option"
            :aria-selected="item.code === sido?.code"
            class="text-body flex min-h-12 w-full items-center rounded-md border-0 bg-transparent px-3 text-left"
            :class="item.code === sido?.code ? 'text-primary-on-soft font-semibold' : 'text-ink'"
            @click="selectSido(item)"
          >
            <span class="flex-1">{{ item.name }}</span>
            <IconCheck v-if="item.code === sido?.code" :size="18" />
          </button>
        </li>
      </ul>
    </GpModal>

    <GpModal :open="openLevel === 'SIGUNGU'" title="시 · 군 · 구 선택" @close="openLevel = null">
      <p v-if="sigungusLoading" class="text-body text-muted m-0">목록을 불러오는 중이에요…</p>
      <p v-else-if="sigungus.length === 0" class="text-body text-muted m-0">
        고를 수 있는 시·군·구가 없어요.
      </p>
      <ul
        v-else
        class="m-0 max-h-[50vh] list-none overflow-y-auto p-0"
        role="listbox"
        aria-label="시 · 군 · 구"
      >
        <li v-for="item in sigungus" :key="item.code">
          <button
            type="button"
            role="option"
            :aria-selected="item.code === sigungu?.code"
            class="text-body flex min-h-12 w-full items-center rounded-md border-0 bg-transparent px-3 text-left"
            :class="item.code === sigungu?.code ? 'text-primary-on-soft font-semibold' : 'text-ink'"
            @click="selectSigungu(item)"
          >
            <span class="flex-1">{{ item.name }}</span>
            <IconCheck v-if="item.code === sigungu?.code" :size="18" />
          </button>
        </li>
      </ul>
    </GpModal>
  </div>
</template>
