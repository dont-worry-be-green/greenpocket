# 그린포켓 디자인 시스템 v2.0

팀 돈워리비그린 · 2026 KB IT's Your Life 해커톤 본선
2026-09-02 · 프론트엔드 Vue.js

---

## 0. 파일 구성

| 파일 | 무엇 | 누가 고치나 |
|---|---|---|
| `tokens.css` | **정본.** 모든 값의 유일한 출처 | 디자인 |
| `tokens.json` | tokens.css에서 생성. Figma Variables·Style Dictionary 임포트용 | 자동 생성 (직접 고치지 말 것) |
| `greenpocket.css` | 컴포넌트 클래스. 토큰만 참조 | 디자인 + FE |
| `components/*.vue` | Vue 컴포넌트 골격 | FE |

```js
// main.js
import './styles/tokens.css'
import './styles/greenpocket.css'
```

**규칙 셋**

1. 화면 코드에 hex를 쓰지 않는다. 없으면 `tokens.css`에 토큰을 추가한다.
2. 원시값(`--gp-green-700`)을 화면에서 참조하지 않는다. 반드시 의미 토큰(`--gp-color-primary`)을 거친다.
3. 폰트 13단계·radius 7단계 밖의 값을 새로 만들지 않는다.

---

## 1. 토큰 구조 — 3계층

```
원시값 primitive      --gp-green-700: #078753          값의 출처
   ↓
의미   semantic       --gp-color-primary: var(--gp-green-700)    화면이 쓰는 층
   ↓
컴포넌트 component     --gp-cta-h: 48px                 특정 컴포넌트 치수
```

총 **205개** — 원시값 42 · 의미 136 · 컴포넌트 27.

왜 3계층인가: 지금까지는 의미 계층만 있어서 "이 회색은 어디서 왔나"를 추적할 수 없었다.
구분선용 회색이 `#E1E3E2 · #E8EBE9 · #E5E8E6 · #DDE1DF · #EAECEB · #EFF2F0` 여섯 종으로
번져 있었는데, 사람 눈에 구분되지 않는 차이라 대부분 실수였다.

---

## 2. 색

### 2-1. 브랜드

신한 슈퍼SOL 스크린샷 11장을 픽셀 추출한 뒤 OKLCH 색상만 261.6° → 157.3°로 회전했다.
157.3°는 그린포켓 앱 아이콘 그린이다. 명도(L)와 채도(C)는 원본을 유지하고 대비 미달분만 보정했다.

| 토큰 | 값 | 어디에 |
|---|---|---|
| `--gp-color-primary` | `#078753` | 채움 버튼 · 탭 활성 |
| `--gp-color-primary-pressed` | `#026A3F` | 누른 상태 |
| `--gp-color-primary-on-soft` | `#037D4C` | 연한 배경 위 그린 텍스트 |
| `--gp-color-primary-soft` | `#00A968` | 아이콘 · 그래프 |
| `--gp-color-primary-bg` | `#E7F3EC` | pill 버튼 배경 |

### 2-2. 의미 계층 — 이 서비스의 핵심

돈이 세 단계를 거치기 때문에 색도 세 갈래다.

| 상태 | 토큰 | 뜻 |
|---|---|---|
| **예상** | `--gp-color-estimated` | 아직 확정되지 않은 값. 아웃라인 배지 전용 |
| **확정된 돈** | `--gp-color-confirmed` | 적립됐지만 아직 현금이 아닌 마일리지 |
| **성공** | `--gp-color-positive` | 절감 달성 |
| **오류** | `--gp-color-negative` | 화면의 유일한 빨강 |

**배경 위에 올라가는 텍스트 색을 반드시 짝으로 쓴다.** v1에서는 배경 토큰만 있고
그 위에 뭘 올릴지가 없어 `#0A5C39`, `#8A6512` 같은 값이 화면에 하드코딩돼 있었다.

