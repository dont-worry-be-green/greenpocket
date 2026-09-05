<script setup>
/*
 * WF-01a 에코마일리지 본인확인 (B-1-01 · COM-05)
 *
 * ⚠️ **기능명세서에 없는 화면이다.** WF-01 의 「연동하기」가 `POST /eco/link` 를 곧바로 불렀는데,
 * 서버는 `X-Demo-Key` 밖에 모르면서 「작년 우리 집 사용량」을 내려준다. 신원을 잇는 단계가
 * 흐름에 통째로 빠져 있어 그 자리를 화면으로 채운다. **서버 계약은 바꾸지 않는다.**
 *
 * ── 상태 두 개 ──────────────────────────────────────────────────────────
 *   FORM        본인확인 입력 (WF-01a)
 *   NOT_MEMBER  미가입 안내 (WF-01b)
 * 둘은 라우트가 아니다. 뒤로가기는 어느 쪽에서든 WF-01 로 돌아가야 한다.
 *
 * 인증에 성공하면 `startLink()` 까지 여기서 부르고 홈으로 `replace` 한다.
 * 뒤로가기로 이 화면에 되돌아오면 이미 연동이 도는 중이라 다시 인증할 이유가 없다.
 * 그다음 진행 표시(WF-02)와 폴링은 홈이 받는다.
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import EcoNotMemberPanel from '@/components/eco/EcoNotMemberPanel.vue'
import EcoVerifyForm from '@/components/eco/EcoVerifyForm.vue'
import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import { useEcoStore } from '@/stores/eco'
import { useOnboardingStore } from '@/stores/onboarding'

const router = useRouter()
const store = useEcoStore()
const onboarding = useOnboardingStore()

const step = ref('FORM')

// 온보딩에서 받은 이름을 프리필한다. 새로고침하면 비어 있고, 그때는 직접 입력한다
const defaultName = computed(() => onboarding.user?.name ?? '')

// 누리집 주소는 GET /eco/status 가 준다(api-spec.md 8.1). 화면이 만들지 않는다
const externalUrl = computed(() => store.status?.externalUrl ?? '')

const errorMessage = computed(() => store.error?.message ?? '')

// 홈을 거치지 않고 주소창으로 들어오면 status 가 비어 누리집 주소를 모른다
onMounted(() => {
  if (!store.status) store.fetchStatus()
})

/** 인증 → 연동 시작 → 홈. 홈이 WF-02 를 그리고 폴링을 이어받는다 */
async function verifyAndLink() {
  const verified = await store.verifyIdentity()
  if (!verified) return
  await startLink()
}

/** WF-01b 에서 「가입 다 했어요」로도 들어온다 */
async function startLink() {
  const linkJobId = await store.startLink()
  if (!linkJobId) return
  router.replace('/whatif')
}

function openSite() {
  if (externalUrl.value) window.open(externalUrl.value, '_blank', 'noopener')
}
</script>

<template>
  <AppSubLayout back="/whatif">
    <div class="pt-2">
      <EcoVerifyForm
        v-if="step === 'FORM'"
        :default-name="defaultName"
        :loading="store.isLoading"
        :error-message="errorMessage"
        @verify="verifyAndLink"
        @not-member="step = 'NOT_MEMBER'"
      />

      <EcoNotMemberPanel
        v-else
        :external-url="externalUrl"
        :loading="store.isLoading"
        @open-site="openSite"
        @retry="startLink"
      />
    </div>
  </AppSubLayout>
</template>
