# GreenPocket Frontend — Vue 규칙

> 루트 `AGENTS.md`를 먼저 읽는다. 이 문서는 프론트엔드 전용 규칙이다.

---

## 1. 스택 (고정)

| 구분 | 사용 | 사용하지 않음 |
| --- | --- | --- |
| 프레임워크 | **Vue 3** | Vue 2, Nuxt |
| 언어 | **JavaScript** | TypeScript |
| 빌드 | Vite | Webpack, Vue CLI |
| 상태관리 | **Pinia (Setup Store)** | Vuex, Pinia Options Store |
| 라우팅 | Vue Router | |
| 스타일 | **Tailwind CSS v4** | Tailwind v3 관행, shadcn-vue, Vuetify, PrimeVue, Element Plus, CSS-in-JS |
| HTTP | axios (공통 인스턴스) | 컴포넌트에서 `fetch` 직접 호출 |

**UI 컴포넌트 라이브러리를 도입하지 않는다.** 버튼·카드·모달·토스트 등은 Tailwind로 직접 만들어 `components/ui/`에 둔다.

### 디자인 토큰 — `src/assets/main.css` 하나가 전부다

205개 토큰이 `@theme static` 블록에 있고, **토큰이 곧 Tailwind 클래스**다.

| 쓸 것 | 쓰지 말 것 |
| --- | --- |
| `bg-primary` · `text-muted` · `border-divider` | `bg-green-600` (Tailwind 기본 팔레트), `#078753` |
| `text-body` · `text-amount` · `text-caption` (크기+굵기+행간이 함께 붙는다) | `text-[15px] font-normal leading-[1.6]` |
| `rounded-lg`(16) · `rounded-md`(12) — 7단계만 | `rounded-[14px]` |
| `p-4`·`gap-3` (Tailwind 기본 4px 스케일이 시안과 맞는다) | 별도 간격 토큰 |
| `h-(--gp-cta-h)` · `size-(--gp-fab)` — 유틸리티가 없는 치수 | `h-[48px]` |

- **색 이름 3개만 원본과 다르다.** `text-strong`→`ink`, `text`→`ink-soft`, `bg`→`canvas`. 값은 같다.
- 필요한 토큰이 없으면 `main.css`에 **추가**한다. 화면에서 hex를 쓰지 않는다.
- **`@theme static`인 이유** — 기본 `@theme`는 안 쓰인 토큰을 지워서 `var(--gp-cta-h)`가 조용히 깨진다.
- 시안 원본(`design-system/greenpocket.css`)은 **쓰지 않는다.** px 하드코딩 428곳에 아트보드 좌표계라 실제 화면에서 깨진다. 토큰만 위 `@theme`로 흡수했다.

### Tailwind v4 — 버전 주의 ⚠️

**v3와 설정 방식이 다르다.** 널리 알려진 v3 관행을 그대로 쓰면 **에러 없이 조용히 무시되어** 원인을 찾기 어렵다.

| 하지 말 것 | 올바른 것 |
| --- | --- |
| `tailwind.config.js` 생성 | **설정 파일이 없다.** 색·간격 등 토큰은 CSS `@theme` 블록에 정의한다 |
| `@tailwind base/components/utilities` | `@import "tailwindcss";` 한 줄 |
| `postcss.config.js`·`autoprefixer` 추가 | `vite.config.js`의 `@tailwindcss/vite` 플러그인 하나로 끝난다 |
| `bg-opacity-50` · `flex-shrink-0` · `outline-none` | `bg-black/50` · `shrink-0` · `outline-hidden` |
| `shadow-sm`·`rounded-sm`·`blur-sm`을 v3 감각으로 사용 | **스케일이 한 칸 밀렸다.** v3의 `shadow-sm`은 v4의 `shadow-xs`다 |

- 위 마지막 항목은 **에러가 나지 않고 결과만 달라진다.** 시안과 그림자·모서리가 미묘하게 다르면 이것부터 확인한다.
- `flex`·`grid`·`px-4`·`text-sm`·`md:`·`hover:` 등 대부분의 유틸리티는 v3와 같다. 차이는 **설정과 일부 유틸리티 이름**에 몰려 있다.
- 참고한 예제가 v3 기준이면 **그대로 옮기지 말고 위 표로 변환한다.**

