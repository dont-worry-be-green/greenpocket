<script setup>
/*
 * 에코마일리지 등록 주소 — MY-01 (E-1-01 · 결정 C-8)
 *
 * ⚠️ **시안에는 없고 명세에는 있는 행이다.** 그리고 이게 **프로필 주소가 아니다** —
 * 에코마일리지 누리집에 등록된 주소이고, 연동할 때 받아 저장한 값이라 프로필을 바꿔도
 * 따라 바뀌지 않는다(다음 연동 때 갱신). 그래서 위 기본 정보 표와 **같은 카드에 두지 않는다.**
 * 섞어 놓으면 「지역」 두 개가 다른 값으로 나란히 서서 어느 쪽이 내 주소인지 알 수 없다.
 *
 * `matchesProfile: false` 면 이사 안내를 띄운다(B-1-08). 문구(`notice`)는 서버가 문장으로
 * 내려주는 값이라 그대로 쓴다.
 *
 * 미연동이면 `ecoAddress` 가 null 이고, 그때는 뷰가 이 카드를 그리지 않는다 —
 * 없는 주소를 지어내지 않는다(핵심 규칙 8).
 */
import GpCard from '@/components/ui/GpCard.vue'
import GpTag from '@/components/ui/GpTag.vue'
import { formatMonth } from '@/utils/format'

defineProps({
  ecoAddress: { type: Object, required: true },
})
</script>

<template>
  <section>
    <h2 class="text-body-strong text-muted mb-2 px-1">에코마일리지 등록 주소</h2>

    <GpCard>
      <div class="flex items-center justify-between gap-3">
        <p class="text-body-strong text-ink m-0 truncate">{{ ecoAddress.label }}</p>
        <GpTag>{{ formatMonth(ecoAddress.registeredAt) }} 등록</GpTag>
      </div>

      <p class="text-caption text-muted mt-2 mb-0">{{ ecoAddress.notice }}</p>
    </GpCard>
  </section>
</template>
