import http from '@/utils/http'

// 数据源列表
export const listDatasources = () => http.get('/datasources')

// 获取单个数据源
export const getDatasource = (id) => http.get(`/datasources/${id}`)

// 创建数据源
export const createDatasource = (data) => http.post('/datasources', data)

// 更新数据源
export const updateDatasource = (id, data) => http.put(`/datasources/${id}`, data)

// 删除数据源
export const deleteDatasource = (id) => http.delete(`/datasources/${id}`)

// 测试连接
export const testDatasourceConnection = (id) => http.post(`/datasources/${id}/test`)
