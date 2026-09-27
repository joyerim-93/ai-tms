import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import AppLayout from '@/layouts/AppLayout.vue'

const DefectForm = () => import('@/views/defect/DefectFormView.vue')

const routes = [
  { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { title: '로그인', public: true } },
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
      {
        path: 'test-cases/requirements',
        component: () => import('@/views/testcase/RequirementListView.vue'),
        meta: { title: '테스트케이스 저장소' },
      },
      {
        path: 'test-cases/:id(\\d+)',
        component: () => import('@/views/testcase/TestCaseDetailView.vue'),
        meta: { title: '테스트케이스 상세' },
      },
      {
        path: 'cycles',
        component: () => import('@/views/cycle/CycleListView.vue'),
        meta: { title: '테스트수행관리' },
      },
      {
        path: 'cycles/:id(\\d+)',
        component: () => import('@/views/cycle/CycleDetailView.vue'),
        meta: { title: '테스트 차수' },
      },
      {
        path: 'defects',
        component: () => import('@/views/defect/DefectListView.vue'),
        meta: { title: '이슈관리' },
      },
      { path: 'defects/new', component: DefectForm, meta: { title: '이슈 등록' } },
      {
        path: 'defects/:id(\\d+)',
        component: () => import('@/views/defect/DefectDetailView.vue'),
        meta: { title: '이슈 상세' },
      },
      { path: 'defects/:id(\\d+)/edit', component: DefectForm, meta: { title: '이슈 수정' } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 로그인 가드: 비로그인이면 /login (원래 가려던 경로는 ?redirect), 로그인 상태에서 /login 이면 홈
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.loaded) await auth.loadMe()
  if (!auth.user && !to.meta.public) return { path: '/login', query: { redirect: to.fullPath } }
  if (auth.user && to.path === '/login') return '/'
})

export default router
