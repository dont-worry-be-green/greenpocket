<script setup>
/*
 * WF-06 에코마일리지 참여 신청 안내 (B-4-05)
 *
 * `application` 은 GET /eco/home 의 `application` 그대로다.
 * **띄울지 말지는 서버의 `showBanner` 가 정한다** — `applicationStatus` 를 보고 화면이 판정하지 않는다.
 *
 * 실제 신청은 누리집에서 한다. 여기서 하는 일은 **신청했다고 표시**하는 것뿐이라
 * 버튼 문구도 "누리집에서 신청하기"다. 우리가 신청을 대신했다고 읽히면 안 된다.
 *
 * 상태 라벨은 이 배너 한 곳에서만 쓰므로 utils/format.js 에 넣지 않고 여기 둔다.
 */
import { computed } from 'vue'

import GpButton from '@/components/ui/GpButton.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconWarning from '@/components/ui/icons/IconWarning.vue'

const props = defineProps({
  application: { type: Object, required: true },
  loading: { type: Boolean, default: false },
})
defineEmits(['apply'])

const STATUS = {
  NOT_APPLIED: { label: '신청 전', tone: 'sub' },
  APPLYING: { label: '신청 중', tone: 'sub' },
  APPLIED: { label: '신청 완료', tone: 'positive' },
  FAILED: { label: '신청 실패', tone: 'negative' },
}

const status = computed(() => STATUS[props.application.applicationStatus] ?? STATUS.NOT_APPLIED)
</script>

<template>
  <aside class="bg-confirmed-bg rounded-lg p-(--gp-card-pad)">
    <div class="flex items-start gap-2">
      <IconWarning :size="20" class="text-on-confirmed mt-0.5 shrink-0" />
      <div class="min-w-0 flex-1">
        <p class="text-body-strong text-ink m-0 flex items-center gap-2">
          아직 에코마일리지 회원이 아니에요
          <GpTag :tone="status.tone" small>{{ status.label }}</GpTag>
        </p>
        <p class="text-caption text-ink-soft mt-1 mb-0">
          회원이어야 평가 결과가 실제 마일리지로 적립돼요. 신청은 서울시 누리집에서 해요.
        </p>
      </div>
    </div>

    <GpButton
      variant="wide"
      size="wide"
      :disabled="loading"
      class="mt-3"
      @click="$emit('apply', application.externalUrl)"
    >
      {{ loading ? '표시하는 중…' : '누리집에서 신청하기' }}
    </GpButton>
  </aside>
</template>
