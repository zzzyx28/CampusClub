<script setup>
import { defineProps, defineEmits } from 'vue';

defineProps({
  menuItems: {
    type: Array,
    required: true,
  },
  activeMenu: {
    type: String,
    default: '',
  },
});

const emit = defineEmits(['select']);

const handleSelect = (index) => {
  emit('select', index);
};
</script>

<template>
  <div class="side-menu">
    <div class="menu-label">管理菜单</div>
    <el-menu :default-active="activeMenu" class="menu" @select="handleSelect">
      <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
        <el-icon v-if="item.icon">
          <component :is="item.icon" />
        </el-icon>
        <span>{{ item.title }}</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<style scoped>
.side-menu {
  height: 100%;
  background: var(--cc-surface);
  padding: 0.75rem 0;
}

.menu-label {
  padding: 0.5rem 1.25rem 0.75rem;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--cc-muted);
}

.menu {
  height: calc(100% - 2rem);
  border-right: none;
  background: transparent;
  padding: 0 0.5rem;
}

:deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  margin: 2px 0;
  border-radius: var(--sa-radius-sm);
  transition: var(--sa-transition);
  font-weight: 500;
  color: var(--cc-ink-soft);
}

:deep(.el-menu-item:hover) {
  background-color: var(--cc-paper) !important;
  color: var(--cc-ink) !important;
}

:deep(.el-menu-item.is-active) {
  background: var(--cc-accent-soft) !important;
  color: var(--cc-accent) !important;
  font-weight: 600;
  border-left: 3px solid var(--cc-accent);
  padding-left: calc(var(--el-menu-base-level-padding) - 3px) !important;
}

:deep(.el-icon) {
  margin-right: 10px;
  font-size: 17px;
  color: inherit;
}
</style>
