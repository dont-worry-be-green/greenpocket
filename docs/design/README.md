# 디자인 시스템

## 토큰의 정본은 코드다

**`frontend/src/assets/main.css` 의 `@theme static` 블록이 유일한 정본이다.**
색을 바꾸려면 그 파일을 고친다. 이 폴더의 파일을 고쳐도 화면은 바뀌지 않는다.

| 파일 | 무엇 | 고쳐도 되나 |
| --- | --- | --- |
| `frontend/src/assets/main.css` | 토큰 228개 + Tailwind 유틸리티 | **여기를 고친다** |
| `design-system.md` | 색 도출 근거·WCAG 대비 실측표·글자 스케일 근거·홈 확정 시안 해부·v2→v3 이행표 | 읽기용 |
| `tokens.json` | W3C Design Tokens 포맷. Figma Variables 동기화용 | 생성물 |
| `frontend/scripts/tokens-json.mjs` | `main.css` → `tokens.json` 생성기 | — |

```bash
cd frontend && node scripts/tokens-json.mjs   # main.css 를 고쳤으면 다시 돌린다
```

## v3 (2026-09-09) — 줄이기 홈 확정 시안 기준

- **토큰 이름은 v2 205개를 하나도 지우거나 바꾸지 않았다.** 값이 바뀌고 39개가 늘었다.
  기존 화면은 그대로 컴파일된다(빌드·vitest 261건 통과). 무엇이 어떻게 달라 보이는지는 `design-system.md` 10절.
- 글자 스케일은 신한 슈퍼SOL 과 잉크 높이를 직접 맞춰 ×0.773 했다. 탭 라벨만 11→14 로 커졌다.
- 카드는 `rounded-card`(24) + `shadow-card`(잉크색 2겹). v2 의 「카드에 그림자 금지」는 폐기됐다.
- 요금색은 글자(`text-elec`) · 채움(`*-fill`) · 바탕(`*-bg`) 세 벌이고, 페이스 4단계(`pace-behind/near/on/ahead`)가 생겼다.

## 시안 원본 CSS 를 쓰지 않는 이유

Claude cowork 로 뽑은 v2 원본에는 `greenpocket.css` 가 있었지만 **저장소에 넣지 않았다.**
토큰 참조가 색 130회뿐이고 `px` 리터럴이 428개라 토큰을 바꿔도 컴포넌트가 따라오지 않았고,
아트보드(393×852 고정 래퍼) 좌표를 전제해 실제 브라우저에서 깨졌다.
v3 홈 시안(`gp-home-final.html`)도 마찬가지로 **값만 토큰으로 옮겼다.** 시안 CSS 를 그대로 붙이지 않는다.

## 이름이 바뀐 토큰 3개 (v2 때부터)

Tailwind 클래스로 읽었을 때 어색하거나 겹쳐서 이름만 줄였다.

| 시안 원본 | `main.css` | 이유 |
| --- | --- | --- |
| `--gp-color-text-strong` | `--color-ink` | `text-text-strong` 이 됨 |
| `--gp-color-text` | `--color-ink-soft` | `--text-body`(글자 크기)와 클래스명 충돌 |
| `--gp-color-bg` | `--color-canvas` | `bg-bg` 가 됨 |

## 남은 일

- 홈(WF-06) → 혜택(BN-01) → 포켓(PK-01) 순으로 v3 문법으로 옮기기. `design-system.md` 9·11절
- Figma Variables 동기화 — MCP 쿼터 리셋(10/1) 이후. `tokens.json` 을 그대로 넣으면 된다
- 다크 모드는 범위 밖이다.
