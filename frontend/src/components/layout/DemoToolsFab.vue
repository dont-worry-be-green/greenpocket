<script setup>
/*
 * 데모 도구 (개발 빌드 전용)
 *
 * 세 가지를 앱 안에서 할 수 있게 한다.
 *   ① 데이터 소스 전환 — 목데이터(픽스처) ↔ 실 API
 *   ② 화면 바로가기 — 정상 흐름으로는 몇 단계를 거쳐야 닿는 상태들
 *   ③ 데모 초기화 — `POST /demo/reset` (api-spec.md 4.4 · COM-10)
 *
 * ⚠️ **왜 떠 있는 버튼인가.** ①③ 을 시작 화면(ONB-01)에 두면 쓸 수 없다.
 * 온보딩을 마치면 가드가 `/onboarding/login` 진입을 막고 홈으로 되돌리기 때문에
 * (`router/guards.js`), 목데이터로 한 번 걸으면 실 API 로 되돌릴 길이 없고
 * 초기화 버튼은 정작 초기화가 필요한 상태에서 닿지 않는다.
 *
 * `App.vue` 가 `import.meta.env.DEV` 로 감싸므로 `npm run build` 결과물에는 없다.
 *
 * ── 왜 오른쪽 가장자리 중앙인가 ────────────────────────────────────────────
 * 화면 아래는 전부 차 있다. 탭바(z-20)·중앙 FAB(z-30)·고정 CTA 바가 하단 112px 을 쓰고,
 * ONB-01 은 CTA 가 고정 바가 아니라 본문 흐름 맨 아래라 그보다 위까지 올라온다.
 * 위쪽도 헤더가 차지한다. 남는 자리가 오른쪽 세로 중앙이라 거기에 손잡이처럼 붙인다.
 *
 * 이 파일만 `api/` 함수를 스토어 없이 직접 부른다(frontend/AGENTS.md 5절 예외).
 * 화면에 남길 상태가 없어 스토어를 하나 더 만드는 쪽이 더 큰 비용이다.
 */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { clearDemoKey } from '@/api/client'
import { DATA_SOURCE, getDataSource, setDataSource } from '@/api/dataSource'
import { resetDemo } from '@/api/demo'
import GpButton from '@/components/ui/GpButton.vue'
import GpModal from '@/components/ui/GpModal.vue'
import { useAuthStore } from '@/stores/auth'
import { DEMO_SHORTCUTS } from './demoShortcuts'

const SOURCES = [
  { value: DATA_SOURCE.API, label: '실 API', hint: '서버에 실제로 호출해요 · 새로고침해도 남아요' },
  { value: DATA_SOURCE.FIXTURE, label: '목데이터', hint: '서버 없이 걸어요 · 미션 목록이 있어요' },
]

const router = useRouter()

const open = ref(false)
const resetting = ref(false)
const error = ref(null)

// 모달을 열 때마다 읽는다. 저장소를 손으로 고쳤어도 실제 값이 보이게
const current = computed(() => (open.value ? getDataSource() : null))
const isApiMode = computed(() => current.value === DATA_SOURCE.API)

function choose(value) {
  if (value === current.value) return
  setDataSource(value)
  // 스토어에 이미 담긴 응답까지 갈아엎어야 해서 새로고침이 가장 확실하다
  window.location.reload()
}

function go(to) {
  open.value = false
  router.push(to)
}

/*
 * 서버는 `DELETE FROM app_user` 한 줄이라 이 데모 키의 사용자가 사라진다.
 * 로컬에 남은 키·온보딩 플래그도 함께 버리고 ONB-01 로그인부터 다시 걷는다.
 * 라우터가 아니라 하드 이동인 이유는 Pinia 에 남은 이전 사용자 데이터까지 비우기 위해서다.
 */
async function reset() {
  if (resetting.value) return
  resetting.value = true
  error.value = null
  try {
    await resetDemo()
    useAuthStore().clearSession()
    clearDemoKey()
    window.location.assign('/onboarding/login')
  } catch (nextError) {
    error.value = nextError
    resetting.value = false
  }
}
</script>

