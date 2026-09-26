import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'

const Placeholder = () => import('@/views/PlaceholderView.vue')
const TestCaseForm = () => import('@/views/testcase/TestCaseFormView.vue')

const routes = [
  {
    path: '/',
    component: AppLayout,
    children: [
      { path: '', component: () => import('@/views/DashboardView.vue'), meta: { title: '대시보드' } },
      {
        path: 'test-cases',
        component: () => import('@/views/testcase/TestCaseListView.vue'),
        meta: { title: '테스트케이스 저장소' },
      },
      { path: 'test-cases/new', component: TestCaseForm, meta: { title: '테스트케이스 등록' } },
      {
        path: 'test-cases/:id(\\d+)',
        component: () => import('@/views/testcase/TestCaseDetailView.vue'),
        meta: { title: '테스트케이스 상세' },
      },
      { path: 'test-cases/:id(\\d+)/edit', component: TestCaseForm, meta: { title: '테스트케이스 수정' } },
      { path: 'cycles', component: Placeholder, meta: { title: '테스트수행관리' } },
      { path: 'defects', component: Placeholder, meta: { title: '결함관리' } },
    ],
  },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
