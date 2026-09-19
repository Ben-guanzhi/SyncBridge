import http from '@/utils/http'

// 任务列表
export const listJobs = () => http.get('/jobs')

// 获取单个任务（含配置 + 数据源）
export const getJob = (id) => http.get(`/jobs/${id}`)

// 创建任务
export const createJob = (data) => http.post('/jobs', data)

// 更新任务
export const updateJob = (id, data) => http.put(`/jobs/${id}`, data)

// 删除任务
export const deleteJob = (id) => http.delete(`/jobs/${id}`)

// 手动执行
export const executeJob = (id) => http.post(`/jobs/${id}/execute`)

// 执行全部定时任务
export const executeAllJobs = () => http.post('/jobs/execute-all')
