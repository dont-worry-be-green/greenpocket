<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import BillManualForm from '@/components/analysis/BillManualForm.vue'
import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import IconCamera from '@/components/ui/icons/IconCamera.vue'
import IconPencil from '@/components/ui/icons/IconPencil.vue'
import IconScan from '@/components/ui/icons/IconScan.vue'
import { useAnalysisStore } from '@/stores/analysis'
import { formatMonthOnly } from '@/utils/format'

const MAX_FILE_SIZE = 10 * 1024 * 1024
const ACCEPTED_TYPES = ['image/jpeg', 'image/png']

const route = useRoute()
const router = useRouter()
const store = useAnalysisStore()
const cameraInput = ref(null)
const albumInput = ref(null)
const fileError = ref('')
const inputMode = ref(route.query.mode === 'manual' ? 'manual' : 'photo')

const targetYearMonth = computed(
  () => route.query.month ?? store.targetMonth?.targetYearMonth ?? '2026-08',
)
const monthLabel = computed(() => formatMonthOnly(targetYearMonth.value))
const initialManualDraft = computed(() =>
  route.query.prefill === 'recognition' ? store.billDraft : null,
)

function openCamera() {
  cameraInput.value?.click()
}

function openAlbum() {
  albumInput.value?.click()
}

function onFileSelected(event) {
  const file = event.target.files?.[0]
  if (!file) return

  if (!ACCEPTED_TYPES.includes(file.type)) {
    fileError.value = 'JPG 또는 PNG 이미지 한 장을 선택해 주세요.'
    event.target.value = ''
    return
  }
  if (file.size > MAX_FILE_SIZE) {
    fileError.value = '10MB 이하 이미지만 등록할 수 있어요.'
    event.target.value = ''
    return
  }

  fileError.value = ''
  store.selectImage(file)
  router.push({ path: '/analysis/bills/analyzing', query: { month: targetYearMonth.value } })
}

function completeManualEntry() {
  router.push('/analysis/bills/confirm')
}
</script>

<template>
  <AppSubLayout back="/analysis" :has-footer="inputMode === 'manual'">
    <header class="mb-5">
      <h1 class="text-title tracking-title text-ink mt-1 mb-1">
        {{ monthLabel }} 고지서를 등록해요
      </h1>
      <p class="text-caption text-muted m-0">관리비·전기·수도·도시가스 고지서를 인식해요.</p>
    </header>

    <div class="bg-primary-bg mb-5 grid grid-cols-2 rounded-xl p-1">
      <button
        type="button"
        class="flex min-h-11 items-center justify-center gap-2 rounded-lg border-0 text-label"
        :class="inputMode === 'photo' ? 'bg-surface text-primary shadow-float' : 'text-muted bg-transparent'"
        @click="inputMode = 'photo'"
      >
        <IconCamera :size="19" />
        사진 촬영
      </button>
      <button
        type="button"
        class="flex min-h-11 items-center justify-center gap-2 rounded-lg border-0 text-label"
        :class="inputMode === 'manual' ? 'bg-surface text-ink shadow-float' : 'text-muted bg-transparent'"
        @click="inputMode = 'manual'"
      >
        <IconPencil :size="19" />
        직접 입력
      </button>
    </div>

    <template v-if="inputMode === 'photo'">
    <section
      class="border-primary-soft bg-surface relative flex min-h-72 flex-col items-center justify-center overflow-hidden rounded-lg border border-dashed px-6 text-center"
    >
      <span class="border-primary-bg absolute -top-10 -right-10 size-24 rounded-full border" />
      <span class="border-primary-bg absolute -bottom-12 -left-12 size-28 rounded-full border" />
      <span
        class="bg-primary-bg text-primary mb-6 flex size-20 items-center justify-center rounded-lg"
        aria-hidden="true"
      >
        <span class="scan-icon">
          <IconScan :size="28" />
        </span>
      </span>
      <h2 class="text-section text-ink mt-0 mb-2">고지서 전체가 보이게 찍어주세요</h2>
    </section>

    <div class="mt-4 grid grid-cols-2 gap-3">
      <GpButton @click="openCamera">사진 촬영</GpButton>
      <button
        type="button"
        class="border-control-border bg-surface text-ink h-(--gp-cta-h) rounded-md border text-button"
        @click="openAlbum"
      >
        앨범에서 고르기
      </button>
    </div>
    <p v-if="fileError" class="text-caption text-negative mt-2 mb-0" role="alert">
      {{ fileError }}
    </p>

    <input
      ref="cameraInput"
      class="hidden"
      type="file"
      accept="image/jpeg,image/png"
      capture="environment"
      @change="onFileSelected"
    />
    <input
      ref="albumInput"
      class="hidden"
      type="file"
      accept="image/jpeg,image/png"
      @change="onFileSelected"
    />
    </template>

    <BillManualForm
      v-else
      :billing-month="targetYearMonth"
      :initial-draft="initialManualDraft"
      @complete="completeManualEntry"
    />
  </AppSubLayout>
</template>

<style scoped>
.scan-icon {
  display: inline-flex;
  animation: scan-icon-zoom 1.4s ease-in-out infinite;
}

@keyframes scan-icon-zoom {
  0%,
  100% {
    transform: scale(0.88);
    opacity: 0.72;
  }

  50% {
    transform: scale(1.14);
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .scan-icon {
    animation: none;
  }
}
</style>
