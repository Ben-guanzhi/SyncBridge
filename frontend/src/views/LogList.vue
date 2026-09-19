<template>
  <div class="page-container">
    <el-card shadow="never" class="search-bar">
      <el-form :inline="true">
        <el-form-item label="批次号">
          <el-input v-model="batchNo" placeholder="输入 UUID" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLogs" :loading="loading">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="logs" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="stage" label="阶段" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'info' : row.status === 1 ? 'success' : 'danger'" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="读取" width="80" align="right">
          <template #default="{ row }">{{ row.rowsRead }}</template>
        </el-table-column>
        <el-table-column label="过滤" width="80" align="right">
          <template #default="{ row }">{{ row.rowsFiltered }}</template>
        </el-table-column>
        <el-table-column label="写入" width="80" align="right">
          <template #default="{ row }">{{ row.rowsWritten }}</template>
        </el-table-column>
        <el-table-column prop="error_message" label="错误信息" min-width="200" show-overflow-tooltip />
        <el-table-column label="耗时" width="100" align="right">
          <template #default="{ row }">
            {{ row.durationMs ? row.durationMs + 'ms' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="start_time" label="开始时间" width="170" />
      </el-table>

      <el-empty v-if="!loading && logs.length === 0" description="暂无日志，请先执行同步任务" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import * as logApi from '@/api/log'

const route = useRoute()
const loading = ref(false)
const logs = ref([])
const batchNo = ref(route.query.batch || '')

function statusText(code) {
  return { 0: '进行中', 1: '成功', 2: '失败' }[code] || code
}

async function loadLogs() {
  if (!batchNo.value.trim()) { logs.value = []; return }
  loading.value = true
  try { logs.value = await logApi.getLogsByBatch(batchNo.value.trim()) }
  finally { loading.value = false }
}

onMounted(() => {
  if (route.query.batch) loadLogs()
})
</script>

<style scoped>
.page-container { max-width: 1400px; margin: 0 auto; }
.search-bar { margin-bottom: 16px; }
</style>
