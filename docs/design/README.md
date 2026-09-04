# 디자인 시스템

## 토큰의 정본은 코드다

**`frontend/src/assets/main.css` 의 `@theme static` 블록이 유일한 정본이다.**
색을 바꾸려면 그 파일을 고친다. 이 폴더의 파일을 고쳐도 화면은 바뀌지 않는다.

| 파일 | 무엇 | 고쳐도 되나 |
| --- | --- | --- |
| `frontend/src/assets/main.css` | 토큰 205개 + Tailwind 유틸리티 | **여기를 고친다** |
| `design-system.md` | 색 도출 근거·WCAG 대비 검증표·컴포넌트 목록 | 읽기용 |
| `tokens.json` | W3C Design Tokens 포맷. Figma Variables 동기화용 | 생성물 |

## 시안 원본 CSS 를 쓰지 않는 이유

Claude cowork 로 뽑은 원본에는 `greenpocket.css` 가 함께 있었지만 **저장소에 넣지 않았다.**

- 토큰을 참조한 곳이 색 130회뿐이고 radius·spacing·duration 은 0회였다. `px` 리터럴이 428개라 토큰을 바꿔도 컴포넌트가 따라오지 않는다.
- `.tabbar{position:absolute;bottom:14px}` 처럼 시안 아트보드(393×852 고정 래퍼) 좌표를 전제해 실제 브라우저에서 깨진다.
- `.card` `.row` `.tag` `.num` 같은 전역 클래스가 Tailwind 유틸리티와 충돌한다.
- 설명 시트용 클래스가 섞여 있어 204규칙 중 89개만 실제로 쓰였다.

**토큰 값만 `main.css` 로 옮겼고, 컴포넌트는 Tailwind 로 다시 만들어 `frontend/src/components/ui/` 에 두었다.**
따라서 `design-system.md` 6절이 설명하는 `greenpocket.css` 사용법은 더 이상 유효하지 않다.
그 문서에서 지금도 유효한 것은 **색 도출 근거·대비 검증표·간격/형태 규칙**이다.

## 이름이 바뀐 토큰 3개

Tailwind 클래스로 읽었을 때 어색하거나 겹쳐서 이름만 줄였다. 값은 같다.

| `design-system.md` | `main.css` | 이유 |
| --- | --- | --- |
| `--gp-color-text-strong` | `--color-ink` | `text-text-strong` 이 됨 |
| `--gp-color-text` | `--color-ink-soft` | `--text-body`(글자 크기)와 클래스명 충돌 |
| `--gp-color-bg` | `--color-canvas` | `bg-bg` 가 됨 |

## 남은 일

- Figma Variables 동기화 — MCP 쿼터 리셋(10/1) 이후. `tokens.json` 을 그대로 넣으면 된다.
- 아이콘 세트 미추출. 현재 `GpTabBar` 는 아이콘을 props 로 받는다.
- 다크 모드는 범위 밖이다.
