/*
 * 녹색생활실천 API 계약 가드.
 *
 * 이 도메인은 **픽스처가 없다.** 백엔드 4개가 이미 구현돼 있고 DTO 가 api-spec 12절과 1:1이라
 * 응답을 한 벌 더 베낄 이유가 없다(`api/greenlife.js` 첫머리 주석).
 *
 * 그 결정이 조용히 뒤집히는 자리가 둘 있다 — 서버가 꺼져 화면이 에러로 뜰 때 급히 shim 을
 * 넣거나, 명세에 없는 엔드포인트를 하나 더 부르는 것이다. 여기서 둘 다 못 박는다.
 */

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import process from 'node:process'

import { describe, expect, it } from 'vitest'

import * as greenlifeApi from '../greenlife'

const source = () => readFileSync(resolve(process.cwd(), 'src/api/greenlife.js'), 'utf8')

describe('api/greenlife.js — 엔드포인트 함수', () => {
  /*
   * 12.5 `POST /greenlife/settlements` 는 일부러 없다. 월 지급분을 포켓 거래로 만드는
   * 시스템 API 라 15.3 화면 매핑표(BN-01~03)에 없고 화면이 부르지 않는다.
   */
  it('BN 세 화면이 쓰는 4개만 있다 — 12.5 정산은 화면이 부르지 않는다', () => {
    expect(Object.keys(greenlifeApi).sort()).toEqual([
      'getGreenlifeItemDetail',
      'getGreenlifeItems',
      'getGreenlifeStatus',
      'linkGreenlife',
    ])
  })

  it('api-spec.md 12절 경로를 그대로 쓴다', () => {
    const text = source()
    expect(text).toContain("client.get('/greenlife/status'")
    expect(text).toContain("client.post('/greenlife/link')")
    expect(text).toContain("client.get('/greenlife/items'")
    expect(text).toContain('client.get(`/greenlife/items/${itemId}`')
  })
})

describe('픽스처를 두지 않는다', () => {
  // 주석은 왜 없는지를 설명하므로 이름이 나온다. 선언을 본다 (eco·onboarding 은 1개를 못 박는다)
  it('USE_FIXTURES 스위치를 선언하지 않는다 — 실제 API 하나만 본다', () => {
    expect(source()).not.toMatch(/const\s+USE_FIXTURES/)
  })

  it('@/fixtures 를 import 하지 않는다', () => {
    expect(source()).not.toMatch(/from\s+['"]@\/fixtures/)
  })
})
