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
 * 합계 제외 여부는 미션 자체가 아니라 목표 미리보기(`POST .../goal/preview`)가 판단한다.
 * 같은 deviceGroup 중 computedRate 최대값만 counted:true 이고, 나머지는
 * exclusionReason("냉방 겹침 · 합계 제외")이 문구까지 내려온다. **프론트가 만들지 않는다.**
 */
import GpTag from './GpTag.vue'
import GpDelta from './GpDelta.vue'
import { formatDifficulty } from '@/utils/format'

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
    <input
      type="checkbox"
      class="accent-primary mt-0.5 size-(--gp-checkbox) flex-none"
      :checked="modelValue"
      @change="$emit('update:modelValue', $event.target.checked)"
    />

    <span class="min-w-0 flex-1">
      <span class="text-list-title tracking-body block leading-[1.35]">{{ mission.title }}</span>

      <span v-if="mission.description" class="text-caption text-muted mt-[3px] block">
        {{ mission.description }}
      </span>

      <span class="mt-[7px] flex flex-wrap items-center gap-[5px]">
        <GpTag small>{{ formatDifficulty(mission.difficulty) }}</GpTag>
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

      <!-- 산출 근거와 출처 기관. 셋 다 있는 미션만 서버가 내려준다 (B-3-01) -->
      <span class="text-nav text-muted mt-1.5 block leading-[1.55]">
        {{ mission.calculationBasis }} · {{ mission.sourceOrg }}
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
