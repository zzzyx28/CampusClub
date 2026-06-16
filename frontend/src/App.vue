<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <RouterView />
</template>

<style>
/* 布局容器：与顶部导航 64px 对齐 */
.user-container,
.service-container,
.admin-container,
.profile-container,
.login-container {
  width: 100%;
  height: 100%;
  padding: 0;
  margin: 0;
  overflow: hidden;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.main-content {
  width: 100%;
  height: calc(100% - var(--cc-header-h, 60px));
  margin: 0;
  overflow: auto;
  position: relative;
}

.el-container {
  margin: 0;
  padding: 0;
  border: none;
  height: 100%;
  width: 100%;
}

.el-menu {
  width: 100%;
  height: 100%;
}

.el-table {
  width: 100%;
}
</style>
