/**
 * Phosphor 아이콘 1개를 프로젝트 규칙에 맞는 SFC로 뽑아 icons/ 에 저장한다.
 *
 *   npm run icon -- <PhosphorName> <IconName> [weight]
 *   npm run icon -- PhCaretLeft IconChevronLeft fill
 *
 * @phosphor-icons/vue 는 devDependency다. 컴포넌트를 그대로 import 하면
 * 아이콘 하나당 6가지 weight가 전부 번들에 실려 gzip 기준 약 0.9kB가 붙는다.
 * 여기서 필요한 weight의 path만 추출해 두면 런타임 의존성 없이 수백 바이트로 끝난다.
 */
import { mkdirSync, existsSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createSSRApp, h } from 'vue'
import { renderToString } from '@vue/server-renderer'
import prettier from 'prettier'
import * as Phosphor from '@phosphor-icons/vue'

const WEIGHTS = ['thin', 'light', 'regular', 'bold', 'fill', 'duotone']
const ICONS_DIR = resolve(dirname(fileURLToPath(import.meta.url)), '../src/components/ui/icons')

const [phosphorName, iconName, weight = 'fill'] = process.argv.slice(2)

function fail(message) {
  console.error(`✗ ${message}`)
  process.exit(1)
}

if (!phosphorName || !iconName) {
  fail('사용법: npm run icon -- <PhosphorName> <IconName> [weight]')
}
if (!Phosphor[phosphorName]) {
  fail(
    `@phosphor-icons/vue 에 ${phosphorName} 이 없다. https://phosphoricons.com 에서 이름을 확인한다.`,
  )
}
if (!/^Icon[A-Z][A-Za-z0-9]*$/.test(iconName)) {
  fail(`컴포넌트 이름은 Icon + 파스칼케이스여야 한다: ${iconName}`)
}
if (!WEIGHTS.includes(weight)) {
  fail(`weight는 ${WEIGHTS.join(' · ')} 중 하나여야 한다: ${weight}`)
}

const target = resolve(ICONS_DIR, `${iconName}.vue`)
if (existsSync(target)) {
  fail(`${iconName}.vue 가 이미 있다. 덮어쓰려면 먼저 지운다.`)
}

const markup = await renderToString(
  createSSRApp({
    render: () => h(Phosphor[phosphorName], { weight, size: 24 }),
  }),
)

// SSR 산출물에서 껍데기를 걷어낸다.
//   <svg ...><!--[--><!--]--><g><path .../></g></svg>
const inner = markup
  .replace(/^<svg[^>]*>/, '')
  .replace(/<\/svg>$/, '')
  .replace(/<!--[[\]]-->/g, '')
  .replace(/^\s*<g>/, '')
  .replace(/<\/g>\s*$/, '')
  .trim()

if (!inner) {
  fail(`${phosphorName}(${weight}) 렌더 결과가 비어 있다.`)
}

const shapes = inner
  .replace(/><\/(path|circle|rect|line|polyline|polygon|ellipse)>/g, ' />')
  .replace(/\/></g, '/>\n<')
  .split('\n')
  .map((line) => `    ${line.trim()}`)
  .join('\n')

const sfc = `<script setup>
/* Phosphor ${phosphorName} (${weight}) — scripts/add-icon.mjs 로 추출 */
defineProps({ size: { type: [Number, String], default: 24 } })
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 256 256"
    fill="currentColor"
    aria-hidden="true"
    focusable="false"
  >
${shapes}
  </svg>
</template>
`

const formatted = await prettier.format(sfc, {
  ...(await prettier.resolveConfig(target)),
  parser: 'vue',
})

mkdirSync(ICONS_DIR, { recursive: true })
writeFileSync(target, formatted, 'utf8')

console.log(`✓ ${iconName}.vue  (${phosphorName} / ${weight}, ${formatted.length}B)`)
console.log(`  import ${iconName} from '@/components/ui/icons/${iconName}.vue'`)
