/*
 * main.css 의 @theme static 블록 → docs/design/tokens.json (W3C Design Tokens 포맷)
 *
 *   node scripts/tokens-json.mjs
 *
 * 정본은 main.css 다. tokens.json 은 Figma Variables · Style Dictionary 임포트용 생성물이라
 * 직접 고치지 않는다. 토큰을 추가·수정했으면 이 스크립트를 다시 돌린다.
 *
 * 매핑 규칙
 *   --gp-<green|neutral|amber|orange|red|elec|gas|water>-<step>  → primitive.<family>.<step>
 *   --color-chart-*   → semantic.chart.*      --color-*  → semantic.color.*   (var(--gp-x-y) 는 {primitive.x.y} 참조로)
 *   --text-<n>(+ --line-height · --font-weight)  → semantic.fontSize.<n> + semantic.typography.<n>
 *   --font-* fontFamily · --leading-* lineHeight · --tracking-* letterSpacing · --radius-* radius
 *   --shadow-* shadow · --ease-* easing · --gp-duration-* duration · --gp-z-* zIndex · --gp-grad-* gradient
 *   나머지 --gp-*  → component.*  (치수)
 *   fontWeight · space 는 main.css 에 토큰이 없어(Tailwind 기본 스케일) 여기서 고정값으로 붙인다.
 */
import { readFileSync, writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const here = dirname(fileURLToPath(import.meta.url))
const SRC = resolve(here, '../src/assets/main.css')
const OUT = resolve(here, '../../docs/design/tokens.json')
const PRIM = ['green', 'neutral', 'amber', 'orange', 'red', 'elec', 'gas', 'water']

const css = readFileSync(SRC, 'utf8')
const body = css.match(/@theme static \{([\s\S]*?)\n\}/)[1]
const decls = []
for (const m of body.matchAll(
  /(--[a-z0-9-]+)\s*:\s*([\s\S]*?);[ \t]*(?:\/\*\s*([\s\S]*?)\s*\*\/)?/g,
)) {
  decls.push({ name: m[1], val: m[2].split(/\s+/).join(' '), desc: m[3]?.split(/\s+/).join(' ') })
}

const ref = (v) => {
  const r = v.match(/^var\(--gp-([a-z]+)-([a-z0-9]+)\)$/)
  return r && PRIM.includes(r[1]) ? `{primitive.${r[1]}.${r[2]}}` : v
}
const tok = (type, value, desc) =>
  desc ? { $type: type, $value: value, $description: desc } : { $type: type, $value: value }
const put = (root, path, node) => {
  let d = root
  for (const k of path.slice(0, -1)) d = d[k] ??= {}
  d[path.at(-1)] = node
}

const primitive = {}
const semantic = {}
const component = {}
const typo = {}
for (const { name, val, desc } of decls) {
  const n = name.slice(2)
  let r
  if ((r = n.match(new RegExp(`^gp-(${PRIM.join('|')})-([a-z0-9]+)$`))))
    put(primitive, [r[1], r[2]], tok('color', val.toUpperCase(), desc))
  else if (n.startsWith('color-chart-'))
    put(semantic, ['chart', n.slice(12)], tok('color', ref(val), desc))
  else if (n.startsWith('color-'))
    put(semantic, ['color', n.slice(6)], tok('color', ref(val), desc))
  else if (n.startsWith('font-'))
    put(semantic, ['fontFamily', n.slice(5)], tok('fontFamily', val, desc))
  else if ((r = n.match(/^text-([a-z-]+?)(?:--(line-height|font-weight))?$/))) {
    const t = (typo[r[1]] ??= {})
    if (r[2] === 'line-height') t.lh = Number(val)
    else if (r[2] === 'font-weight') t.w = Number(val)
    else Object.assign(t, { size: val, desc })
  } else if (n.startsWith('leading-'))
    put(semantic, ['lineHeight', n.slice(8)], tok('number', Number(val), desc))
  else if (n.startsWith('tracking-'))
    put(semantic, ['letterSpacing', n.slice(9)], tok('dimension', val, desc))
  else if (n.startsWith('radius-'))
    put(semantic, ['radius', n.slice(7)], tok('dimension', val, desc))
  else if (n.startsWith('shadow-')) put(semantic, ['shadow', n.slice(7)], tok('shadow', val, desc))
  else if (n.startsWith('ease-'))
    put(semantic, ['easing', n.slice(5)], tok('cubicBezier', val, desc))
  else if (n.startsWith('gp-duration-'))
    put(semantic, ['duration', n.slice(12)], tok('duration', val, desc))
  else if (n.startsWith('gp-z-'))
    put(semantic, ['zIndex', n.slice(5)], tok('number', Number(val), desc))
  else if (n.startsWith('gp-grad-'))
    put(semantic, ['gradient', n.slice(8)], tok('gradient', val, desc))
  else if (n.startsWith('gp-')) component[n.slice(3)] = tok('dimension', val, desc)
  else throw new Error(`unmapped token: ${name}`)
}
for (const [k, t] of Object.entries(typo)) {
  put(semantic, ['fontSize', k], tok('dimension', t.size, t.desc))
  put(
    semantic,
    ['typography', k],
    tok(
      'typography',
      {
        fontFamily: '{semantic.fontFamily.sans}',
        fontWeight: t.w,
        fontSize: t.size,
        lineHeight: t.lh,
      },
      t.desc,
    ),
  )
}
semantic.fontWeight = Object.fromEntries(
  [
    ['regular', 400],
    ['medium', 500],
    ['semibold', 600],
    ['bold', 700],
    ['extrabold', 800],
    ['black', 900],
  ].map(([k, w]) => [k, tok('fontWeight', w)]),
)
semantic.space = Object.fromEntries(
  Array.from({ length: 11 }, (_, i) => [String(i), tok('dimension', `${i * 4}px`)]),
)
semantic.space.$description =
  'Tailwind 기본 4px 스케일을 그대로 쓴다 (p-1=4 · p-3=12 · p-4=16 · p-5=20). main.css 에 별도 토큰이 없다'

const out = {
  $schema: 'https://tr.designtokens.org/format/',
  $description:
    "그린포켓 디자인 토큰 v3 · 팀 돈워리비그린 · 2026 KB IT's Your Life 해커톤. frontend/src/assets/main.css 의 @theme static 이 정본이고 이 파일은 그것에서 생성됨 (docs/design/README.md 참고).",
  primitive,
  semantic,
  component,
}
writeFileSync(OUT, JSON.stringify(out, null, 2) + '\n')
const count = (o) =>
  o && typeof o === 'object'
    ? '$value' in o
      ? 1
      : Object.entries(o)
          .filter(([k]) => !k.startsWith('$'))
          .reduce((s, [, v]) => s + count(v), 0)
    : 0
console.log(
  `tokens.json ← main.css · primitive ${count(primitive)} · semantic ${count(semantic)} · component ${count(component)}`,
)
