<script setup>
import { onBeforeUnmount, onMounted } from 'vue'

import IconClose from '@/components/ui/icons/IconClose.vue'

defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  back: { type: [String, Object], default: null },
  hasFooter: { type: Boolean, default: false },
})
const emit = defineEmits(['close'])

let previousOverflow = ''

function onKeydown(event) {
  if (event.key === 'Escape') emit('close')
}

onMounted(() => {
  previousOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.body.style.overflow = previousOverflow
  document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <Teleport to="body">
    <div
      class="report-overlay bg-overlay fixed inset-0 z-50 flex items-center justify-center p-4"
      @click.self="emit('close')"
    >
      <section
        role="dialog"
        aria-modal="true"
        :aria-label="title"
        class="report-dialog bg-canvas shadow-float flex max-h-[calc(100dvh-2rem)] w-full max-w-(--gp-viewport-w) flex-col overflow-hidden rounded-lg"
      >
        <header
          class="bg-canvas border-divider relative flex shrink-0 flex-col items-center justify-center border-b px-14 py-3"
          :class="subtitle ? 'min-h-20' : 'min-h-14'"
        >
          <h1 class="text-section text-ink m-0 truncate text-center">{{ title }}</h1>
          <p v-if="subtitle" class="text-caption text-muted mt-1 mb-0 text-center">
            {{ subtitle }}
          </p>
          <button
            type="button"
            aria-label="리포트 닫기"
            class="text-muted absolute top-2 right-1 flex size-(--gp-min-touch) cursor-pointer items-center justify-center rounded-full border-0 bg-transparent"
            @click="emit('close')"
          >
            <IconClose :size="20" />
          </button>
        </header>

        <main class="overflow-y-auto px-(--gp-gutter) pb-8">
          <slot />
        </main>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.report-overlay {
  animation: report-overlay-in 180ms ease-out both;
}

.report-dialog {
  transform-origin: center;
  animation: report-dialog-in 260ms cubic-bezier(0.2, 0.8, 0.2, 1) both;
}

@keyframes report-overlay-in {
  from {
    opacity: 0;
  }
}

@keyframes report-dialog-in {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.96);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (prefers-reduced-motion: reduce) {
  .report-overlay,
  .report-dialog {
    animation: none;
  }
}
</style>
