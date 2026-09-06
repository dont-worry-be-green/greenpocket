<script setup>
/*
 * 마일리지 적립 — WF-11 (B-5-03)
 *
 * ⚠️ 회차는 `route.params.roundId` 다(WF-10 과 같은 이유 — 확정된 것은 지난 회차다).
 *
 * ── 도메인 경계 ────────────────────────────────────────────────────────────
 * 「현금으로 바꾸기」는 `POST /pocket/conversions` 라 **포켓 도메인의 일이다.**
 * 여기서 전환을 실행하지 않고 `/pocket` 으로 보내기만 한다 — 동의 화면과 멱등키 처리가
 * 그쪽에 있고(핵심 규칙 4·5), 이 화면이 흉내 내면 전환이 두 곳에서 일어난다.
 *
 * ── 뒤로가기가 아니라 닫기다 ──────────────────────────────────────────────
 * 한 번 보고 닫는 결과 화면이라 헤더에 X 만 둔다(시안 WF-11). 「나중에 할래요」도 같은 뜻이라
 * 둘 다 홈으로 보낸다 — B-5-03 대로 **나중에 골라도 포켓 탭에서 전환할 수 있다.**
 *
 * `otherUses` 는 현금 말고 마일리지를 쓸 수 있는 곳이다. 카드 한 장이었는데 한 줄로 접었다 —
 * 지금 결정할 일(현금으로 바꿀지)과 나중 이야기가 같은 무게로 놓여 있었다.
 */
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import EcoAmountBreakdown from '@/components/eco/EcoAmountBreakdown.vue'
import EcoSettlementCard from '@/components/eco/EcoSettlementCard.vue'
import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import { useEcoStore } from '@/stores/eco'

const route = useRoute()
const router = useRouter()
const store = useEcoStore()

const roundId = computed(() => route.params.roundId)
const settlement = computed(() => store.settlement)

function load() {
  store.fetchSettlement(roundId.value)
}
watch(roundId, load, { immediate: true })

/** 전환은 포켓 탭에서 한다. 여기서 실행하지 않는다 */
const goConvert = () => router.push('/pocket')

/** 「나중에 할래요」. 포켓 탭에 미전환으로 남는다(B-5-03) */
const goLater = () => router.push('/whatif')

const otherUsesLabel = computed(() => {
  const uses = settlement.value?.otherUses ?? []
  return uses.length ? `마일리지는 ${uses.join(' · ')}에도 쓸 수 있어요` : ''
})
</script>

<template>
  <AppSubLayout dismiss back="/whatif" has-footer>
    <p v-if="store.isLoading && !settlement" class="text-caption text-muted py-10 text-center">
      적립 내역을 불러오는 중이에요
    </p>

    <div v-else-if="!settlement" class="py-10 text-center">
      <p class="text-caption text-muted mt-0 mb-4">
        {{ store.error?.message || '적립 내역을 불러오지 못했어요' }}
      </p>
      <GpButton variant="pill" size="pill" @click="load">다시 시도</GpButton>
    </div>

    <div v-else class="space-y-4">
      <EcoSettlementCard :settlement="settlement" />

      <!-- 판정 근거를 함께 둔다 (핵심 규칙 7). `note` 가 기준선 문구다 -->
      <EcoAmountBreakdown
        title="어떻게 계산됐나요"
        :baseline="settlement.calculation.baselineAmount"
        :actual="settlement.calculation.actualAmount"
        :saved="settlement.calculation.savedAmount"
        :note="settlement.calculation.note"
      />

      <!-- 돈의 3단계 중 ②→③ 경계. 이 화면에서 가장 오해가 잦은 지점이다 (핵심 규칙 2) -->
      <aside v-if="!settlement.isCash" class="bg-confirmed-bg rounded-lg p-(--gp-card-pad)">
        <p class="text-body-strong text-on-confirmed mt-0 mb-1">아직 현금이 아니에요</p>
        <p class="text-caption text-on-confirmed mt-0 mb-0 opacity-90">
          현금으로 바꿔야 그린포켓 계좌로 들어와요. 바꾸지 않으면 마일리지로만 남아 있어요.
        </p>
      </aside>

      <p v-if="otherUsesLabel" class="text-caption text-muted mt-0 mb-0 text-center">
        {{ otherUsesLabel }}
      </p>
    </div>

    <template #footer>
      <!-- `convertible` 은 서버가 판정한 값이다(회차당 1회 · 핵심 규칙 5). 화면이 다시 세지 않는다 -->
      <div
        v-if="settlement?.convertible"
        class="bg-canvas border-divider fixed inset-x-0 bottom-0 z-20 mx-auto max-w-(--gp-viewport-w) border-t px-(--gp-gutter) pt-3 pb-[max(12px,env(safe-area-inset-bottom))]"
      >
        <GpButton @click="goConvert">현금으로 바꾸기</GpButton>
        <button
          type="button"
          class="text-label text-primary-on-soft mt-1 w-full cursor-pointer border-0 bg-transparent p-2 font-semibold"
          @click="goLater"
        >
          나중에 할래요
        </button>
      </div>
    </template>
  </AppSubLayout>
</template>
