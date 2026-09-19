<template>
  <div class="page-container">
    <el-card shadow="never" class="toolbar">
      <el-button type="primary" @click="$router.push('/jobs/edit')">
        <el-icon><Plus /></el-icon> 新建任务
      </el-button>
      <el-button type="success" :loading="executingAll" @click="executeAll">
        <el-icon><VideoPlay /></el-icon> 执行全部定时任务
      </el-button>
      <el-button @click="loadList">
        <el-icon><Refresh /></el-icon> 刷新
      </el-button>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px;">
      <el-col :span="6">
        <el-statistic title="总任务数" :value="total" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="已启用" :value="enabledCount" value-style="{ color: '#67C23A' }" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="本次成功" :value="lastSuccess" value-style="{ color: '#409EFF' }" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="本次失败" :value="lastFailed" value-style="{ color: '#F56C6C' }" />
      </el-col>
    </el-row>

    <!-- 任务列表 -->
    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="任务名称" min-width="200" />
        <el-table-column label="同步模式" width="180">
          <template #default="{ row }">
            <el-tag size="small">{{ syncModeLabel(row.syncMode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="源 → 目标" min-width="300">
          <template #default="{ row }">
            <span>{{ sourceName(row.sourceDsId) }} → {{ targetName(row.targetDsId) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="incrementalKey" label="增量字段" width="120" />
        <el-table-column label="调度" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.scheduleCron" size="small" type="info">{{ row.scheduleCron }}</el-tag>
            <span v-else style="color:#909399">手动</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0"
                       @change="onStatusChange(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="editJob(row)">编辑</el-button>
            <el-button link type="success" size="small" @click="runOnce(row)" :loading="row._running">
              运行一次
            </el-button>
            <el-button link type="warning" size="small" @click="$router.push(`/logs?batch=${row.lastBatch}`)">日志</el-button>
            <el-popconfirm title="确认删除？" @confirm="doDelete(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 执行结果弹窗 -->
    <el-dialog v-model="resultDialogVisible" :title="'执行结果 - ' + resultData.jobName" width="800px" destroy-on-close>
      <el-descriptions :column="2" border size="small" style="margin-bottom: 16px;">
        <el-descriptions-item label="批次号">{{ resultData.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(resultData.status)">{{ resultData.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ resultData.durationMs }} ms</el-descriptions-item>
        <el-descriptions-item label="成功段数">{{ resultData.successCount }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="resultData.segmentResults || []" border size="small">
        <el-table-column prop="configId" label="配置ID" width="80" />
        <el-table-column prop="status" label="状态" width="150">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
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
        <el-table-column label="位点更新" min-width="150">
          <template #default="{ row }">
            {{ row.lastSyncValueUpdated || '-' }}
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="resultDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, VideoPlay } from '@element-plus/icons-vue'
import * as jobApi from '@/api/job'
import * as dsApi from '@/api/datasource'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const executingAll = ref(false)
const allDatasources = ref([])

// 运行结果弹窗
const resultDialogVisible = ref(false)
const resultData = ref({})

// 统计数据
const total = computed(() => list.value.length)
const enabledCount = computed(() => list.value.filter(j => j.status === 1).length)
const lastSuccess = computed(() => resultData.value.successCount || 0)
const lastFailed = computed(() => resultData.value.failureCount || 0)

const syncModeMap = { 1: 'DB→DB', 2: 'DB→API', 3: 'API→DB', 4: 'API→API' }

async function loadList() {
  loading.value = true
  try {
    const [jobs, dss] = await Promise.all([
      jobApi.listJobs(),
      dsApi.listDatasources()
    ])
    list.value = jobs
    allDatasources.value = dss
  } finally { loading.value = false }
}

function syncModeLabel(code) { return syncModeMap[code] || code }

function sourceName(dsId) {
  const ds = allDatasources.value.find(d => d.id === dsId)
  return ds ? ds.name : `#${dsId}`
}
function targetName(dsId) {
  const ds = allDatasources.value.find(d => d.id === dsId)
  return ds ? ds.name : `#${dsId}`
}

function statusTagType(status) {
  return { SUCCESS: '', FAILED: 'danger', PARTIAL_SUCCESS: 'warning' }[status] || ''
}

function editJob(row) {
  router.push(`/jobs/edit/${row.id}`)
}

async function runOnce(row) {
  row._running = true
  try {
    const res = await jobApi.executeJob(row.id)
    resultData.value = res
    resultDialogVisible.value = true
    if (res.segmentResults?.length) {
      // 把 batchNo 附加到 task 上用于日志跳转
      row.lastBatch = res.batchNo
    }
  } catch (e) { /* shown by interceptor */ } finally { row._running = false }
}

async function executeAll() {
  executingAll.value = true
  try {
    await jobApi.executeAllJobs()
    ElMessage.success('全部定时任务已触发')
  } finally { executingAll.value = false }
}

async function onStatusChange(row) {
  try { await jobApi.updateJob(row.id, { status: row.status }) }
  catch { row.status = row.status === 1 ? 0 : 1 }
}

async function doDelete(id) {
  await jobApi.deleteJob(id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.page-container { padding-bottom: 20px; }
.toolbar { margin-bottom: 16px; display: flex; gap: 8px; }
</style>
