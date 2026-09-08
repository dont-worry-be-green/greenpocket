<script setup>
/*
 * ONB-01 랜딩 (COM-01 · 이슈 #121)
 *
 * **첫 화면이라 뒤로가기가 없다.** `AppSubLayout` 은 `GpBackHeader` 를 항상 그리므로 맞지 않고,
 * 헤더 없는 전체화면 셸은 `components/layout/` 에 없다. 소비자가 이 화면 하나뿐이라
 * 공용 폴더에 셸을 새로 만들지 않고 여기서 직접 쓴다.
 *
 * ── 이름을 여기서 받지 않는다 ───────────────────────────────────────────
 * 예전에는 이 화면이 이름을 받아 `POST /users` 를 불렀다. 서비스에 로그인·회원가입이
 * 들어오면서 이름은 회원가입으로 옮겼고(이슈 #121), 이 화면은 **두 갈래로 갈라 주기만 한다.**
 * 계정 생성과 로그인은 실제 인증 API가 담당한다.
 */
import { useRouter } from 'vue-router'

import GpButton from '@/components/ui/GpButton.vue'
import IconPlant from '@/components/ui/icons/IconPlant.vue'

const router = useRouter()
</script>

<template>
  <div class="bg-canvas flex min-h-dvh flex-col px-(--gp-gutter)">
    <main class="flex flex-1 flex-col items-center justify-center py-10 text-center">
      <span
        class="bg-primary-bg text-primary flex size-24 items-center justify-center rounded-2xl"
        aria-hidden="true"
      >
        <IconPlant :size="48" />
      </span>

      <h1 class="text-title tracking-display text-ink mt-6 mb-0">그린포켓</h1>
      <p class="text-body text-muted mt-3 mb-0">
        전기 고지서 한 장으로<br />
        얼마나 아낄 수 있는지 알려드려요
      </p>
    </main>

    <div class="space-y-2 pt-2 pb-[max(12px,env(safe-area-inset-bottom))]">
      <GpButton @click="router.push('/onboarding/signup')">회원가입</GpButton>

      <GpButton variant="wide" size="wide" class="w-full" @click="router.push('/onboarding/login')">
        이미 계정이 있어요
      </GpButton>
    </div>
  </div>
</template>
