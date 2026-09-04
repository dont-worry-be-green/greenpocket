<script setup>
/*
 * 그린포켓 · 하단 탭바 (기능명세서 COM-02)
 * 진단 · 혜택 · [Green What-if] · 포켓 · 마이페이지
 * 가운데 What-if 는 FAB 형태이고 서비스의 메인 탭이자 진입 화면을 겸한다.
 *
 * 시안(연동전.png)은 화면 폭을 꽉 채우는 막대가 아니라 좌우 여백을 둔 **떠 있는 pill** 이다.
 * 아트보드에서는 position:absolute 였지만 실제 화면에서는 fixed 라야 스크롤과 무관하게 붙어 있고,
 * 하단 노치는 safe-area-inset 으로 피한다.
 *
 * 이 컴포넌트는 표시만 한다. 어느 경로로 갈지는 AppTabLayout 이 정한다.
 */
defineProps({
  active: { type: String, default: 'whatif' },
  // [{ key, label, icon, fab }] · 순서 고정: 진단 · 혜택 · whatif · 포켓 · 마이페이지
  tabs: { type: Array, required: true },
})
defineEmits(['change'])
</script>

<template>
  <!-- 바깥은 위치만 잡는다. 배경을 여기 두면 pill 바깥까지 흰색이 번진다 -->
  <div
    class="fixed inset-x-0 bottom-0 z-20 mx-auto max-w-(--gp-viewport-w) px-2 pb-[max(8px,env(safe-area-inset-bottom))]"
  >
    <nav class="bg-surface shadow-float flex h-(--gp-tabbar-h) rounded-full" role="tablist">
      <button
        v-for="t in tabs"
        :key="t.key"
        type="button"
        role="tab"
        :aria-selected="active === t.key"
        :aria-label="t.label"
        class="ease-standard relative flex h-full flex-1 cursor-pointer flex-col items-center justify-end gap-1 border-0 bg-transparent pb-2 transition-colors duration-140"
        :class="active === t.key ? 'text-primary' : 'text-icon-off'"
        @click="$emit('change', t.key)"
      >
        <!--
          FAB 은 탭바 위로 20px 솟아 있다(시안 실측). absolute 로 띄우는 이유는
          흐름에 두면 나머지 4개 탭의 라벨 높이까지 같이 밀려서 라벨 줄이 어긋나기 때문이다.
        -->
        <span
          v-if="t.fab"
          class="text-on-primary shadow-float absolute bottom-6 left-1/2 z-30 flex size-(--gp-fab) -translate-x-1/2 items-center justify-center rounded-full bg-(image:--gp-grad-hero)"
        >
          <component :is="t.icon" :size="26" />
        </span>
        <component :is="t.icon" v-else :size="22" />
        <span class="text-nav" :class="{ 'font-bold': active === t.key }">{{ t.label }}</span>
      </button>
    </nav>
  </div>
</template>
