<script setup>
import { defineProps } from 'vue';

defineProps({
  title: {
    type: String,
    required: true,
  },
  value: {
    type: [Number, String],
    required: true,
  },
  icon: {
    type: String,
    default: '',
  },
  color: {
    type: String,
    default: '#c84b31',
  },
});
</script>

<template>
  <el-card class="stat-card" shadow="never">
    <div class="stat-content">
      <div class="stat-icon" :style="{ backgroundColor: color }">
        <el-icon v-if="icon">
          <component :is="icon" />
        </el-icon>
      </div>
      <div class="stat-info">
        <div class="stat-value">{{ value }}</div>
        <div class="stat-title">{{ title }}</div>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.stat-card {
  width: 100%;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--sa-shadow-md) !important;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: white;
  font-size: 22px;
  flex-shrink: 0;
}

.stat-value {
  font-size: 1.65rem;
  font-weight: 700;
  font-family: var(--sa-font-heading);
  color: var(--cc-ink);
  letter-spacing: -0.02em;
  line-height: 1.1;
}

.stat-title {
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--cc-muted);
  margin-top: 0.25rem;
}
</style>
