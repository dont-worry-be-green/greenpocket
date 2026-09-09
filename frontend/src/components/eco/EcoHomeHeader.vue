<script setup>
/*
 * WF-06 홈 헤더 (B-4-01) — 그린포켓 로고(PNG)·이름 줄, 헤드라인, 카드 뒤에서 빼꼼한 캐릭터.
 *
 * 홈에만 있는 헤더다. 다른 네 탭은 `GpPageHeader` + 흰 카드로 시작한다(design-system.md 9-1).
 * 하늘·언덕 배경과 「다 지키면 이번 달 ~원」 보조문은 뺐다 — 헤더가 길어지고 너무 귀여워졌다.
 *
 * 헤드라인은 이 화면에서 가장 먼저 읽히는 문장이라 카드 제목(18.5)보다 두 단계 위(24)다.
 * 남은 개수는 서버가 준 `completedCount · totalCount` 로만 만든다 — 목록을 세지 않는다.
 *
 * 캐릭터는 첫 카드 뒤(z-0)에 서고 카드 윗선이 두 주먹 바로 아래를 자른다.
 * 그래서 이 헤더 다음에 오는 첫 카드는 `relative z-[1]` 이어야 한다.
 * 표정은 페이스(ecoPace.js)를 따른다 — 상태색과 같은 축이라 둘이 어긋나지 않는다.
 */
import { computed } from 'vue'

import gpLogo from '@/assets/그린포켓 로고 .png'
import characterBehind from '@/assets/character/sad.png'
import characterNear from '@/assets/character/push.png'
import characterOn from '@/assets/character/ok.png'
import characterAhead from '@/assets/character/best.png'

const props = defineProps({
  name: { type: String, default: '' },
  /** GET /eco/home 의 `todayMissions` 또는 오늘의 실천 응답. `{ completedCount, totalCount }` */
  todayMissions: { type: Object, default: null },
  pace: { type: String, default: 'near' },
})

const CHARACTER = {
  behind: characterBehind,
  near: characterNear,
  on: characterOn,
  ahead: characterAhead,
}
const character = computed(() => CHARACTER[props.pace] ?? CHARACTER.near)

const headline = computed(() => {
  const total = props.todayMissions?.totalCount ?? 0
  const completed = props.todayMissions?.completedCount ?? 0
  if (total === 0) return ['오늘 할 미션이', '아직 없어요']
  const remaining = total - completed
  if (remaining <= 0) return ['오늘 미션을', '다 지켰어요']
  return ['오늘 미션', `${remaining}개가 남았어요`]
})
</script>

<template>
  <header class="relative pt-3.5 pb-3.5">
    <div class="flex items-center gap-2">
      <img :src="gpLogo" alt="" aria-hidden="true" class="size-10 shrink-0 object-contain select-none" />
      <b class="text-list-title text-ink tracking-title">{{ name ? `${name}님` : '안녕하세요' }}</b>
    </div>

    <p class="text-title tracking-title text-ink mt-[30px] mb-0 max-w-[214px]">
      {{ headline[0] }}<br />{{ headline[1] }}
    </p>

    <img
      :src="character"
      alt=""
      aria-hidden="true"
      class="pointer-events-none absolute right-0 -bottom-[30px] z-0 size-[126px] drop-shadow-[0_3px_7px_rgb(20_50_36/0.12)] select-none"
    />
  </header>
</template>
