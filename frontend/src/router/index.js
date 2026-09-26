import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'

const Placeholder = () => import('@/views/PlaceholderView.vue')

const routes = [
  {
    path: '/',
    component: AppLayout,
    children: [
      { path: '', component: () => import('@/views/DashboardView.vue'), meta: { title: '대시보드' } },
      { path: 'test-cases', component: Placeholder, meta: { title: '테스트케이스 저장소' } },
      { path: 'cycles', component: Placeholder, meta: { title: '테스트수행관리' } },
      { path: 'defects', component: Placeholder, meta: { title: '결함관리' } },
    ],
  },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
