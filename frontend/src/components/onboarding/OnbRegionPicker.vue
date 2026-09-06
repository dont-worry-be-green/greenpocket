<script setup>
/*
 * 온보딩 · 지역 선택 (A-1-01 · ONB-02)
 *
 * ── 시·도는 고르지 않는다 ──────────────────────────────────────────────────
 * MVP 서비스 지역이 서울특별시뿐이라(결정 C-15) `GET /meta/regions` 는 시·도를 **1건만** 준다.
 * 항목이 하나인 모달을 띄우는 것은 고르는 시늉일 뿐이라, 서버가 준 값을 그대로 보여주고
 * 시·군·구만 고르게 한다. 자동 선택은 뷰(`ProfileView`)가 한다 — 여기는 표시만 맡는다.
 *
 * 목록은 서버가 준다. **기본값을 두지 않는다.**
 * `hasRegionAverage` 가 false 인 지역에는 A-1-01 의 안내 문구를 띄운다.
 */
import { ref } from 'vue'

import GpModal from '@/components/ui/GpModal.vue'
import IconCheck from '@/components/ui/icons/IconCheck.vue'
import IconChevronRight from '@/components/ui/icons/IconChevronRight.vue'
import IconInfo from '@/components/ui/icons/IconInfo.vue'

defineProps({
  /** 서버가 준 시·도. 뷰가 자동 선택해 넘긴다. 아직 못 받았으면 null */
  sido: { type: Object, default: null },
  sigungus: { type: Array, default: () => [] },
  sigungu: { type: Object, default: null },
  sigungusLoading: { type: Boolean, default: false },
})
const emit = defineEmits(['update:sigungu'])

const open = ref(false)

function selectSigungu(item) {
  open.value = false
  emit('update:sigungu', item)
}
</script>

<template>
  <div>
    <span class="text-body-strong text-muted mb-3 block">지역</span>

    <div class="space-y-2">
      <!-- 고를 것이 없는 값이라 버튼이 아니다. 비활성 버튼으로 두면 누를 수 있는 것처럼 보인다 -->
      <div
        class="bg-surface-sub border-divider text-body flex min-h-14 w-full items-center rounded-lg border px-4"
      >
        <span class="flex-1" :class="sido ? 'text-ink' : 'text-disabled-text'">
          {{ sido ? sido.name : '지역을 불러오는 중이에요…' }}
        </span>
      </div>

      <button
        type="button"
        :disabled="!sido"
        class="bg-surface border-border text-body disabled:bg-disabled-bg flex min-h-14 w-full items-center rounded-lg border px-4 text-left disabled:cursor-not-allowed"
        aria-haspopup="listbox"
        @click="open = true"
      >
        <span class="flex-1" :class="sigungu ? 'text-ink' : 'text-disabled-text'">
          {{ sigungu ? sigungu.name : '시 · 군 · 구' }}
        </span>
        <IconChevronRight :size="18" class="text-icon-off" />
      </button>
    </div>

    <p class="text-caption text-muted mt-2 mb-0">
      서울시 제도라 지금은 서울특별시만 고를 수 있어요.
    </p>

    <!-- 데이터 없음은 오류가 아니다(핵심 규칙 8). 빈 목록도 안내로 그린다 -->
    <p
      v-if="sido && !sigungusLoading && sigungus.length === 0"
      class="text-caption text-muted bg-surface-sub mt-3 mb-0 flex gap-2 rounded-md p-3"
    >
      <IconInfo :size="16" class="text-icon-off mt-0.5 shrink-0" aria-hidden="true" />
      <span>{{ sido.name }}의 시·군·구 목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.</span>
    </p>
    <p
      v-else-if="sigungu && !sigungu.hasRegionAverage"
      class="text-caption text-muted bg-surface-sub mt-3 mb-0 flex gap-2 rounded-md p-3"
    >
      <IconInfo :size="16" class="text-icon-off mt-0.5 shrink-0" aria-hidden="true" />
      <span>비교 자료가 없는 지역은 더 넓은 범위의 평균을 쓰고, 그 범위를 화면에 표시해요.</span>
    </p>

    <GpModal :open="open" title="시 · 군 · 구 선택" @close="open = false">
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
