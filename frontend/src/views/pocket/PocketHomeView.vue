<script setup>
/*
 * 포켓 홈 PK-01 (D-1-04 · D-1-06 · D-4-01 · 결정 C-31)
 *
 * 잔액 카드는 브랜드 초록 면(「그린포켓 잔액 ⓘ」 · 잔액 + › → 거래 내역) 위에 흰 인셋 행 2개
 * (「전환 가능한 마일리지 · 30,000M ›」 → 전환, 「출금 신청 · 은행 로고 국민 ›」 → 출금)를 얹는다.
 * 그 아래 「추천 상품」 카드(D-4-01) · 「최근 내역」 카드 순이다.
 *
 * 홈에 두지 않는 것 — 출금계좌 번호 전체(포켓 관리 PK-06), 잔액 내역(에코/녹색생활),
 * 「마일리지 전환은 1일 1회…」 안내(전환 PK-03). 빈 상태(D-1-06)는 같은 카드에서 값만 비운다.
 * 초록 solid 면은 화면에 이 카드 하나다.
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppTabLayout from '@/components/layout/AppTabLayout.vue'
import ecoMark from '@/assets/eco-mileage-mark.png'
import BankLogo from '@/components/pocket/BankLogo.vue'
import { bankShortName } from '@/components/pocket/bankMeta'
import PocketRecommendedProductCard from '@/components/pocket/PocketRecommendedProductCard.vue'
import PocketState from '@/components/pocket/PocketState.vue'
import GpButton from '@/components/ui/GpButton.vue'
import GpCard from '@/components/ui/GpCard.vue'
import GpModal from '@/components/ui/GpModal.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconCaretRight from '@/components/ui/icons/IconCaretRight.vue'
import IconCoins from '@/components/ui/icons/IconCoins.vue'
import IconLeaf from '@/components/ui/icons/IconLeaf.vue'
import IconPocket from '@/components/ui/icons/IconPocket.vue'
import { usePocketStore } from '@/stores/pocket'
import { formatDateTime, formatMileage, formatSignedWon, formatWon } from '@/utils/format'

const router = useRouter()
const store = usePocketStore()
const actionMessage = ref('')
const isConversionNoticeOpen = ref(false)
const recommendedProductBootstrapping = computed(
  () => !store.recommendedProduct && !store.recommendedProductError,
)
const pocket = computed(() => store.home ?? { balance: 0, convertibleMileage: 0 })
/*
 * 최근 내역 4건 (D-1-04 · 결정 C-16). 홈 응답의 `recentTransactions` 가 정본이고,
 * 없으면 내역 조회의 그룹을 펴서 앞 4건을 쓴다.
 */
const recentTransactions = computed(() => {
  const fromHome = store.home?.recentTransactions
  if (Array.isArray(fromHome)) return fromHome.slice(0, 4)
  const groups = store.transactions?.groups ?? []
  return groups.flatMap((g) => g.items).slice(0, 4)
})

/** 기본 출금계좌. 없으면(D-1-06 `empty.noAccount`) 출금 행이 계좌 등록으로 간다 */
const defaultAccount = computed(() => store.home?.defaultAccount ?? null)
const hasAccount = computed(() => Boolean(defaultAccount.value) && !store.home?.empty?.noAccount)
const canConvert = computed(
  () =>
    Number(pocket.value.convertibleMileage) > 0 && Boolean(pocket.value.convertibleSource?.roundId),
)

function goWithdraw() {
  router.push(hasAccount.value ? '/pocket/withdraw' : '/pocket/accounts/new')
}
const withdrawalMap = computed(() => {
  const map = {}
  for (const w of store.withdrawals?.content ?? []) map[w.transactionId] = w
  return map
})

onMounted(() => {
  store.fetchHome()
  store.fetchTransactions()
  store.fetchWithdrawals()
  store.fetchRecommendedProduct()
  window.addEventListener('focus', completeConversionOnReturn)
})
onBeforeUnmount(() => window.removeEventListener('focus', completeConversionOnReturn))

function openConversionNotice() {
  if (!pocket.value.convertibleSource?.roundId) return
  isConversionNoticeOpen.value = true
}

async function convertMileage() {
  const roundId = pocket.value.convertibleSource?.roundId
  if (!roundId) return
  isConversionNoticeOpen.value = false
  const externalWindow = window.open('about:blank', '_blank')
  if (externalWindow) externalWindow.opener = null
  const started = await store.startConversion(roundId)
  if (started?.externalUrl) {
    if (externalWindow) externalWindow.location.replace(started.externalUrl)
    else window.location.assign(started.externalUrl)
    actionMessage.value = '누리집에서 전환한 뒤 돌아오면 포켓에 반영돼요.'
  } else {
    externalWindow?.close()
    actionMessage.value = store.conversionError?.message
  }
  window.setTimeout(() => (actionMessage.value = ''), 2200)
}

const withdrawalStatusLabels = {
  REQUESTED: '출금 요청',
  PROCESSING: '처리 중',
  COMPLETED: '출금',
  FAILED: '출금 실패',
  CANCELED: '출금 취소',
}

