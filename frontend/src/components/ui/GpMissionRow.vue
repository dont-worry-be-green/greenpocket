<script setup>
/*
 * 그린포켓 · 실천 미션 행 (WF-04)
 * 절감률은 모두 추정치다. 출처와 산출 근거를 행 안에 같이 보여준다 —
 * "이 숫자 어디서 나왔어요?"에 화면 안에서 답이 나와야 한다 (기능명세서 B-3-01).
 *
 * mission 은 `GET /eco/rounds/{roundId}/goal-form` 의 segments[].missions[] 원형 그대로다.
 *   { missionId, missionCode, title, description, difficulty,
 *     evidenceAmount, evidenceUnit, evidenceText, calculationBasis, sourceOrg,
 *     deviceGroup, seasonTags, computedRate, capped, selected }
 *
 * 계절 한정 미션(`seasonTags` 가 사계절이 아닌 것)에는 「여름 전용」 칩을 단다 — 오늘의 실천은
 * 현재 계절 태그로 거르므로(B-3-05) 9월에 고른 냉방 미션은 홈에 안 보인다. 고를 때 알려야 한다.
 *
 * 합계 제외 여부는 미션 자체가 아니라 목표 미리보기(`POST .../goal/preview`)가 판단한다.
 * 같은 deviceGroup 중 computedRate 최대값만 counted:true 이고, 나머지는
 * exclusionReason("냉방 겹침 · 합계 제외")이 문구까지 내려온다. **프론트가 만들지 않는다.**
 */
import GpTag from './GpTag.vue'
import GpDelta from './GpDelta.vue'
import IconCheck from './icons/IconCheck.vue'
import { formatDifficulty, formatSeasonTags } from '@/utils/format'

defineProps({
  mission: { type: Object, required: true },
  modelValue: { type: Boolean, default: false },
  counted: { type: Boolean, default: true }, // preview 의 counted
  exclusionReason: { type: String, default: '' }, // preview 의 exclusionReason
  recommended: { type: Boolean, default: false },
  rateCap: { type: Number, default: null }, // segment 의 missionRateCap. capped 문구에 쓴다
})
defineEmits(['update:modelValue'])
</script>

<template>
  <label
    class="flex cursor-pointer items-start gap-3 py-[13px]"
    :class="{ 'opacity-60': !counted }"
  >
    <!--
      브라우저 기본 체크박스는 모서리를 못 깎는다. input 에 appearance-none 을 주고 input 자체를 둥근 사각형(7px)으로
      그린 뒤, 체크 아이콘은 peer-checked 로 겹쳐 올린다 — 홈 오늘의 실천(EcoTodayMissions)과 같은 control 토큰이되
      거긴 「완료」라 원이고 여긴 「고르기」라 사각형이다(2026-09-09 수현). input 을 sr-only 로 숨기는 방식은
      클릭 때 문서 맨 위로 튀어서 쓰지 않는다.
    -->
    <span class="relative mt-0.5 size-(--gp-checkbox) flex-none">
      <input
        type="checkbox"
        class="peer ease-standard border-control-border bg-surface checked:border-control-on checked:bg-control-on focus-visible:ring-primary/40 m-0 size-full cursor-pointer appearance-none rounded-[7px] border-[1.8px] transition-colors duration-140 outline-hidden focus-visible:ring-2"
        :checked="modelValue"
        @change="$emit('update:modelValue', $event.target.checked)"
      />
      <span
        class="text-on-primary pointer-events-none absolute inset-0 flex items-center justify-center opacity-0 peer-checked:opacity-100"
        aria-hidden="true"
      >
        <IconCheck :size="13" />
      </span>
    </span>

    <span class="min-w-0 flex-1">
      <span class="text-list-title tracking-body block leading-[1.35] font-normal">{{
        mission.title
      }}</span>

      <span class="mt-[7px] flex flex-wrap items-center gap-[5px]">
        <GpTag small>{{ formatDifficulty(mission.difficulty) }}</GpTag>
        <GpTag v-if="formatSeasonTags(mission.seasonTags)" small tone="sub">
          {{ formatSeasonTags(mission.seasonTags) }} 전용
        </GpTag>
        <GpTag v-if="mission.evidenceText" small :tone="counted ? 'primary' : 'sub'">
          {{ mission.evidenceText }}
        </GpTag>
        <!-- 상한이 걸려 잘렸으면 알린다 (api-spec 9.1 · 계산식 11) -->
        <GpTag v-if="mission.capped && rateCap !== null" small tone="estimated">
          한 미션 상한 {{ rateCap }}% 적용
        </GpTag>
        <GpTag v-if="!counted && exclusionReason" small>{{ exclusionReason }}</GpTag>
        <GpTag v-if="recommended" small tone="confirmed">추천</GpTag>
      </span>
    </span>

    <GpDelta
      :value="mission.computedRate"
      size="sm"
      :show-word="false"
      class="min-w-12 flex-none justify-end pt-px"
    />
  </label>
</template>
