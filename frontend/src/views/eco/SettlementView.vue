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
 * 한 번 보고 닫는 결과 화면이라 헤더에 X 만 둔다(시안 WF-11). ⚠️ 「나중에 할래요」 버튼은
 * **수현 결정으로 뺐다**(2026-09-10 · 결정 C-39) — 같은 뜻인 X 닫기가 홈으로 보내고,
 * B-5-03 완료 조건대로 **닫은 뒤에도 포켓 탭에서 전환할 수 있다.**
 *
 * `otherUses` 는 현금 말고 마일리지를 쓸 수 있는 곳이다. 카드 한 장이었는데 한 줄로 접었다 —
 * 지금 결정할 일(현금으로 바꿀지)과 나중 이야기가 같은 무게로 놓여 있었다.
 *
 * ── 화면 구성 (2026-09-10 수현 · 결정 C-39) ──────────────────────────────────
 * 헤더(동전·금액·기간) → 스탯 타일 셋(감축률·구간·덜 낸 요금) → 요금 비교 카드(막대 둘) →
 * 다른 쓰임새 한 줄 → 고정 푸터(「아직 현금이 아니에요」 인라인 노티스 + CTA 하나).
 * 색면은 0 장이다. 앰버 경고 카드는 CTA 위 한 줄로 내렸다 — 버튼을 누르기 직전에 읽히는 자리다.
 * 기각: 앰버 경고 카드 · 요금 비교 카드 안 차액 행 · 헤더 캡션의 「사용량 R% 줄여 구간」(타일과 중복).
 */
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import EcoAmountBreakdown from '@/components/eco/EcoAmountBreakdown.vue'
import EcoSettlementCard from '@/components/eco/EcoSettlementCard.vue'
import EcoSettlementStats from '@/components/eco/EcoSettlementStats.vue'
import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import IconWarning from '@/components/ui/icons/IconWarning.vue'
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

    <div v-else class="space-y-3">
      <EcoSettlementCard :settlement="settlement" />

      <!-- 감축률 → 구간 → 덜 낸 요금. 순서가 곧 인과다 (결정 C-38·C-39) -->
      <EcoSettlementStats
        :cumulative-rate="settlement.cumulativeRate"
        :tier="settlement.tier"
        :saved="settlement.calculation.savedAmount"
      />

      <!--
        요금 막대 둘은 원화 성과 표시다 (핵심 규칙 1·7). 마일리지 계산 근거가 아니라서
        「어떻게 계산됐나요」라 부르지 않는다 (결정 C-38). `note` 가 기준선 문구다
      -->
      <!-- 타일 셋과는 카드 간격(12)보다 넓게 띄운다 — 타일이 헤더 쪽 묶음이라는 뜻 -->
      <EcoAmountBreakdown
        title="요금 비교"
        class="mt-5!"
        :baseline="settlement.calculation.baselineAmount"
        :actual="settlement.calculation.actualAmount"
        :note="settlement.calculation.note"
      />

      <p v-if="otherUsesLabel" class="text-caption text-muted mt-0 mb-0 text-center">
        {{ otherUsesLabel }}
      </p>
    </div>

    <template #footer>
      <!-- `convertible` 은 서버가 판정한 값이다(회차당 1회 · 핵심 규칙 5). 화면이 다시 세지 않는다 -->
      <div
        v-if="settlement?.convertible"
        class="bg-canvas fixed inset-x-0 bottom-0 z-20 mx-auto max-w-(--gp-viewport-w) px-(--gp-gutter) pt-3 pb-[max(12px,env(safe-area-inset-bottom))]"
      >
        <!--
          돈의 3단계 중 ②→③ 경계. 이 화면에서 가장 오해가 잦은 지점이다 (핵심 규칙 2).
          카드가 아니라 CTA 바로 위 인라인 노티스 — 버튼을 누르기 직전에 읽힌다 (2026-09-10 수현)
        -->
        <p
          v-if="!settlement.isCash"
          class="text-caption text-on-confirmed mb-2.5 flex items-start justify-center gap-1.5 text-center"
        >
          <IconWarning :size="15" class="text-confirmed mt-px shrink-0" />
          <span>아직 현금이 아니에요 · 현금으로 바꿔야 그린포켓 계좌로 들어와요</span>
        </p>
        <!-- 「나중에 할래요」 버튼은 뺐다(2026-09-10 수현 · 결정 C-39). 나중에 = 헤더 X 닫기 -->
        <GpButton @click="goConvert">현금으로 바꾸기</GpButton>
      </div>
    </template>
  </AppSubLayout>
</template>
