<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

import { formatHousing } from '@/utils/format'

const props = defineProps({
  profile: { type: Object, required: true },
  benefits: {
    type: Array,
    default: () => [
      {
        id: 'YOUTH_MONTHLY_RENT',
        title: '청년월세 특별지원',
        benefit: '월 최대 20만원 · 최대 12개월',
        description: '청년 1인 가구의 주거비 부담을 덜어주는 지원금이에요.',
        schedule: '접수 일정 확인',
      },
      {
        id: 'SEOUL_MOVING_SUPPORT',
        title: '서울시 청년 이사비 지원',
        benefit: '이사비·중개보수 지원',
        description: '서울로 이사한 청년의 이사비와 중개보수 부담을 줄여줘요.',
        schedule: '모집 공고 확인',
      },
      {
        id: 'YOUTH_HOUSING_BENEFIT',
        title: '청년 주거급여 분리지급',
        benefit: '매월 주거비 지원',
        description: '부모와 떨어져 사는 청년의 실제 주거비를 지원하는 제도예요.',
        schedule: '상시 확인',
      },
    ],
  },
})

const activeIndex = ref(0)
const trackRef = ref(null)
let autoplayTimer

const AUTOPLAY_DELAY = 4000

const criteria = computed(() => {
  const region = [props.profile.sidoName?.replace('특별시', ''), props.profile.sigunguName]
    .filter(Boolean)
    .join(' ')
  return `${region} · ${formatHousing(props.profile.housingType, props.profile.areaBand)} 기준`
})

function updateActive(event) {
  const track = event.currentTarget
  const cards = [...track.children]
  if (!cards.length) return

  activeIndex.value = cards.reduce((closest, card, index) => {
    const currentDistance = Math.abs(card.offsetLeft - track.scrollLeft)
    const closestDistance = Math.abs(cards[closest].offsetLeft - track.scrollLeft)
    return currentDistance < closestDistance ? index : closest
  }, 0)
}

function stopAutoplay() {
  window.clearInterval(autoplayTimer)
  autoplayTimer = undefined
}

function startAutoplay() {
  stopAutoplay()

  if (
    props.benefits.length < 2 ||
    window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
  ) {
    return
  }

  autoplayTimer = window.setInterval(() => {
    const track = trackRef.value
    const cards = [...(track?.children ?? [])]
    if (!track || !cards.length) return

    const nextIndex = (activeIndex.value + 1) % cards.length
    track.scrollTo({ left: cards[nextIndex].offsetLeft, behavior: 'smooth' })
    activeIndex.value = nextIndex
  }, AUTOPLAY_DELAY)
}

onMounted(startAutoplay)
onBeforeUnmount(stopAutoplay)
</script>

<template>
  <section class="bg-primary-bg rounded-xl px-4 py-5" aria-labelledby="support-benefits-title">
    <header class="flex items-start justify-between gap-3">
      <div>
        <h2 id="support-benefits-title" class="text-section tracking-display text-ink m-0">
          맞춤 지원 혜택
        </h2>
        <p class="text-body-sm text-primary-on-soft mt-1 mb-0">내 정보로 찾은 지원금이에요.</p>
      </div>
      <span class="text-caption text-primary-on-soft mt-1 shrink-0 font-semibold">
        {{ benefits.length }}개 추천
      </span>
    </header>

    <div
      ref="trackRef"
      class="support-benefit-track mt-4 flex snap-x snap-mandatory gap-3 overflow-x-auto pb-1"
      role="region"
      aria-label="맞춤 지원금 추천"
      aria-roledescription="carousel"
      tabindex="0"
      @scroll.passive="updateActive"
      @pointerenter="stopAutoplay"
      @pointerleave="startAutoplay"
      @focusin="stopAutoplay"
      @focusout="startAutoplay"
      @touchstart.passive="stopAutoplay"
      @touchend.passive="startAutoplay"
    >
      <article
        v-for="(benefit, index) in benefits"
        :key="benefit.id"
        class="bg-surface border-border min-w-[88%] snap-start rounded-lg border p-4"
        role="group"
        :aria-label="`${index + 1} / ${benefits.length}`"
      >
        <h3 class="text-list-title text-ink m-0">{{ benefit.title }}</h3>
        <p class="text-body-sm text-ink-soft mt-2 mb-0">{{ benefit.benefit }}</p>

        <p class="text-caption text-muted mt-3 mb-0 leading-relaxed">{{ benefit.description }}</p>

        <div
          class="border-divider mt-3 flex items-center justify-between gap-3 border-0 border-t pt-3"
        >
          <span class="text-caption-sm text-muted truncate">{{ criteria }}</span>
          <span class="text-caption text-primary-on-soft shrink-0 font-semibold">
            {{ benefit.schedule }}
          </span>
        </div>
      </article>
    </div>

    <div class="mt-3 flex justify-center gap-1.5" aria-label="추천 카드 위치">
      <span
        v-for="(_, index) in benefits"
        :key="index"
        class="size-1.5 rounded-full transition-colors"
        :class="index === activeIndex ? 'bg-primary' : 'bg-disabled-bg'"
        aria-hidden="true"
      />
    </div>
  </section>
</template>

<style scoped>
.support-benefit-track {
  scrollbar-width: none;
  overscroll-behavior-x: contain;
}

.support-benefit-track::-webkit-scrollbar {
  display: none;
}
</style>
