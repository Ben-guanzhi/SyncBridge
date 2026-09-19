<template>
  <el-container class="app-layout">
    <!-- Sidebar -->
    <el-aside :width="store.sidebarCollapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <el-icon size="24"><DataLine /></el-icon>
        <span v-show="!store.sidebarCollapsed">数据同步平台</span>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        :collapse="store.sidebarCollapsed"
        background-color="#1a1f36"
        text-color="#a3aed0"
        active-text-color="#409eff"
      >
        <el-menu-item index="/datasources">
          <el-icon><Connection /></el-icon>
          <template #title>数据源管理</template>
        </el-menu-item>
        <el-menu-item index="/jobs">
          <el-icon><List /></el-icon>
          <template #title>同步任务</template>
        </el-menu-item>
        <el-menu-item index="/logs">
          <el-icon><Document /></el-icon>
          <template #title>执行日志</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- Header -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="store.toggleSidebar">
            <Fold v-if="!store.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <span class="page-title">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <el-tag type="success" effect="plain">v1.0.0</el-tag>
        </div>
      </el-header>

      <!-- Content -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useMainStore } from '@/stores/main'
import { DataLine, Connection, List, Document, Fold, Expand } from '@element-plus/icons-vue'

const store = useMainStore()
const route = useRoute()

const currentTitle = computed(() => {
  return route.meta.title || '控制台'
})
</script>

<style scoped>
.app-layout {
  height: 100vh;
}
.sidebar {
  background: #1a1f36;
  transition: width 0.3s;
  overflow: hidden;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}
.header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.collapse-btn {
  cursor: pointer;
  font-size: 20px;
  color: #606266;
}
.page-title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.main-content {
  background: #f0f2f5;
}
</style>
