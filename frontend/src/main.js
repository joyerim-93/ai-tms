import { createApp } from 'vue'
import './styles/theme.css'
import './styles/base.css'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import { setUnauthorizedHandler } from '@/api/http'
import { useAuthStore } from '@/stores/authStore'

const pinia = createPinia()
createApp(App).use(pinia).use(router).mount('#app')

// 세션 만료 등으로 401 을 받으면 로그인 화면으로 (로그인 후 원래 화면으로 복귀)
setUnauthorizedHandler(() => {
  useAuthStore(pinia).user = null
  if (router.currentRoute.value.path !== '/login') {
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
  }
})
