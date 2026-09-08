<script setup>
import { computed, onMounted } from 'vue'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import KbBankWordmark from '@/components/pocket/KbBankWordmark.vue'
import PocketState from '@/components/pocket/PocketState.vue'
import GpButton from '@/components/ui/GpButton.vue'
import IconInfo from '@/components/ui/icons/IconInfo.vue'
import { usePocketStore } from '@/stores/pocket'
import { formatDate, formatWon } from '@/utils/format'

const store = usePocketStore()
const product = computed(() => store.recommendedProduct)
const productBootstrapping = computed(
  () => !store.recommendedProduct && !store.recommendedProductError,
)

function formatDepositAmount(amount) {
  if (amount % 1_000_000 === 0) return `${amount / 1_000_000}백만원`
  if (amount % 10_000 === 0) return `${amount / 10_000}만원`
  return formatWon(amount)
}

const depositLabel = computed(() => {
  const deposit = product.value?.monthlyDeposit
  if (!deposit) return '-'
  return `월 ${formatDepositAmount(deposit.minimumAmount)} ~ ${formatDepositAmount(deposit.maximumAmount)} (원단위)`
})

const termsLabel = computed(() => {
  const terms = product.value?.contractTermsMonths ?? []
  return terms
    .map((months) => (months % 12 === 0 ? `${months / 12}년` : `${months}개월`))
    .join(' · ')
})

const noticeLabel = computed(() =>
  product.value?.notice?.replace('금리와 우대 조건은', '금리 및 우대 이율은'),
)

onMounted(() => {
  if (!store.recommendedProduct) store.fetchRecommendedProduct()
})

function openApplication() {
  if (product.value?.applicationUrl) {
    window.open(product.value.applicationUrl, '_blank', 'noopener')
  }
}
</script>

<template>
  <AppSubLayout title="상품 상세" back="/pocket" center-title has-footer>
    <PocketState
      :loading="store.recommendedProductLoading || productBootstrapping"
      :error="store.recommendedProductError"
      @retry="store.fetchRecommendedProduct"
    >
      <div v-if="product" class="space-y-4 pt-3">
        <section class="product-hero relative min-h-40 overflow-hidden rounded-xl p-4">
          <h1 class="text-title tracking-display text-ink relative z-10 mt-4 mb-1 max-w-[72%] break-keep">
            {{ product.name }}
          </h1>
          <p class="text-body text-ink-soft relative z-10 m-0 max-w-[68%] break-keep">
            {{ product.tagline }}
          </p>
          <KbBankWordmark class="absolute right-4 bottom-4" />
        </section>

        <section class="bg-surface grid grid-cols-3 rounded-lg px-2 py-4 text-center">
          <div class="border-divider flex min-w-0 flex-col items-center justify-center border-r px-2">
            <span class="text-caption text-muted">상품 유형</span>
            <strong class="kb-accent-text text-body-sm mt-1 break-keep">{{ product.productType }}</strong>
          </div>
          <div class="border-divider flex min-w-0 flex-col items-center justify-center border-r px-2">
            <span class="text-caption text-muted">저축금액</span>
            <strong class="kb-accent-text text-caption mt-1 break-keep">{{ depositLabel }}</strong>
          </div>
          <div class="flex min-w-0 flex-col items-center justify-center px-2">
            <span class="text-caption text-muted">가입 기간</span>
            <strong class="kb-accent-text text-body-sm mt-1 break-keep">{{ termsLabel }}</strong>
          </div>
        </section>

        <section>
          <h2 class="text-section text-ink mt-0 mb-3">우대이율 조건</h2>
          <div class="grid grid-cols-2 gap-2">
            <div
              v-for="(mission, index) in product.preferentialMissions"
              :key="mission"
              class="bg-surface flex min-h-14 items-center gap-2 rounded-md px-3 py-2"
            >
              <span
                class="kb-accent-badge text-caption flex size-7 shrink-0 items-center justify-center rounded-full font-semibold"
                aria-hidden="true"
              >
                {{ index + 1 }}
              </span>
              <strong class="text-body-sm text-ink break-keep">{{ mission }}</strong>
            </div>
          </div>
        </section>

        <aside class="bg-surface flex gap-3 rounded-lg p-4">
          <span
            class="kb-accent-badge flex size-9 shrink-0 items-center justify-center rounded-full"
            aria-hidden="true"
          >
            <IconInfo :size="19" />
          </span>
          <div>
            <p class="text-body-sm text-muted m-0 break-keep">{{ noticeLabel }}</p>
            <p class="text-caption-sm text-muted mt-1 mb-0">
              정보 기준일 {{ formatDate(product.informationBaseDate) }}
            </p>
          </div>
        </aside>
      </div>
    </PocketState>

    <template #footer>
      <div
        v-if="product"
        class="bg-canvas border-divider fixed inset-x-0 bottom-0 z-20 mx-auto max-w-(--gp-viewport-w) border-t px-(--gp-gutter) pt-3 pb-[max(12px,env(safe-area-inset-bottom))]"
      >
        <GpButton
          class="kb-application-button"
          :disabled="!product.applicationUrl"
          @click="openApplication"
        >
          KB 상품 신청 페이지로 이동
        </GpButton>
        <p class="text-caption-sm text-muted mt-1 mb-0 text-center">
          버튼을 누르면 KB국민은행 외부 페이지로 이동합니다.
        </p>
      </div>
    </template>
  </AppSubLayout>
</template>

<style scoped>
.product-hero {
  background: linear-gradient(135deg, var(--gp-amber-50), var(--gp-amber-200));
}

.kb-accent-text {
  color: var(--gp-amber-700);
}

.kb-accent-badge {
  background: var(--gp-amber-100);
  color: var(--gp-amber-700);
}

.kb-application-button {
  background: #f4a900;
  color: #1a1a1a;
}

.kb-application-button:active {
  background: #d99500;
}
</style>
