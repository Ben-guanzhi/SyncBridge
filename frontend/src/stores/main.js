import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useMainStore = defineStore('main', () => {
  const sidebarCollapsed = ref(false)
  const isDark = ref(false)

  const label = computed(() => sidebarCollapsed.value ? '☰' : '✕')

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return { sidebarCollapsed, isDark, label, toggleSidebar }
})
