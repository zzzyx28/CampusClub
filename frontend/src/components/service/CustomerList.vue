<script setup>
import { defineProps, defineEmits } from 'vue';

const props = defineProps({
  customers: {
    type: Array,
    default: () => []
  },
  currentCustomerId: {
    type: Number,
    default: null
  }
});

const emit = defineEmits(['select']);

// 选择客户
const selectCustomer = (customer) => {
  emit('select', customer);
};
</script>

<template>
  <div class="customer-list">
    <div
      v-for="customer in customers"
      :key="customer.id"
      class="customer-item"
      :class="{ active: currentCustomerId === customer.id }"
      @click="selectCustomer(customer)"
    >
      <el-avatar :size="40" icon="User" />
      <div class="customer-info">
        <div class="customer-name">{{ customer.name }}</div>
        <div class="customer-message">{{ customer.lastMessage }}</div>
      </div>
      <div class="customer-meta">
        <div class="customer-time">{{ customer.time }}</div>
        <el-badge v-if="customer.unread > 0" :value="customer.unread" class="unread-badge" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.customer-list {
  height: 100%;
  overflow-y: auto;
}

.customer-item {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid var(--sa-border, #e2e8f0);
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease;
  border-left: 3px solid transparent;
}

.customer-item:hover {
  background-color: rgba(37, 99, 235, 0.06);
}

.customer-item.active {
  background: linear-gradient(90deg, rgba(37, 99, 235, 0.1), rgba(37, 99, 235, 0.02));
  border-left-color: var(--sa-primary-bright, #2563eb);
}

.customer-info {
  flex: 1;
  margin-left: 10px;
  overflow: hidden;
}

.customer-name {
  font-weight: 600;
  margin-bottom: 5px;
  color: var(--sa-text, #0f172a);
}

.customer-message {
  color: var(--sa-text-muted, #475569);
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.customer-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.customer-time {
  color: var(--sa-text-muted, #475569);
  font-size: 12px;
  margin-bottom: 5px;
}

.unread-badge {
  margin-top: 2px;
}
</style> 