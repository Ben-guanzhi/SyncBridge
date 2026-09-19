<template>
  <div class="page-container" v-loading="pageLoading">
    <el-page-header @back="goBack" :title="'任务: ' + (formData.name || '新建')" style="margin-bottom: 16px;" />

    <el-form :model="formData" label-width="120px" size="default">
      <!-- 基本信息 -->
      <el-card shadow="never">
        <template #header>基本信息</template>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="任务名称" required>
              <el-input v-model="formData.name" placeholder="如：PUR-REQ-Sync-to-Kingdee" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="描述">
              <el-input v-model="formData.description" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="源数据源" required>
              <el-select v-model="formData.sourceDsId" filterable placeholder="选择源数据源">
                <el-option v-for="d in dbSources" :key="d.id" :label="d.name" :value="d.id" />
                <el-option v-for="d in apiSources" :key="d.id" :label="d.name + ' (API)''" :value="d.id" />
                <el-option v-for="d in excelSources" :key="d.id" :label="d.name + ' (Excel)''" :value="d.id" />
                <el-option v-for="d in sqlSources" :key="d.id" :label="d.name + ' (SQL)''" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="目标数据源" required>
              <el-select v-model="formData.targetDsId" filterable placeholder="选择目标数据源">
                <el-option v-for="d in dbTargets" :key="d.id" :label="d.name" :value="d.id" />
                <el-option v-for="d in apiTargets" :key="d.id" :label="d.name + ' (API)''" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="同步模式">
              <el-tag>{{ syncModeLabel }}</el-tag>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="增量字段">
              <el-input v-model="formData.incrementalKey" placeholder="如：updated_at / FReqDate" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Cron 调度">
              <el-input v-model="formData.scheduleCron" placeholder="留空=手动触发" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="启用状态">
              <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 配置段列表 -->
      <el-card shadow="never" style="margin-top: 16px;">
        <template #header>
          <span>同步配置段</span>
          <el-button type="primary" size="small" @click="addConfig" style="float:right">
            <el-icon><Plus /></el-icon> 新增配置段
          </el-button>
        </template>

        <el-table :data="formData.configs" border size="small">
          <el-table-column type="index" label="#" width="40" align="center" />
          <el-table-column label="执行顺序" width="90">
            <template #default="{ row, $index }">
              <el-input-number v-model="row.itemOrder" :min="0" :max="99" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="是否启用" width="70">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" :active-value="true" :inactive-value="false" size="small" />
            </template>
          </el-table-column>

          <!-- 源侧 -->
          <el-table-column label="源表/API路径" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.sourceTable" size="small" placeholder="表名或API路径" />
            </template>
          </el-table-column>
          <el-table-column label="行过滤(Aviator)" min-width="200">
            <template #default="{ row }">
              <el-input v-model="row.sourceCondition" size="small" placeholder="Aviator:$status == 'OK'" />
            </template>
          </el-table-column>
          <el-table-column label="分页大小" width="90">
            <template #default="{ row }">
              <el-input-number v-model="row.pageSize" :min="1" :max="10000" size="small" />
            </template>
          </el-table-column>

          <!-- 目标侧 -->
          <el-table-column label="目标表/API" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.targetTable" size="small" placeholder="目标表或API路径" />
            </template>
          </el-table-column>
          <el-table-column label="写入模式" width="110">
            <template #default="{ row }">
              <el-select v-model="row.targetWriteMode" size="small">
                <el-option :value="1" label="INSERT" />
                <el-option :value="2" label="UPSERT" />
                <el-option :value="3" label="UPDATE" />
                <el-option :value="4" label="DELETE" />
              </el-select>
            </template>
          </el-table-column>

          <!-- 主明细标记 -->
          <el-table-column label="主表" width="70" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.isMaster" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="嵌套JSON路径" width="140">
            <template #default="{ row }">
              <el-input v-model="row.nestedJsonPath" size="small" placeholder="Entries" />
            </template>
          </el-table-column>

          <!-- 操作 -->
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ $index }">
              <el-button link type="primary" size="small" @click="openMappingDialog(formData.configs[$index])">
                映射
              </el-button>
              <el-button link type="danger" size="small" @click="removeConfig($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 底部操作栏 -->
      <div style="text-align: center; padding: 24px 0;">
        <el-button type="primary" :loading="submitting" @click="onSave" size="large">保存任务</el-button>
        <el-button @click="goBack" size="large">取消</el-button>
      </div>
    </el-form>

    <!-- 字段映射对话框 -->
    <el-dialog v-model="mappingDialogVisible" title="字段映射配置" width="750px" destroy-on-close>
      <p style="color:#909399;font-size:12px;margin-bottom:12px">
        定义源字段 → 目标字段的映射关系。可以在 "转换表达式" 列填写 Aviator 表达式进行数据转换，如: <code>$amount * 1.13</code>
      </p>
      <div v-if="currentConfig" style="max-height: 500px; overflow-y: auto;">
        <el-table :data="currentConfig.fieldMappings || []" border size="small">
          <el-table-column label="源字段" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.source_field" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="目标字段" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.target_field" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="转换表达式" min-width="200">
            <template #default="{ row }">
              <el-input v-model="row.transform" size="small" placeholder="留空=直接赋值" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="60">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="currentConfig.fieldMappings.splice($index, 1)">×</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button type="primary" plain size="small" @click="addMappingRow" style="margin-top: 8px">
          <el-icon><Plus /></el-icon> 添加映射
        </el-button>
      </div>
      <template #footer>
        <el-button @click="mappingDialogVisible = false">完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import * as jobApi from '@/api/job'
