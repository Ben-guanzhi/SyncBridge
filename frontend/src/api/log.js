import http from '@/utils/http'

// 按批次号查日志
export const getLogsByBatch = (batchNo) => http.get(`/logs/batch/${batchNo}`)
