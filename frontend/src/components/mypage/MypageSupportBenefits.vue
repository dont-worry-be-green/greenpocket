<script setup>
import PolicyCard from '@/components/mypage/PolicyCard.vue'
import GpButton from '@/components/ui/GpButton.vue'

defineProps({ youthPolicy: { type: Object, required: true } })
defineEmits(['configure', 'all', 'select', 'linkEco'])
</script>

<template>
  <section aria-labelledby="youth-policy-title">
    <header class="mb-3 flex items-end justify-between gap-3 px-1">
      <div>
        <h2 id="youth-policy-title" class="text-section tracking-display text-ink m-0">
          나를 위한 청년정책
        </h2>
        <p class="text-caption text-muted mt-1 mb-0">내 조건에 가까운 정책부터 보여드려요.</p>
      </div>
      <button
        v-if="youthPolicy.profileCompleted"
        type="button"
        class="text-label text-primary-on-soft min-h-11 shrink-0 cursor-pointer border-0 bg-transparent px-1 font-semibold"
        @click="$emit('configure')"
      >
        조건 수정
      </button>
    </header>

    <div v-if="!youthPolicy.profileCompleted" class="bg-primary-bg rounded-xl p-5">
      <p class="text-body-strong text-ink mt-0 mb-1">맞춤 추천 조건을 알려주세요</p>
      <p class="text-body-sm text-primary-on-soft mt-0 mb-4">
        현재 상태, 연소득, 학력과 관심 분야를 선택하면 돼요.
      </p>
      <GpButton variant="wide" size="wide" @click="$emit('configure')">추천 조건 설정하기</GpButton>
    </div>

    <template v-else>
      <div v-if="!youthPolicy.regionLinked" class="bg-confirmed-bg rounded-md px-4 py-3">
        <p class="text-body-sm text-on-confirmed mt-0 mb-2">
          지금은 전국 정책만 추천해요. 에코마일리지를 연동하면 내 지역 정책도 볼 수 있어요.
        </p>
        <button
          type="button"
          class="text-label text-on-confirmed min-h-11 cursor-pointer border-0 bg-transparent p-0 font-semibold underline"
          @click="$emit('linkEco')"
        >
          에코마일리지 연동하기
        </button>
      </div>

      <div
        v-if="youthPolicy.preview?.length"
        class="space-y-3"
        :class="!youthPolicy.regionLinked ? 'mt-3' : ''"
      >
        <PolicyCard
          v-for="policy in youthPolicy.preview"
          :key="policy.policyId"
          :policy="policy"
          compact
          @select="$emit('select', $event)"
        />
      </div>

      <div v-else class="bg-surface rounded-lg px-5 py-8 text-center">
        <p class="text-body text-muted m-0">현재 조건에 맞는 추천 정책이 없어요.</p>
      </div>

      <GpButton class="mt-3" variant="wide" size="wide" @click="$emit('all')">
        전체 청년정책 보기
      </GpButton>
    </template>
  </section>
</template>
