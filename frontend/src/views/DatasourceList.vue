<template>
  <div class="page-container">
    <!-- 操作栏 -->
    <el-card shadow="never" class="toolbar">
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon> 新增数据源
      </el-button>
      <el-button @click="loadList">
        <el-icon><Refresh /></el-icon> 刷新
      </el-button>
    </el-card>

    <!-- 数据源列表 -->
    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeTagClass(row.type)" size="small">
              {{ row.type === 1 ? '数据库' : row.type === 2 ? 'HTTP API' : row.type === 3 ? 'Excel' : 'SQL文件' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="地址" min-width="200">
          <template #default="{ row }">
            <template v-if="row.type === 1">
              {{ row.host }}:{{ row.port }}/{{ row.databaseName }}
            </template>
            <template v-else-if="row.type === 2">
              {{ row.baseUrl }}
            </template>
            <template v-else>
              {{ row.filePath || '内联SQL' }}
            </template>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="testConn(row)">测试</el-button>
            <el-popconfirm title="确认删除？" @confirm="deleteItem(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑数据源' : '新增数据源'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：OMS_主库" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">数据库</el-radio>
            <el-radio :value="2">HTTP API</el-radio>
            <el-radio :value="3">Excel文件</el-radio>
            <el-radio :value="4">SQL文件</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- DB 字段 -->
        <template v-if="form.type === 1">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="主机" prop="host">
                <el-input v-model="form.host" placeholder="127.0.0.1" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="端口" prop="port">
                <el-input-number v-model="form.port" :min="1" :max="65535" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="数据库名" prop="databaseName">
            <el-input v-model="form.databaseName" />
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="form.username" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="密码" prop="password">
                <el-input v-model="form.password" type="password" show-password />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="最大连接">
                <el-input-number v-model="form.maxConnections" :min="1" :max="50" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="超时(ms)" prop="timeoutMs">
                <el-input-number v-model="form.timeoutMs" :min="1000" :step="1000" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <!-- API 字段 -->
        <template v-else-if="form.type === 2">
          <el-form-item label="Base URL" prop="baseUrl">
            <el-input v-model="form.baseUrl" placeholder="https://api.example.com" />
          </el-form-item>
          <el-form-item label="认证方式">
            <el-select v-model="form.authMethod" placeholder="请选择">
              <el-option label="无" value="" />
              <el-option label="Bearer Token" value="bearer" />
              <el-option label="Basic Auth" value="basic" />
              <el-option label="API Key" value="apikey" />
            </el-select>
          </el-form-item>
          <el-form-item label="Token / Key" v-if="form.authMethod && form.authMethod !== 'basic'">
            <el-input v-model="form.authToken" type="password" show-password />
          </el-form-item>
          <el-row v-if="form.authMethod === 'basic'" :gutter="16">
            <el-col :span="12">
              <el-form-item label="用户名">
                <el-input v-model="form.username" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="密码">
                <el-input v-model="form.password" type="password" show-password />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <!-- Excel 文件 字段 -->
        <template v-else-if="form.type === 3">
          <el-form-item label="文件路径" prop="filePath">
            <el-input v-model="form.filePath" placeholder="D:/data/sync_source.xlsx" />
          </el-form-item>
          <el-form-item label="工作表" prop="sheetName">
            <el-input v-model="form.sheetName" placeholder="Sheet1（可选，留空用第一个工作表）" />
          </el-form-item>
        </template>

        <!-- SQL 文件 字段 -->
        <template v-else-if="form.type === 4">
          <el-form-item label="文件路径" prop="filePath">
            <el-input v-model="form.filePath" placeholder="D:/data/init_data.sql" />
          </el-form-item>
          <el-form-item label="或内联 SQL">
            <el-input v-model="form.sqlContent" type="textarea" :rows="6" placeholder="可直接粘贴SQL内容（INSERT语句）" />
          </el-form-item>
        </template>

        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import * as dsApi from '@/api/datasource'

const loading = ref(false)
const list = ref([])

// Dialog
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const submitting = ref(false)
const form = ref({
  name: '', type: 1, host: '', port: 3306, databaseName: '',
  username: '', password: '', maxConnections: 10, timeoutMs: 10000,
  enabled: true, baseUrl: '', authMethod: '', authToken: '',
  filePath: '', sheetName: '', sqlContent: ''
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

async function loadList() {
  loading.value = true
  try { list.value = await dsApi.listDatasources() } finally { loading.value = false }
}

function openDialog(row) {
  if (row) {
    isEdit.value = true
    form.value = { ...row }
  } else {
    isEdit.value = false
    form.value = { type: 1, maxConnections: 10, timeoutMs: 10000, enabled: true }
  }
  dialogVisible.value = true
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = { ...form.value }
    // 隐藏密码字段避免回显
    if (!payload.password) delete payload.password
    if (isEdit.value) {
      await dsApi.updateDatasource(payload.id, payload)
      ElMessage.success('更新成功')
    } else {
      await dsApi.createDatasource(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadList()
  } finally { submitting.value = false }
}

async function testConn(row) {
  try {
    const res = await dsApi.testDatasourceConnection(row.id)
    ElMessage.success(res || '连接成功')
  } catch (e) {
    // error already shown by interceptor
  }
}

async function deleteItem(id) {
  await dsApi.deleteDatasource(id)
  ElMessage.success('删除成功')
  loadList()
}

function typeTagClass(type) {
  const map = { 1: '', 2: 'success', 3: 'warning', 4: 'info' }
  return map[type] || ''
}

onMounted(loadList)
</script>

<style scoped>
.page-container { padding-bottom: 20px; }
.toolbar { margin-bottom: 16px; }
</style>
