<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import MypageState from '@/components/mypage/MypageState.vue'
import PolicyCard from '@/components/mypage/PolicyCard.vue'
import PolicyPreferenceForm from '@/components/mypage/PolicyPreferenceForm.vue'
import GpButton from '@/components/ui/GpButton.vue'
import GpCard from '@/components/ui/GpCard.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconExternalLink from '@/components/ui/icons/IconExternalLink.vue'
import { useMypageStore } from '@/stores/mypage'
import { usePolicyStore } from '@/stores/policy'
import {
  formatApplicationStatus,
  formatPolicyCategory,
  formatPolicyDate,
  policyMatchMeta,
} from '@/utils/policy'

const route = useRoute()
const router = useRouter()
const store = usePolicyStore()
const mypage = useMypageStore()

const editing = ref(false)
const saved = ref(false)
const currentStatus = ref(null)
const annualIncomeBand = ref(null)
const householdStatus = ref(null)

const bootstrapping = computed(() => !store.detail && !store.detailError)
const match = computed(() => policyMatchMeta(store.detail?.match?.status))
const canPreview = computed(() => Boolean(currentStatus.value && annualIncomeBand.value && householdStatus.value))
const safeUrl = (value) => /^https?:\/\//i.test(value ?? '')

async function load() {
  await store.fetchDetail(route.params.policyId)
}

async function openPreferences() {
  editing.value = true
  saved.value = false
  const data = await mypage.fetchPolicyPreferences()
  if (!data) return
  currentStatus.value = data.currentStatus
  annualIncomeBand.value = data.annualIncomeBand
  householdStatus.value = data.householdStatus
}

function preferencePayload(extra = {}) {
  return {
    currentStatus: currentStatus.value,
    annualIncomeBand: annualIncomeBand.value,
    householdStatus: householdStatus.value,
    ...extra,
  }
}

async function preview() {
  if (!canPreview.value) return
  await store.runPreview(preferencePayload({ page: 0, size: 5 }))
}

async function savePreferences() {
  if (!canPreview.value) return
  const result = await mypage.savePolicyPreferences(preferencePayload())
  if (result) saved.value = true
}

onMounted(load)
</script>

