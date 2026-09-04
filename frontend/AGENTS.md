# GreenPocket Frontend — Vue 규칙

> 루트 `AGENTS.md`를 먼저 읽는다. 이 문서는 프론트엔드 전용 규칙이다.

---

## 0. ⚠️ 시작 전 — `package.json` 이 있는지 확인한다

- **없으면 Vue 프로젝트가 아직 스캐폴딩되지 않은 것이다.** 아래 4번 디렉토리 구조와 8번 검증 명령어는 그 이후에 유효하며, `npm` 명령은 전부 실패한다.
- **스캐폴딩을 혼자 진행하지 않는다.** 전원이 공유하는 기반이라 한 사람이 정한 구조에 나머지가 묶인다. 사용자에게 먼저 알린다.

---

## 1. 스택 (고정)

| 구분 | 사용 | 사용하지 않음 |
| --- | --- | --- |
| 프레임워크 | **Vue 3** | Vue 2, Nuxt |
| 언어 | **JavaScript** | TypeScript |
| 빌드 | Vite | Webpack, Vue CLI |
| 상태관리 | **Pinia (Setup Store)** | Vuex, Pinia Options Store |
| 라우팅 | Vue Router | |
| 스타일 | **Tailwind CSS** | shadcn-vue, Vuetify, PrimeVue, Element Plus, CSS-in-JS |
| HTTP | axios (공통 인스턴스) | 컴포넌트에서 `fetch` 직접 호출 |

**UI 컴포넌트 라이브러리를 도입하지 않는다.** 버튼·카드·모달·토스트 등은 Tailwind로 직접 만들어 `components/ui/`에 둔다.

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
├── api/           axios 인스턴스와 도메인별 API 함수
├── stores/        Pinia 스토어
├── views/         라우트 단위 페이지
├── components/
│   ├── ui/        버튼·카드·모달 등 공통 UI
│   └── <도메인>/  도메인 전용 컴포넌트
├── composables/   재사용 로직
├── utils/         포맷터 등
└── router/
```

---

## 5. API 호출

- **컴포넌트에서 axios를 직접 호출하지 않는다.** `api/` 함수 → `stores/` → 컴포넌트 순서를 지킨다.
- 백엔드 공통 응답 포맷은 **axios 인터셉터에서 한 번만 벗긴다.** 컴포넌트마다 `res.data.data`를 파싱하지 않는다.
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
**서버는 숫자와 enum만 내려준다.** 포맷팅은 전부 FE 책임이므로 `utils/`의 공통 포맷터를 쓰고 컴포넌트에서 개별 포맷하지 않는다.

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
