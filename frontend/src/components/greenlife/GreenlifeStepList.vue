<script setup>
/*
 * 단계 목록 — BN-01 「참여 방법」(`joinSteps`) · BN-03 「실천 방법」(`practiceSteps`) 공용.
 *
 * 두 곳 모두 서버가 **문장 배열만** 준다(api-spec 12.1 · 12.4). 순서가 뜻을 가지는 가입 절차는
 * 번호를, 순서가 없는 실천 요령은 체크를 앞에 단다.
 *
 * ── 시안의 단계별 아이콘을 넣지 않았다 ──────────────────────────────────
 * BN-01 시안은 번호 옆에 회원가입·설정·적립 아이콘을 하나씩 달지만 응답에는 아이콘 키가 없다.
 * 인덱스로 3개를 박아 두면 서버가 문장을 하나 더하거나 순서를 바꾸는 순간 뜻과 그림이 어긋난다.
 * 번호만으로도 순서는 읽히므로 근거 없는 그림을 만들지 않는다.
 */
import IconCheck from '@/components/ui/icons/IconCheck.vue'

defineProps({
  steps: { type: Array, default: () => [] },
  variant: { type: String, default: 'number' }, // 'number' | 'check'
})
</script>

<template>
  <ol class="m-0 list-none space-y-2 p-0">
    <li
      v-for="(step, index) in steps"
      :key="index"
      class="bg-surface-sub flex items-center gap-3 rounded-md px-3 py-2.5"
    >
      <span
        v-if="variant === 'number'"
        class="bg-primary text-on-primary text-label flex size-6 flex-none items-center justify-center rounded-full"
      >
        {{ index + 1 }}
      </span>
      <span
        v-else
        class="bg-primary-bg text-primary flex size-6 flex-none items-center justify-center rounded-full"
      >
        <IconCheck :size="14" />
      </span>

      <span class="text-body-sm text-ink-soft">{{ step }}</span>
    </li>
  </ol>
</template>