import * as dsApi from '@/api/datasource'

const route = useRoute()
const router = useRouter()
const pageLoading = ref(false)
const submitting = ref(false)
const allDatasources = ref([])

const syncModeMap = { 1: 'DB→DB', 2: 'DB→API', 3: 'API→DB', 4: 'API→API', 5: '文件→DB', 6: '文件→API', 7: 'DB→文件', 8: '文件→文件' }

const formData = ref({
  name: '', description: '', sourceDsId: null, targetDsId: null,
  syncMode: 2, scheduleCron: '', incrementalKey: '', status: 1,
  configs: []
})

const mappingDialogVisible = ref(false)
const currentConfig = ref(null)

// 按类型分源数据源
const dbSources = computed(() => allDatasources.value.filter(d => d.type === 1))
const apiSources = computed(() => allDatasources.value.filter(d => d.type === 2))
const excelSources = computed(() => allDatasources.value.filter(d => d.type === 3))
const sqlSources = computed(() => allDatasources.value.filter(d => d.type === 4))
const dbTargets = computed(() => allDatasources.value.filter(d => d.type === 1))
const apiTargets = computed(() => allDatasources.value.filter(d => d.type === 2))

const syncModeLabel = computed(() => {
  const s = formData.value.sourceDsId
  const t = formData.value.targetDsId
  if (!s || !t) return '-'
  const sourceIsApi = allDatasources.value.find(d => d.id === s)?.type === 2
  const sourceIsFile = allDatasources.value.find(d => d.id === s)?.type >= 3
  const targetIsApi = allDatasources.value.find(d => d.id === t)?.type === 2
  const targetIsFile = allDatasources.value.find(d => d.id === t)?.type >= 3
  if (sourceIsFile && targetIsFile) return '文件→文件'
  if (sourceIsFile) return '文件→DB'
  if (targetIsFile) return 'DB→文件'
  if (sourceIsApi && targetIsApi) return 'API→API'
  if (sourceIsApi) return 'API→DB'
  if (targetIsApi) return 'DB→API'
  return 'DB→DB'
})

async function loadDatasources() {
  allDatasources.value = await dsApi.listDatasources()
}

async function loadJob() {
  const id = route.params.id
  if (!id) return // 新建
  pageLoading.value = true
  try {
    const job = await jobApi.getJob(id)
    formData.value = {
      ...job,
      configs: job.configs || []
    }
  } finally { pageLoading.value = false }
}

function addConfig() {
  formData.value.configs.push({
    itemOrder: formData.value.configs.length,
    enabled: true,
    isMaster: false,
    pageSize: 100,
    fieldMappings: [],
    masterDetailRelations: []
  })
}

function removeConfig(index) {
  formData.value.configs.splice(index, 1)
  // 重排顺序
  formData.value.configs.forEach((c, i) => c.itemOrder = i)
}

function openMappingDialog(config) {
  currentConfig.value = config
  mappingDialogVisible.value = true
}

function addMappingRow() {
  currentConfig.value.fieldMappings.push({
    source_field: '',
    target_field: '',
    transform: ''
  })
}

async function onSave() {
  if (!formData.value.name) { ElMessage.warning('请填写任务名称'); return }
  if (!formData.value.sourceDsId || !formData.value.targetDsId) {
    ElMessage.warning('请选择源和目标数据源')
    return
  }

  submitting.value = true
  try {
    // 计算同步模式
    const s = allDatasources.value.find(d => d.id === formData.value.sourceDsId)
    const t = allDatasources.value.find(d => d.id === formData.value.targetDsId)
    const sourceIsApi = s?.type === 2
    const targetIsApi = t?.type === 2
    const sourceIsFile = s?.type >= 3
    const targetIsFile = t?.type >= 3
    if (sourceIsFile && targetIsFile) formData.value.syncMode = 8
    else if (sourceIsFile) formData.value.syncMode = 5
    else if (targetIsFile) formData.value.syncMode = 7
    else if (sourceIsApi && targetIsApi) formData.value.syncMode = 4
    else if (sourceIsApi) formData.value.syncMode = 3
    else if (targetIsApi) formData.value.syncMode = 2
    else formData.value.syncMode = 1

    const payload = JSON.parse(JSON.stringify(formData.value))

    if (route.params.id) {
      payload.id = parseInt(route.params.id)
      await jobApi.updateJob(payload.id, payload)
      ElMessage.success('更新成功')
    } else {
      await jobApi.createJob(payload)
      ElMessage.success('创建成功')
    }
    goBack()
  } catch (e) { /* shown by interceptor */ } finally { submitting.value = false }
}

function goBack() { router.push('/jobs') }

onMounted(async () => {
  await Promise.all([loadJob(), loadDatasources()])
})
</script>

<style scoped>
.page-container { max-width: 1200px; margin: 0 auto; }
</style>