| 배경 | 그 위 텍스트 | 그 위 구분선 |
|---|---|---|
| `--gp-color-positive-bg` | `--gp-color-on-positive` | — |
| `--gp-color-confirmed-bg` | `--gp-color-on-confirmed` / `--gp-color-on-confirmed-muted` | `--gp-color-confirmed-divider` |
| `--gp-color-primary` | `--gp-color-on-primary` | — |

### 2-3. 증감 표기

부호(`−12%`)는 늘었다는 뜻인지 줄었다는 뜻인지 매번 판단해야 한다.
게다가 이 서비스에서는 `−`가 좋은 뜻이고 `+`가 나쁜 뜻이라 더 헷갈렸다.

**화살표 + 숫자 + 말 세 겹으로 방향을 못 박는다. 색만으로 구분하지 않는다.**

```
↓ 12% 줄었어요   --gp-color-decrease
↑ 2%  늘었어요   --gp-color-increase
```

컴포넌트: `<GpDelta :value="12" />` · `<GpDelta :value="-2" />`

### 2-4. 요금 계열

진단 탭 그래프와 아이콘 타일이 쓴다. v1에는 없어서 생성 스크립트에만 있던 값이다.

| 요금 | 선·아이콘 | 배경 |
|---|---|---|
| 전기 | `--gp-color-elec` `#078753` | `--gp-color-elec-bg` |
| 도시가스 | `--gp-color-gas` `#E08A1E` | `--gp-color-gas-bg` |
| 수도 | `--gp-color-water` `#1B7FC4` | `--gp-color-water-bg` |

### 2-5. 차트

| 토큰 | 규칙 |
|---|---|
| `--gp-chart-reference` | 작년·지역 평균은 **회색 기준선** |
| `--gp-chart-current` | 이번 기간만 상태색 |
| `--gp-chart-below-goal` | 목표 미달 막대는 앰버 |

세 요금을 한 차트에 그릴 때는 **축척을 공유**한다. 이중 축을 쓰지 않는다.

### 2-6. 선은 3단계뿐

| 토큰 | 어디에 |
|---|---|
| `--gp-color-border` | 카드 테두리 · 입력 |
| `--gp-color-divider` | 카드 안 행 구분선 |
| `--gp-color-track` | 슬라이더 · 비교 바 트랙 |

이 밖의 회색을 새로 만들지 않는다.

### 2-7. 접근성

WCAG AA(4.5:1) 실측. `#9A9E9C`(비활성 탭)이 **2.71:1로 미달**이라 `--gp-color-text-muted`로 올렸다.

| 조합 | 대비 |
|---|---|
| 본문 / 흰 배경 | 10.41:1 ✓ |
| 본문 / 앱 배경 | 9.73:1 ✓ |
| primary 위 흰 글씨 | 4.57:1 ✓ |
| positive-bg 위 텍스트 | 7.24:1 ✓ |
| confirmed-bg 위 제목 | 4.50:1 ✓ |
| confirmed-bg 위 보조 | 4.66:1 ✓ |
| 비활성 탭 (수정 후) | 4.92:1 ✓ |

---

## 3. 타이포

Pretendard. 393pt 기준. **13단계만 쓴다.**

| 토큰 | px | 용도 |
|---|---|---|
| `--gp-font-size-50` | 11 | 탭바 라벨 · 마이크로 배지 |
| `--gp-font-size-100` | 12 | 보조 캡션 · 차트 축 |
| `--gp-font-size-200` | 13 | 캡션 · 출처 · 기준일 |
| `--gp-font-size-300` | 14 | 리스트 보조 · 배지 |
| `--gp-font-size-400` | 15 | 본문 |
| `--gp-font-size-500` | 16 | 버튼 · 입력 |
| `--gp-font-size-600` | 17 | 리스트 제목 |
| `--gp-font-size-700` | 20 | 카드 섹션 제목 |
| `--gp-font-size-800` | 24 | 강조 수치 |
| `--gp-font-size-900` | 26 | 페이지 타이틀 |
| `--gp-font-size-1000` | 28 | 금액 히어로 |
| `--gp-font-size-1100` | 34 | 대형 수치 (진행률) |
| `--gp-font-size-1200` | 42 | 결과 화면 대형 수치 |

