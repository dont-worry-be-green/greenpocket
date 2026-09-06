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
 * ── 어디에 뜨나 ────────────────────────────────────────────────────────────
 * 기본은 화면 아래에 붙는 **시트**다. 긴 목록(시·군·구 선택 · 데모 도구)은 엄지에 가깝고
 * 스크롤이 자연스러워야 해서 그렇다.
 *
 * `align="center"` 를 주면 화면 **가운데 팝업**이 된다. 목록이 아니라 결과를 알리고 답을
 * 받아 가는 모달(WF-09 결산 알림)이 그렇다 — 아래에 붙으면 알림이 아니라 서랍처럼 읽힌다.
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
  /** 'bottom'(기본, 시트) | 'center'(가운데 팝업). 위 주석 참고 */
  align: { type: String, default: 'bottom' },
  /** 닫기 버튼·백드롭·ESC 를 막는다. 반드시 골라야 하는 모달에 쓴다 */
  dismissible: { type: Boolean, default: true },
})
const emit = defineEmits(['close'])

/*
 * 가운데 팝업은 좌우 여백을 조금 더 준다 — 시트는 화면에 붙지만 팝업은 떠 있어야 해서,
 * 같은 여백을 쓰면 가장자리에 닿아 붙어 있는 것처럼 보인다.
 */
const ALIGN = {
  center: 'items-center px-5 py-6',
  bottom: 'items-end px-(--gp-gutter) pb-[max(16px,env(safe-area-inset-bottom))]',
}
const alignClass = () => ALIGN[props.align] ?? ALIGN.bottom

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
      class="fixed inset-0 z-50 flex justify-center bg-black/50"
      :class="alignClass()"
      @click="onBackdrop"
    >
      <!-- 내용이 길어도 화면을 넘기지 않는다. 짧은 모달에서는 아무 차이가 없다 -->
      <div
        class="bg-surface shadow-float max-h-[85dvh] w-full max-w-(--gp-viewport-w) overflow-y-auto rounded-lg p-(--gp-card-pad)"
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