---

## 2. 컴포넌트 작성 규칙 ⚠️ 가장 자주 어기는 부분

- **Composition API + `<script setup>`만 사용한다.**
  Options API(`export default { data() {}, methods: {}, computed: {} }`)를 쓰지 않는다.
- **`<script setup>` 안에서 `export default`를 쓰지 않는다.**
- **TypeScript 구문을 쓰지 않는다.**
  참고한 예제에 타입 표기(`ref<string>()`, `defineProps<{...}>()`, `: string`, `interface`)가 있으면 **제거하고 JavaScript로 변환한다.**
- props는 `defineProps({ ... })` 런타임 선언으로 작성한다.
- 파일 순서는 `<script setup>` → `<template>` → `<style>`.

```vue
<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  amount: { type: Number, required: true },
  isConfirmed: { type: Boolean, default: false },
})
const emit = defineEmits(['select'])

const isOverAverage = computed(() => props.amount > 0)
</script>

<template>
  <button class="w-full rounded-lg px-4 py-3" @click="emit('select')">
    ...
  </button>
</template>
```

---

## 3. Pinia — Setup Store로 통일

`state:` / `getters:` / `actions:` 객체 형태(Options Store)를 쓰지 않는다.

```js
// stores/bill.js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useBillStore = defineStore('bill', () => {
  const bills = ref([])
  const isLoading = ref(false)

  const latestBill = computed(() => bills.value[0] ?? null)

  async function fetchBills() { /* ... */ }

  return { bills, isLoading, latestBill, fetchBills }
})
```

---

## 4. 디렉토리 구조

```
frontend/src/
├── api/           client.js(공통 axios) + 도메인별 API 함수
├── stores/        Pinia 스토어
├── views/<도메인>/      라우트 단위 페이지
├── components/
│   ├── ui/              버튼·카드·모달 등 공통 UI
│   └── <도메인>/        도메인 전용 컴포넌트
├── composables/   재사용 로직
├── utils/         format.js 등 포맷터
└── router/
    ├── index.js         라우터 생성. 화면을 추가할 때 여기를 고치지 않는다
    └── routes/<도메인>.js
```

`<도메인>`은 6개로 고정한다. 화면 ID(`api-spec.md` 15.3)와 1:1로 대응한다.

| 도메인 | 화면 ID | 내용 |
| --- | --- | --- |
| `onboarding` | ONB | 시작·프로필 |
| `analysis` | AN | 고지서·생활비 분석 |
| `greenlife` | BN | 녹색생활실천 |
| `eco` | WF | 에코마일리지·목표·평가 |
| `pocket` | PK | 그린포켓·출금 |
| `mypage` | MY | 마이페이지·보관함 |

- **라우트는 `router/routes/<도메인>.js`에만 추가한다.** 넷이 동시에 화면을 붙이는데 `router/index.js`를 같이 고치면 매번 충돌한다.
- **다른 도메인 폴더를 수정하지 않는다.** 공용은 `components/ui/`·`utils/`·`api/client.js`뿐이고, 이 셋을 고칠 때는 먼저 알린다.

---

## 5. API 호출

- **컴포넌트에서 axios를 직접 호출하지 않는다.** `api/` 함수 → `stores/` → 컴포넌트 순서를 지킨다.
- 공통 인스턴스는 **`api/client.js`** 하나다. 새 axios 인스턴스를 만들지 않는다.
  `X-Demo-Key` 첨부, 공통 응답 래퍼(`data`) 언래핑, 에러 정규화(`ApiError`)가 이미 들어 있다.
  **컴포넌트에서 `res.data.data`를 파싱하지 않는다.**
- **`docs/api/api-spec.md`에 정의된 필드명만 사용한다.**
  백엔드를 다른 사람이 개발 중이라, 상상해서 만든 필드는 통합 시점에 반드시 깨진다. 명세에 없으면 **먼저 질문한다.**