시안에서는 30종이 쓰이고 있었다 (`15.5px` 33회, `13.5px` 45회, `14.5px` 18회 …).
빌드 마지막에 `_snap.py`가 스케일로 강제 정렬한다 — 생성기가 무엇을 쓰든 산출물은 스케일 안에 있다.

합성 토큰(`font` 축약형)도 함께 제공한다: `--gp-text-title` · `--gp-text-body` · `--gp-text-caption` 등 15종.

**금액·사용량 숫자에는 예외 없이 `font-variant-numeric: tabular-nums`.** (`.num` 클래스)

---

## 4. 형태

| 토큰 | px | 어디에 |
|---|---|---|
| `--gp-radius-xs` | 5 | 수치·상태 배지 (알약 아님) |
| `--gp-radius-sm` | 8 | 작은 칩 · 막대 끝 |
| `--gp-radius-md` | 12 | 입력 · CTA · 아이콘 타일 · 배너 |
| `--gp-radius-lg` | 16 | 카드 |
| `--gp-radius-xl` | 20 | 히어로 카드 · 세그먼트 |
| `--gp-radius-2xl` | 26 | 바텀시트 상단 |
| `--gp-radius-full` | 999 | pill · 필터 칩 · 토스트 · 원형 |

**원과 알약은 `--gp-radius-full`로 쓴다.** 크기의 절반값(예: 54px FAB에 27px)을 적지 않는다 —
크기가 바뀌면 원이 깨진다. 실제로 스케일 정리 중에 이 방식으로 FAB·토글·라디오가 한 번 깨졌다.

**카드에 그림자를 쓰지 않는다.** 떠 있는 것(탭바·토스트·모달)만 `--gp-shadow-float`.

---

## 5. 간격 · 치수

4px 베이스: `--gp-space-1`(4) ~ `--gp-space-10`(40).

컴포넌트 치수는 슈퍼SOL 스크린샷 연결성분 실측값이다. **4/8 그리드로 반올림하지 말 것.**

| 토큰 | px | |
|---|---|---|
| `--gp-gutter` | 12 | 화면 좌우 여백 (카드 폭 369 @393) |
| `--gp-card-pad` | 16 | 카드 내부 |
| `--gp-card-gap` | 20 | 카드 사이 |
| `--gp-safe-bottom` | 112 | 탭바 + 여백 |
| `--gp-row-h` | 60 | 리스트 행 (행 전체가 터치 영역) |
| `--gp-cta-h` | 48 | 화면 하단 CTA |
| `--gp-tabbar-h` | 60 | |
| `--gp-fab` | 56 | 가운데 What-if 버튼 |
| `--gp-min-touch` | 44 | 최소 터치 영역 |

---

## 6. 모션

| 토큰 | ms | 어디에 |
|---|---|---|
| `--gp-duration-instant` | 80 | 체크박스 · 토글 |
| `--gp-duration-fast` | 140 | 버튼 press · 호버 |
| `--gp-duration-base` | 220 | 아코디언 · 탭 전환 |
| `--gp-duration-slow` | 320 | 바텀시트 · 화면 전환 |

이징: `--gp-ease-standard` (기본) · `--gp-ease-enter` · `--gp-ease-exit`.

`greenpocket.css`에 `prefers-reduced-motion` 대응이 들어 있다.

---

## 7. 상태

| 토큰 | |
|---|---|
| `--gp-focus-ring` | `:focus-visible` 전역 적용 |
| `--gp-color-disabled-bg` / `-text` | 비활성 |
| `--gp-color-skeleton` / `-shine` | 로딩 |
| `--gp-color-overlay` | 모달 뒤 딤 |
| `--gp-color-control-border` / `-off` / `-on` | 체크박스 · 라디오 · 토글 |

