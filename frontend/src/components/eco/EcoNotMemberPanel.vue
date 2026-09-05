<script setup>
/*
 * WF-01b 에코마일리지 미가입 안내 — WF-01a 의 두 번째 상태다.
 *
 * ⚠️ **이 분기는 서버 판정이 아니다.** 사용자가 「아직 회원이 아니에요」를 직접 누른 결과다.
 * 공통 에러 코드에 없는 `ECO_NOT_MEMBER` 같은 코드를 만들지 않으려는 선택이고
 * (AGENTS.md 3절), 서버가 판정할 수 있게 되면 그때 교체한다.
 *
 * 가입은 누리집에서 한다. 여기서 대신해 주지 않는다 — WF-06 참여 신청 배너와 같은 태도다.
 */
import GpButton from '@/components/ui/GpButton.vue'
import GpCard from '@/components/ui/GpCard.vue'

defineProps({
  /** GET /eco/status 의 externalUrl (api-spec.md 8.1). 없으면 버튼을 감춘다 */
  externalUrl: { type: String, default: '' },
  loading: { type: Boolean, default: false },
})
const emit = defineEmits(['openSite', 'retry'])
</script>

<template>
  <div class="space-y-5">
    <div>
      <h1 class="text-title tracking-display text-ink m-0">먼저 가입이 필요해요</h1>
      <p class="text-body text-muted mt-2 mb-0">
        에코마일리지는 서울시가 운영하는 제도예요. 누리집에서 가입하고 돌아오면 사용량을
        불러올게요
      </p>
    </div>

    <GpCard title="가입할 때 필요한 것">
      <ul class="text-body-sm text-ink-soft m-0 list-none space-y-2 p-0">
        <li>지금 살고 있는 집 주소</li>
        <li>전기 고객번호 (고지서에 적혀 있어요)</li>
        <li>도시가스 · 수도 고객번호 (있으면 함께 등록해요)</li>
      </ul>
    </GpCard>

    <div class="space-y-3">
      <GpButton v-if="externalUrl" @click="emit('openSite')">누리집에서 가입하기</GpButton>
      <GpButton variant="wide" :disabled="loading" @click="emit('retry')">
        {{ loading ? '불러오는 중...' : '가입 다 했어요 · 불러오기' }}
      </GpButton>
    </div>

    <!--
      자동 재조회를 넣지 않는다. `visibilitychange` 는 탭 전환·알림·화면 잠금에도 걸려
      발표 중에 엉뚱한 시점에 연동이 시작된다. 수동 버튼은 C-1-01 에 선례가 있다.
    -->
    <p class="text-caption text-muted m-0">
      가입 직후에는 사용량이 아직 안 보일 수 있어요. 그럴 땐 잠시 뒤에 다시 눌러 주세요.
    </p>
  </div>
</template>
