<script setup>
/*
 * 그린포켓 · 모달
 *
 * 화면 위에 **떠 있는 것**이라 카드와 달리 그림자를 쓴다(`GpCard` 주석의 예외 항목).
 *
 * ── z-index ────────────────────────────────────────────────────────────────
 * 탭바가 `z-20`, 그 위 FAB 이 `z-30` 이라 `z-50` 으로 둘 다 덮는다.
 * `<Teleport to="body">` 로 빼는 이유는 `AppTabLayout` 안에 두면 레이아웃의
 * `max-w`·`overflow` 안에 갇혀 배경이 화면 전체를 덮지 못하기 때문이다.
 *
 * ── 닫기 ───────────────────────────────────────────────────────────────────
 * 백드롭 클릭 · ESC · 닫기 버튼 셋 다 `close` 를 낸다. **여기서 직접 닫지 않는다** —
 * 부모가 열림 상태를 들고 있어야 「서버에 봤다고 알리기」 같은 일을 함께 할 수 있다.
 * 백드롭 판정은 `event.target === event.currentTarget` 으로 한다. `@click` 만 두면
 * 안쪽 카드를 눌러도 이벤트가 올라와 닫힌다.
 *
 * 열려 있는 동안 뒤 화면이 스크롤되지 않게 `body` 를 잠근다.
 */
import { onUnmounted, watch } from 'vue'

import IconClose from './icons/IconClose.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  /** 닫기 버튼·백드롭·ESC 를 막는다. 반드시 골라야 하는 모달에 쓴다 */
  dismissible: { type: Boolean, default: true },
})
const emit = defineEmits(['close'])

function requestClose() {
  if (props.dismissible) emit('close')
}

function onBackdrop(event) {
  if (event.target === event.currentTarget) requestClose()
}

function onKeydown(event) {
  if (event.key === 'Escape') requestClose()
}

function lockScroll(locked) {
  if (typeof document === 'undefined') return
  document.body.style.overflow = locked ? 'hidden' : ''
}

watch(
  () => props.open,
  (open) => {
    lockScroll(open)
    if (open) window.addEventListener('keydown', onKeydown)
    else window.removeEventListener('keydown', onKeydown)
  },
  { immediate: true },
)

// 모달이 열린 채로 화면이 바뀌면 리스너와 스크롤 잠금이 남는다
onUnmounted(() => {
  lockScroll(false)
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 z-50 flex items-end justify-center bg-black/50 px-(--gp-gutter) pb-[max(16px,env(safe-area-inset-bottom))]"
      @click="onBackdrop"
    >
      <div
        class="bg-surface shadow-float w-full max-w-(--gp-viewport-w) rounded-lg p-(--gp-card-pad)"
        role="dialog"
        aria-modal="true"
      >
        <header v-if="title || dismissible" class="mb-3 flex items-start justify-between gap-2">
          <h2 v-if="title" class="text-section tracking-display m-0">{{ title }}</h2>
          <button
            v-if="dismissible"
            type="button"
            class="text-muted -mt-1 -mr-1 cursor-pointer border-0 bg-transparent p-1"
            aria-label="닫기"
            @click="requestClose"
          >
            <IconClose :size="20" />
          </button>
        </header>

        <slot />

        <div v-if="$slots.footer" class="mt-4">
          <slot name="footer" />
        </div>
      </div>
    </div>
  </Teleport>
</template>