function transactionStatusLabel(item) {
  if (item.direction === 'DEBIT')
    return withdrawalStatusLabels[item.transactionStatus] ?? item.transactionStatus
  if (item.transactionStatus !== 'COMPLETED') return item.transactionStatus
  return item.transactionType === 'GREENLIFE' ? '지급 완료' : '입금'
}

async function completeConversionOnReturn() {
  if (!store.pendingConversion || store.conversionLoading) return
  const result = await store.completeConversion()
  actionMessage.value = result
    ? `${formatWon(result.amount)} 전환이 완료됐어요.`
    : store.conversionError?.message
  window.setTimeout(() => (actionMessage.value = ''), 2200)
}
</script>

<template>
  <AppTabLayout tab="pocket" title="포켓">
    <template #headerAction>
      <button
        type="button"
        class="bg-surface shadow-card text-caption text-ink-soft flex min-h-9 cursor-pointer items-center gap-0.5 rounded-full border-0 py-2 pr-2.5 pl-3 font-bold"
        @click="router.push('/pocket/management')"
      >
        포켓 관리
        <IconCaretRight :size="13" class="text-icon-off" />
      </button>
    </template>

    <PocketState :loading="store.isLoading" :error="store.error" @retry="store.fetchHome">
      <div class="flex flex-col gap-(--gp-card-gap)">
        <!-- 잔액 카드. 초록 면 + 흰 인셋 행 2개 (결정 C-31) -->
        <section
          class="rounded-card text-on-primary bg-[linear-gradient(172deg,var(--gp-green-600)_0%,var(--color-primary)_42%,var(--color-primary-pressed)_100%)] p-2.5 pt-[18px] pl-5 shadow-[0_8px_20px_-6px_rgb(5_116_65/0.45)]"
        >
          <div class="pb-3">
            <p class="text-label m-0 font-bold">그린포켓 잔액</p>
            <button
              type="button"
              class="text-on-primary mt-1.5 inline-flex cursor-pointer items-center gap-0.5 border-0 bg-transparent p-0 text-left"
              aria-label="거래 내역 전체 보기"
              @click="router.push('/pocket/transactions')"
            >
              <span class="inline-flex items-baseline gap-1">
                <span class="text-display tracking-display tabular-nums">{{
                  formatWon(pocket.balance).replace('원', '')
                }}</span>
                <span class="text-section font-extrabold">원</span>
              </span>
              <IconCaretRight :size="20" class="opacity-85" />
            </button>
            <p
              v-if="store.home?.empty?.noTransaction"
              class="text-caption m-0 mt-1.5 font-semibold"
            >
              아직 입금이 없어요
            </p>
          </div>

          <div class="bg-surface text-ink -ml-2.5 rounded-[18px] px-3.5 py-0.5">
            <button
              type="button"
              class="border-divider flex min-h-[52px] w-full cursor-pointer items-center gap-2 border-0 bg-transparent py-2 text-left disabled:cursor-default"
              :disabled="!canConvert || store.conversionLoading"
              @click="openConversionNotice"
            >
              <span class="text-body-strong text-ink-soft flex-1">전환 가능한 마일리지</span>
              <span class="inline-flex items-center gap-1.5 whitespace-nowrap">
                <template v-if="canConvert">
                  <img :src="ecoMark" alt="에코마일리지" class="h-3.5 w-auto" />
                  <span class="text-list-title text-ink tabular-nums">
                    {{ formatMileage(pocket.convertibleMileage).replace('M', '')
                    }}<span class="text-caption-sm text-ink-soft font-bold">M</span>
                  </span>
                </template>
                <span v-else class="text-label text-muted">아직 없어요</span>
              </span>
              <IconCaretRight :size="16" class="text-icon-off shrink-0" />
            </button>

            <button
              type="button"
              class="border-divider flex min-h-[52px] w-full cursor-pointer items-center gap-2 border-0 border-t bg-transparent py-2 text-left"
              @click="goWithdraw"
            >
              <span class="text-body-strong text-ink-soft flex-1">출금 신청</span>
              <span class="inline-flex items-center gap-1.5 whitespace-nowrap">
                <template v-if="hasAccount">
                  <BankLogo
                    :bank-code="defaultAccount.bankCode"
                    :bank-name="defaultAccount.bankName"
                    :size="22"
                  />
                  <span class="text-label text-ink font-bold">
                    {{ bankShortName(defaultAccount.bankCode, defaultAccount.bankName) }}
                  </span>
                </template>
                <span v-else class="text-label text-muted">출금계좌 등록</span>
              </span>
              <IconCaretRight :size="16" class="text-icon-off shrink-0" />
            </button>
          </div>
        </section>

        <PocketRecommendedProductCard
          :product="store.recommendedProduct"
          :loading="store.recommendedProductLoading || recommendedProductBootstrapping"
          :error="store.recommendedProductError"
          @view="router.push('/pocket/recommended-product')"
          @retry="store.fetchRecommendedProduct"
        />

        <GpCard title="최근 내역">
          <template v-if="recentTransactions.length" #action>
            <button
              type="button"
              class="text-caption text-muted flex cursor-pointer items-center gap-0.5 border-0 bg-transparent p-0 font-bold"
              @click="router.push('/pocket/transactions')"
            >
              전체 보기
              <IconCaretRight :size="12" />
            </button>
          </template>

          <ul
            v-if="recentTransactions.length"
            class="divide-divider -mt-1 m-0 list-none divide-y p-0"
          >
            <li
              v-for="item in recentTransactions"
              :key="item.transactionId"
              class="flex min-h-[62px] items-center gap-3 py-2.5"
            >
              <BankLogo
                v-if="item.direction === 'DEBIT'"
                :bank-code="
                  withdrawalMap[item.transactionId]?.accountSnapshot?.bankCode ??
                  defaultAccount?.bankCode
                "
                :bank-name="
                  withdrawalMap[item.transactionId]?.accountSnapshot?.bankName ??
                  defaultAccount?.bankName
                "
                :size="40"
              />
              <span
                v-else
                class="flex size-10 shrink-0 items-center justify-center rounded-md"
                :class="
                  item.transactionType === 'ECO_MILEAGE'
                    ? 'bg-confirmed-bg'
                    : 'bg-primary-bg text-primary-soft'
                "
                aria-hidden="true"
              >
                <img
                  v-if="item.transactionType === 'ECO_MILEAGE'"
                  :src="ecoMark"
                  alt=""
                  class="h-4 w-auto"
                />
                <IconLeaf v-else :size="20" />
              </span>
              <div class="min-w-0 flex-1">
                <p class="text-body-strong text-ink m-0 truncate">
                  <template
                    v-if="
                      item.direction === 'DEBIT' &&
                      withdrawalMap[item.transactionId]?.accountSnapshot
                    "
                  >
                    {{ withdrawalMap[item.transactionId].accountSnapshot.bankName }}
                    {{ withdrawalMap[item.transactionId].accountSnapshot.accountNo }}
                  </template>
                  <template v-else>{{ item.label }}</template>
                </p>
                <p class="text-caption-sm text-muted m-0">{{ formatDateTime(item.completedAt) }}</p>
              </div>
              <div class="flex shrink-0 flex-col items-end gap-1">
                <p
                  class="text-body-strong m-0 tabular-nums"
                  :class="item.direction === 'CREDIT' ? 'text-primary-on-soft' : 'text-ink'"
                >
                  {{ formatSignedWon(item.direction === 'CREDIT' ? item.amount : -item.amount) }}
                </p>
                <GpTag
                  v-if="item.transactionStatus"
                  :tone="item.direction === 'DEBIT' ? 'sub' : 'primary'"
                  small
                >
                  {{ transactionStatusLabel(item) }}
                </GpTag>
              </div>
            </li>
          </ul>

          <!-- D-1-06 빈 상태. 명세 문구 그대로 -->
          <div v-else class="flex flex-col items-center gap-1.5 py-4 text-center">
            <span
              class="bg-primary-bg text-primary-soft mb-1.5 flex size-11 items-center justify-center rounded-full"
              aria-hidden="true"
            >
              <IconPocket :size="22" />
            </span>
            <p class="text-body-strong text-ink m-0">아직 적립 내역이 없어요</p>
            <p class="text-caption text-muted m-0">
              다양한 친환경 활동을 실천하고<br />그린포켓을 채워보세요!
            </p>
          </div>
        </GpCard>
      </div>
    </PocketState>

    <GpModal :open="isConversionNoticeOpen" align="center" @close="isConversionNoticeOpen = false">
      <div class="flex flex-col items-center px-2 pt-1 text-center">
        <span
          class="bg-gas-bg text-gas mb-5 flex size-16 items-center justify-center rounded-full"
          aria-hidden="true"
        >
          <IconCoins :size="32" />
        </span>
        <h2 class="text-section tracking-display mt-0 mb-5">마일리지를 전환할까요?</h2>
        <p class="text-body text-ink-soft m-0 break-keep">
          {{ formatMileage(pocket.convertibleMileage) }}을 서울시 에코마일리지에서<br />
          현금으로 전환할 수 있어요.
        </p>
      </div>

      <template #footer>
        <div class="mt-4 grid grid-cols-[0.85fr_1.15fr] gap-3">
          <button
            type="button"
            class="bg-disabled-bg text-ink-soft h-(--gp-cta-h) rounded-md border-0 text-button"
            @click="isConversionNoticeOpen = false"
          >
            취소
          </button>
          <GpButton :disabled="store.conversionLoading" @click="convertMileage">
            에코마일리지로 이동
          </GpButton>
        </div>
      </template>
    </GpModal>

    <div
      v-if="actionMessage"
      class="bg-ink text-on-primary shadow-float fixed bottom-24 left-1/2 z-70 w-max max-w-[calc(100%-32px)] -translate-x-1/2 rounded-full px-4 py-3 text-caption"
    >
      {{ actionMessage }}
    </div>
  </AppTabLayout>
</template>
