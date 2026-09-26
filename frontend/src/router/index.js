import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'

const DefectForm = () => import('@/views/defect/DefectFormView.vue')
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
      {
        path: 'test-cases/requirements',
        component: () => import('@/views/testcase/RequirementListView.vue'),
        meta: { title: '테스트케이스 저장소' },
      },
      { path: 'test-cases/new', component: TestCaseForm, meta: { title: '테스트케이스 등록' } },
      {
        path: 'test-cases/:id(\\d+)',
        component: () => import('@/views/testcase/TestCaseDetailView.vue'),
        meta: { title: '테스트케이스 상세' },
      },
      { path: 'test-cases/:id(\\d+)/edit', component: TestCaseForm, meta: { title: '테스트케이스 수정' } },
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

export default createRouter({
  history: createWebHistory(),
  routes,
})