<template>
  <AppSubLayout title="청년정책 상세" back="/mypage/policies">
    <MypageState :loading="bootstrapping" :error="store.detailError" @retry="load">
      <article v-if="store.detail" class="space-y-4 pt-2">
        <GpCard>
          <div class="flex flex-wrap gap-2">
            <GpTag tone="primary">{{ formatPolicyCategory(store.detail.category) }}</GpTag>
            <GpTag v-if="store.detail.subCategory">{{ store.detail.subCategory }}</GpTag>
          </div>
          <h1 class="text-title tracking-display text-ink mt-4 mb-0 break-keep">{{ store.detail.title }}</h1>
          <p class="text-body text-ink-soft mt-3 mb-0 whitespace-pre-line">{{ store.detail.description }}</p>
        </GpCard>

        <GpCard title="나와 맞는 이유">
          <GpTag :tone="match.tone">{{ match.label }}</GpTag>
          <ul class="mt-3 mb-0 space-y-2 pl-5">
            <li v-for="reason in store.detail.match?.reasons ?? []" :key="reason" class="text-body-sm text-ink-soft">{{ reason }}</li>
          </ul>
          <p class="text-caption text-muted mt-3 mb-0">실제 신청 자격은 반드시 공고에서 확인해 주세요.</p>
        </GpCard>

        <GpCard title="지원 내용">
          <p class="text-body text-ink-soft m-0 whitespace-pre-line">{{ store.detail.supportContent }}</p>
        </GpCard>

        <GpCard title="신청 안내">
          <dl class="divide-divider m-0 divide-y">
            <div v-for="row in [
              { label: '상태', value: formatApplicationStatus(store.detail.application?.status) },
              { label: '기간', value: `${formatPolicyDate(store.detail.application?.startDate)} ~ ${formatPolicyDate(store.detail.application?.endDate)}` },
              { label: '방법', value: store.detail.application?.method || '-' },
              { label: '주관 기관', value: store.detail.organizations?.supervising || '-' },
              { label: '운영 기관', value: store.detail.organizations?.operating || '-' },
            ]" :key="row.label" class="py-3 first:pt-0 last:pb-0">
              <dt class="text-caption text-muted mb-1">{{ row.label }}</dt>
              <dd class="text-body-strong text-ink m-0 whitespace-pre-line">{{ row.value }}</dd>
            </div>
          </dl>
        </GpCard>

        <GpCard title="대상 조건">
          <dl class="divide-divider m-0 divide-y">
            <div v-for="row in [
              ['연령', 'age'], ['소득', 'income'], ['취업 상태', 'employment'], ['학력', 'education'],
              ['전공', 'major'], ['혼인', 'marriage'], ['기타', 'special'],
            ]" :key="row[1]" class="flex items-start justify-between gap-3 py-3 first:pt-0 last:pb-0">
              <dt class="text-body text-muted shrink-0">{{ row[0] }}</dt>
              <dd class="text-body-strong text-ink m-0 text-right">{{ store.detail.conditions?.[row[1]] || '-' }}</dd>
            </div>
          </dl>
        </GpCard>

        <a
          v-if="safeUrl(store.detail.application?.url)"
          :href="store.detail.application.url"
          target="_blank"
          rel="noopener noreferrer"
          class="bg-primary text-button text-on-primary flex min-h-12 items-center justify-center gap-2 rounded-md no-underline"
        >
          신청 페이지 열기 <IconExternalLink :size="17" />
        </a>

        <GpButton variant="wide" size="wide" @click="openPreferences">조건을 바꿔 다시 추천하기</GpButton>

        <section v-if="editing" class="bg-surface rounded-lg p-4" aria-labelledby="preview-title">
          <h2 id="preview-title" class="text-section tracking-display text-ink mt-0 mb-1">임시 조건으로 다시 추천</h2>
          <p class="text-caption text-muted mt-0 mb-5">다시 추천만 하면 저장된 내 정보는 바뀌지 않아요.</p>

          <MypageState :loading="mypage.profileLoading" :error="mypage.profileError" @retry="openPreferences">
            <PolicyPreferenceForm
              v-if="mypage.policyPreferences"
              v-model:current-status="currentStatus"
              v-model:annual-income-band="annualIncomeBand"
              v-model:household-status="householdStatus"
            />
            <div class="mt-5 space-y-2">
              <GpButton :disabled="!canPreview || store.previewLoading" @click="preview">
                {{ store.previewLoading ? '다시 찾는 중...' : '이 조건으로 다시 추천' }}
              </GpButton>
              <GpButton variant="ghost" size="wide" class="w-full" :disabled="!canPreview || mypage.saveLoading" @click="savePreferences">
                {{ mypage.saveLoading ? '저장하는 중...' : '내 정보에 저장' }}
              </GpButton>
            </div>
          </MypageState>

          <p v-if="saved" class="bg-positive-bg text-body-sm text-on-positive mt-4 mb-0 rounded-md px-3 py-2">내 추천 조건에 저장했어요.</p>
          <p v-if="store.previewError || mypage.saveError" class="text-body-sm text-negative mt-4 mb-0">{{ (store.previewError || mypage.saveError).message }}</p>

          <div v-if="store.preview" class="border-divider mt-5 border-0 border-t pt-5">
            <h3 class="text-list-title text-ink mt-0 mb-3">새 조건 추천 {{ store.preview.totalElements }}개</h3>
            <div class="space-y-3">
              <PolicyCard v-for="policy in store.preview.content" :key="policy.policyId" :policy="policy" compact @select="router.push(`/mypage/policies/${$event.policyId}`)" />
            </div>
          </div>
        </section>

        <p class="text-caption-sm text-muted m-0 text-center">출처 {{ store.detail.source }} · 정책 정보는 제공 기관 공고가 우선해요.</p>
      </article>
    </MypageState>
  </AppSubLayout>
</template>
