import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/components/Layout.vue'

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/datasources',
    children: [
      {
        path: 'datasources',
        name: 'Datasources',
        component: () => import('@/views/DatasourceList.vue'),
        meta: { title: '数据源管理' }
      },
      {
        path: 'jobs',
        name: 'Jobs',
        component: () => import('@/views/JobList.vue'),
        meta: { title: '同步任务' }
      },
      {
        path: 'job/:id/edit',
        name: 'JobEdit',
        component: () => import('@/views/JobEdit.vue'),
        meta: { title: '编辑任务' }
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('@/views/LogList.vue'),
        meta: { title: '执行日志' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
