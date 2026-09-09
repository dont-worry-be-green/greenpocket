import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import { setUnauthorizedHandler } from './api/client'
import router from './router'
import { useAuthStore } from './stores/auth'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)

setUnauthorizedHandler(async () => {
  useAuthStore(pinia).clearSession()
  if (!router.currentRoute.value.path.startsWith('/onboarding')) {
    await router.replace('/onboarding/login')
  }
})

app.use(router)

app.mount('#app')

if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch((error) => {
      console.warn('서비스 워커 등록에 실패했습니다.', error)
    })
  })
}