<template>
  <!-- 래퍼는 클릭을 통과시킨다. 안 그러면 화면 오른쪽 절반이 통째로 눌리지 않는다 -->
  <div
    class="pointer-events-none fixed inset-x-0 top-1/2 z-40 mx-auto flex max-w-(--gp-viewport-w) -translate-y-1/2 justify-end"
  >
    <button
      type="button"
      class="bg-ink text-on-primary shadow-float text-caption pointer-events-auto cursor-pointer rounded-l-md border-0 py-2 pr-1.5 pl-2 font-semibold opacity-70"
      aria-label="데모 도구 열기"
      @click="open = true"
    >
      DEV
    </button>
  </div>

  <GpModal :open="open" title="데모 도구" @close="open = false">
    <!-- 바로가기가 길어 모달이 화면을 넘긴다. GpModal 은 공용이라 여기서 감싼다 -->
    <div class="-mr-2 max-h-[65vh] space-y-5 overflow-y-auto pr-2">
      <section>
        <h3 class="text-body-strong text-ink mt-0 mb-1">데이터 소스</h3>
        <p class="text-caption text-muted mt-0 mb-3">
          온보딩과 What-if 에 적용돼요. 실 API 에서는 실천 미션 목록이 비어 있어요 —
          <code>mission_catalog</code> 시드가 아직 없어요(#75).
        </p>

        <div class="space-y-2">
          <button
            v-for="source in SOURCES"
            :key="source.value"
            type="button"
            class="text-body flex min-h-14 w-full cursor-pointer items-center gap-3 rounded-lg border px-4 text-left"
            :class="
              source.value === current
                ? 'border-primary bg-primary-bg text-primary-on-soft'
                : 'border-border bg-surface text-ink'
            "
            :aria-pressed="source.value === current"
            @click="choose(source.value)"
          >
            <span class="flex-1">
              <span class="block font-semibold">{{ source.label }}</span>
              <span class="text-caption text-muted block">{{ source.hint }}</span>
            </span>
            <span v-if="source.value === current" class="text-caption font-semibold">사용 중</span>
          </button>
        </div>
      </section>

      <section>
        <h3 class="text-body-strong text-ink mt-0 mb-1">화면 바로가기</h3>
        <p v-if="isApiMode" class="text-caption text-negative mt-0 mb-3">
          지금은 실 API 모드예요. 아래 대부분은 시드 데이터가 없어 목데이터 모드에서 보여요.
        </p>
        <p v-else class="text-caption text-muted mt-0 mb-3">
          정상 흐름으로는 여러 단계를 거쳐야 닿는 상태들이에요.
        </p>

        <div v-for="section in DEMO_SHORTCUTS" :key="section.group" class="mt-3 first:mt-0">
          <p class="text-caption-sm text-muted mt-0 mb-1.5 font-semibold">{{ section.group }}</p>
          <div class="flex flex-wrap gap-1.5">
            <button
              v-for="item in section.items"
              :key="item.to"
              type="button"
              class="bg-surface-sub text-caption text-ink-soft h-(--gp-pill-h) cursor-pointer rounded-full border-0 px-3 font-semibold"
              @click="go(item.to)"
            >
              {{ item.label }}
            </button>
          </div>
        </div>
      </section>

      <section>
        <h3 class="text-body-strong text-ink mt-0 mb-1">데모 초기화</h3>
        <p class="text-caption text-muted mt-0 mb-3">
          이 기기의 사용자 데이터를 서버에서 지우고 처음(ONB-01)부터 다시 시작해요. 고지서 · 목표 ·
          포켓 거래가 전부 사라져요.
        </p>

        <GpButton variant="primary" size="cta" :disabled="resetting" @click="reset">
          {{ resetting ? '초기화하는 중...' : '초기화하고 처음부터' }}
        </GpButton>

        <p v-if="error" class="text-body-sm text-negative mt-3 mb-0">{{ error.message }}</p>
      </section>
    </div>
  </GpModal>
</template>
