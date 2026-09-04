import { createRouter, createWebHistory } from 'vue-router'

import onboarding from './routes/onboarding'
import analysis from './routes/analysis'
import greenlife from './routes/greenlife'
import eco from './routes/eco'
import pocket from './routes/pocket'
import mypage from './routes/mypage'

/*
 * 라우트는 도메인별 파일로 나눠 둔다.
 * 네 명이 동시에 화면을 붙이므로 이 파일을 함께 고치면 매번 충돌한다.
 * 화면을 추가할 때는 이 파일이 아니라 routes/<도메인>.js 를 고친다.
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [...onboarding, ...analysis, ...greenlife, ...eco, ...pocket, ...mypage],
  scrollBehavior() {
    return { top: 0 }
  },
})

export default router