- 화면별 호출 목록은 `api-spec.md` **15.3 「화면 → API」** 표에 있다. 화면 작업 전에 이 표를 먼저 본다.
- 모든 API 호출에 **로딩·빈 결과·실패·재시도** 상태를 처리한다 (기능명세서 COM-08). 무반응 화면이나 빈 화면을 남기지 않는다.
- **비동기 작업은 폴링한다.** OCR(`POST /bills/ocr` → `GET /bills/ocr/{jobId}`, 30초)과 에코 연동(`POST /eco/link` → `GET /eco/link/{linkJobId}`, 20초)은 `202` 응답 후 폴링하며, 타임아웃 시 재시도 버튼을 둔다.
- **데이터 없음은 오류가 아니다.** `200` + `available: false` + `unavailableReason` 응답을 에러 화면으로 처리하지 말고 안내 문구로 렌더링한다.
- **전환·출금 요청에는 `Idempotency-Key` 헤더(UUID)를 붙인다.** 재시도 시 같은 키를 재사용해야 거래가 중복 생성되지 않는다.

---

## 6. 표시 규칙 (전 화면 공통)

기능명세서 COM-06·COM-07에 정의된 규칙이다. **화면마다 다르게 구현하면 안 된다.**
**서버는 숫자와 enum만 내려준다.** 포맷팅은 전부 FE 책임이다.
**`utils/format.js`의 포맷터를 쓰고, 컴포넌트에서 `toLocaleString`·`toFixed`를 직접 부르지 않는다.** 없는 포맷이 필요하면 이 파일에 추가한다.

공통 컴포넌트가 `components/ui/`에 7개 있다 — `GpButton` `GpCard` `GpTag` `GpDelta` `GpBandPicker` `GpMissionRow` `GpTabBar`.
비슷한 것을 새로 만들기 전에 먼저 확인한다. `GpDelta`·`GpTag`가 위 증감·상태 라벨 규칙을 이미 지키고 있다.

- **금액:** 천 단위 구분기호 + `원`. 마일리지는 `M` (1M = 1원).
- **증감:** 부호 대신 **화살표 + 숫자 + 말** (`↓12% 줄었어요`, `↑2% 늘었어요`). 비교 차액만 `+4,300원`처럼 부호를 붙인다.
- **상태 라벨 4종:** 예상 마일리지·적립 예정 포인트 → `예상` / `적립 예정`, 평가 확정 마일리지 → `확인`, 포켓 입금 거래 → `입금`, 녹색생활실천 지급분 → `지급 완료`.
  **색상만으로 구분하지 않고 텍스트 라벨을 반드시 함께 넣는다.**
- **돈의 3단계를 섞지 않는다.** 예상 마일리지 → 적립된 마일리지(아직 현금 아님) → 그린포켓 입금(실제 잔액). 예상값과 "덜 낸 요금"(`savedAmount`) 옆에 전환·출금 버튼을 두지 않는다.
- **처리 중을 완료로 표시하지 않는다.** OCR·연동은 `SUCCEEDED`, 거래는 `COMPLETED`가 아니면 완료 화면(PK-04 등)을 띄우지 않는다.
- 사용량(kWh·㎥)보다 원화 금액을 먼저·크게 노출한다.
- **시안의 하드코딩 수치를 그대로 옮기지 않는다.** WF-04·WF-06·WF-07·WF-10 시안의 `10.5% · 12% · 83%`는 틀린 값이고, 실제는 `11.322% · 12.499% · 82.5%` 다(결정 C-13).

---

## 7. 모바일 우선

- **모바일 웹이 기본 타깃이다.** Tailwind 클래스를 모바일 기준으로 작성하고 `sm:` 이상 브레이크포인트는 필요할 때만 추가한다.
- 최상위 컨테이너에 최대 너비를 두어 데스크톱에서도 모바일 레이아웃을 유지한다.
- 터치 타겟은 최소 44px 높이를 확보한다.

---

## 8. 검증 명령어

작업 후 **반드시 빌드 통과를 확인한다.**

```bash
cd frontend
npm run dev       # 개발 서버
npm run build     # 빌드 확인
```

---

## 9. 금지사항

- Options API, TypeScript 구문, UI 컴포넌트 라이브러리 도입
- `package.json` 의존성 임의 추가·변경
- 인라인 `style` 남용 (Tailwind 클래스를 사용한다)
- 요청 범위 밖 컴포넌트 리팩터링·파일 삭제
- 백엔드 API가 아직 없다는 이유로 임의의 목업 데이터 구조를 만들어 굳히는 것
  (필요하면 `api-spec.md`를 근거로 하고, 없으면 질문한다)
