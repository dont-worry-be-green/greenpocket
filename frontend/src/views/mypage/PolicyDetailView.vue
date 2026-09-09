<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import MypageState from '@/components/mypage/MypageState.vue'
import GpCard from '@/components/ui/GpCard.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconExternalLink from '@/components/ui/icons/IconExternalLink.vue'
import { usePolicyStore } from '@/stores/policy'
import {
  formatApplicationStatus,
  formatPolicyCategory,
  formatPolicyPeriod,
  policyMatchMeta,
} from '@/utils/policy'

const route = useRoute()
const store = usePolicyStore()

const bootstrapping = computed(() => !store.detail && !store.detailError)
const match = computed(() => policyMatchMeta(store.detail?.match?.status))
const safeUrl = (value) => /^https?:\/\//i.test(value ?? '')

async function load() {
  await store.fetchDetail(route.params.policyId)
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
          <h1 class="text-title tracking-display text-ink mt-4 mb-0 break-keep">
            {{ store.detail.title }}
          </h1>
          <p class="text-body text-ink-soft mt-3 mb-0 whitespace-pre-line">
            {{ store.detail.description }}
          </p>
        </GpCard>

        <GpCard title="나와 맞는 이유">
          <GpTag :tone="match.tone">{{ match.label }}</GpTag>
          <ul class="mt-3 mb-0 space-y-2 pl-5">
            <li
              v-for="reason in store.detail.match?.reasons ?? []"
              :key="reason"
              class="text-body-sm text-ink-soft"
            >
              {{ reason }}
            </li>
          </ul>
          <p class="text-caption text-muted mt-3 mb-0">
            실제 신청 자격은 반드시 공고에서 확인해 주세요.
          </p>
        </GpCard>

        <GpCard title="지원 내용">
          <p class="text-body text-ink-soft m-0 whitespace-pre-line">
            {{ store.detail.supportContent }}
          </p>
        </GpCard>

        <GpCard title="신청 안내">
          <dl class="divide-divider m-0 divide-y">
            <div
              v-for="row in [
                { label: '상태', value: formatApplicationStatus(store.detail.application?.status) },
                {
                  label: '기간',
                  value: formatPolicyPeriod(store.detail.application),
                },
                { label: '방법', value: store.detail.application?.method || '-' },
                { label: '주관 기관', value: store.detail.organizations?.supervising || '-' },
                { label: '운영 기관', value: store.detail.organizations?.operating || '-' },
              ]"
              :key="row.label"
              class="py-3 first:pt-0 last:pb-0"
            >
              <dt class="text-caption text-muted mb-1">{{ row.label }}</dt>
              <dd class="text-body-strong text-ink m-0 whitespace-pre-line">{{ row.value }}</dd>
            </div>
          </dl>
        </GpCard>

        <GpCard title="대상 조건">
          <dl class="divide-divider m-0 divide-y">
            <div
              v-for="row in [
                ['연령', 'age'],
                ['소득', 'income'],
                ['취업 상태', 'employment'],
                ['학력', 'education'],
                ['전공', 'major'],
                ['혼인', 'marriage'],
                ['기타', 'special'],
              ]"
              :key="row[1]"
              class="flex items-start justify-between gap-3 py-3 first:pt-0 last:pb-0"
            >
              <dt class="text-body text-muted shrink-0">{{ row[0] }}</dt>
              <dd class="text-body-strong text-ink m-0 text-right">
                {{ store.detail.conditions?.[row[1]] || '-' }}
              </dd>
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

        <p class="text-caption-sm text-muted m-0 text-center">
          출처 {{ store.detail.source }} · 정책 정보는 제공 기관 공고가 우선해요.
        </p>
      </article>
    </MypageState>
  </AppSubLayout>
</template>