z-index는 8단계로 고정: `base 0 · sticky 10 · tabbar 20 · fab 30 · sheet 40 · overlay 50 · modal 60 · toast 70`.

---

## 8. Vue 컴포넌트

| 컴포넌트 | props | 비고 |
|---|---|---|
| `GpButton` | `variant` (primary\|pill\|wide\|ghost) · `size` (cta\|wide\|pill) · `disabled` | 하단 CTA는 `primary` + `cta` 하나만 |
| `GpCard` | `title` · `caption` · `badge` · `tone` (default\|sub\|estimated\|confirmed) | 그림자 없음 |
| `GpTag` | `tone` (sub\|primary\|positive\|confirmed\|negative\|estimated) · `small` | radius-xs 사각형 |
| `GpDelta` | `value` · `size` · `digits` · `word` · `showWord` | **증감은 반드시 이걸로** |
| `GpBandPicker` | `v-model` · `bands` | 슬라이더 대신 구간 칩 |
| `GpMissionRow` | `mission` · `v-model` · `muted` · `recommended` | 출처·근거를 행 안에 |
| `GpTabBar` | `active` · `tabs` | 가운데 What-if는 FAB |

### 미션 데이터 형태

```js
{
  id: 'e2',
  title: '에어컨 하루 1시간 줄이기',
  sub: '켜 두는 시간만 줄여도 크게 달라져요',
  group: '냉방',          // 같은 그룹은 합계에서 최대값 하나만
  effect: 18,             // 우리 집 기준 추정 감축률 %
  claim: '월 40kWh · 4,880원',
  basis: '15평형 2kW를 20일 기준 · 40kWh ÷ 우리 집 223kWh',
  source: '한국에너지공단',
  level: '보통',           // 쉬움 | 보통 | 어려움
  season: 'summer',       // all | summer | winter
}
```

합계는 **그룹별 최대값만 더한다.** "에어컨 1시간 줄이기"와 "에어컨 대신 선풍기"를
둘 다 고르면 158kWh가 되지만 실제로는 그렇게 줄지 않는다.

---

## 9. 남은 일

- [ ] **Figma Variables 동기화** — `tokens.json`은 준비됨. Figma MCP 할당량이 10월 1일 리셋되면 업로드
- [ ] 아이콘 SVG를 컴포넌트로 분리 (`GpIcon`) — 지금은 문자열로 인라인
- [ ] 다크 모드 — 토큰 구조는 대응 가능하나 팔레트 미정. MVP 범위 밖
- [ ] 죽은 클래스 정리 — `greenpocket.css` 204규칙 중 앱 화면이 실제로 쓰는 것은 89개.
      나머지는 폐기 화면·문서 아트보드용이라 개발 착수 후 실사용 기준으로 한 번 더 걷어낼 것

---

## 부록 · 빌드 순서

```
ds/tokens.css  ─┬→ ds/_mkjson.py → tokens.json        (피그마·Style Dictionary)
                └→ ds/_mkbase.py → _base.css          (시안 아트보드가 품는 사본)
ds/greenpocket.css ─┘                    ↓
                                    _head.txt · *.dc.html 동기화
                                         ↓
생성기(_gen_*.py) → _snap.py(스케일 강제) → _measall.cjs(높이)
                  → _gencanvas.py → seed-canvas.mjs(아티팩트)
                  → _png.cjs / _genhtml.py(내보내기)
```

**`ds/tokens.css`와 `ds/greenpocket.css`가 정본이다.** 시안 아트보드는 이 둘을
`<style>`로 그대로 품으므로, 화면 시안과 앱이 같은 CSS를 쓴다 — HTML 내보내기에서
본 변수명이 개발 코드에 그대로 있다.

`tokens.json`과 `_base.css`는 **생성물**이다. 직접 고치면 다음 빌드에 덮인다.
`_base.css`에는 시안 전용 클래스(`.phone`, 설명 시트 `.w*`)가 덧붙는데,
이것들은 `greenpocket.css`에 들어가지 않는다.
