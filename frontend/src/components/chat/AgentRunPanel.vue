<script setup>
import { computed } from 'vue';

const props = defineProps({
  run: { type: Object, default: null },
  events: { type: Array, default: () => [] },
  actionLoading: { type: Boolean, default: false }
});

const emit = defineEmits(['approve', 'reject']);

const roleNames = {
  ACTIVITY_RESEARCHER: '活动侦察员',
  REGISTRATION_SPECIALIST: '报名顾问',
  POLICY_ANALYST: '政策分析员',
  DATA_ANALYST: '数据分析员',
  CAMPUS_GUIDE: '校园向导',
  TRANSACTION_EXECUTOR: '事务执行员',
  QUALITY_REVIEWER: '质量审校员'
};

const statusMeta = {
  QUEUED: ['排队中', 'muted'],
  RUNNING: ['协作中', 'running'],
  WAITING_APPROVAL: ['待审批', 'warning'],
  COMPLETED: ['已完成', 'success'],
  PARTIALLY_COMPLETED: ['降级完成', 'warning'],
  FAILED: ['失败', 'danger'],
  CANCELLED: ['已取消', 'muted']
};

const visibleTasks = computed(() => props.run?.tasks || []);
const completedCount = computed(() => visibleTasks.value.filter(task =>
  ['COMPLETED', 'FAILED', 'TIMED_OUT', 'SKIPPED'].includes(task.status)).length);
const progress = computed(() => visibleTasks.value.length
  ? Math.round((completedCount.value / visibleTasks.value.length) * 100)
  : 0);
const recentEvents = computed(() => props.events.slice(-8).reverse());
const runStatus = computed(() => statusMeta[props.run?.status] || [props.run?.status || '—', 'muted']);

const taskState = (status) => ({
  PENDING: '等待', RUNNING: '执行中', COMPLETED: '完成', FAILED: '失败', TIMED_OUT: '超时', SKIPPED: '跳过'
}[status] || status);
</script>

<template>
  <aside v-if="run" class="agent-panel">
    <div class="panel-header">
      <div>
        <span class="eyebrow">ORCHESTRATION</span>
        <h2>Agent 协作轨迹</h2>
      </div>
      <span class="run-status" :class="runStatus[1]">{{ runStatus[0] }}</span>
    </div>

    <div class="run-meta">
      <span>Run {{ run.id?.slice(0, 8) }}</span>
      <span>{{ completedCount }}/{{ visibleTasks.length }} tasks</span>
    </div>
    <div class="progress-track"><i :style="{ width: `${progress}%` }"></i></div>

    <div class="task-list">
      <div v-for="task in visibleTasks" :key="task.taskId" class="task-item" :class="task.status?.toLowerCase()">
        <span class="task-dot"></span>
        <div class="task-copy">
          <strong>{{ roleNames[task.role] || task.role }}</strong>
          <small>{{ task.objective }}</small>
        </div>
        <span class="task-state">{{ taskState(task.status) }}</span>
      </div>
    </div>

    <div v-if="run.status === 'WAITING_APPROVAL'" class="approval-card">
      <span class="eyebrow">HUMAN IN THE LOOP</span>
      <strong>写操作需要你的批准</strong>
      <p>{{ run.approvalReason }}</p>
      <div class="approval-actions">
        <el-button size="small" @click="emit('reject')" :disabled="actionLoading">拒绝</el-button>
        <el-button size="small" type="primary" @click="emit('approve')" :loading="actionLoading">批准执行</el-button>
      </div>
    </div>

    <details v-if="recentEvents.length" class="event-log">
      <summary>审计事件（{{ events.length }}）</summary>
      <div v-for="event in recentEvents" :key="event.sequence" class="event-row">
        <span>#{{ event.sequence }}</span>
        <p>{{ event.message }}</p>
      </div>
    </details>
  </aside>
</template>

<style scoped>
.agent-panel {
  width: 330px;
  flex: 0 0 330px;
  padding: 1rem;
  border: 1px solid var(--cc-border);
  border-radius: var(--sa-radius-lg);
  background: color-mix(in srgb, var(--cc-surface) 94%, var(--cc-accent));
  box-shadow: var(--sa-shadow-card);
  overflow: auto;
}

.panel-header, .run-meta, .approval-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: .75rem;
}

.eyebrow {
  color: var(--cc-accent);
  font-size: .64rem;
  font-weight: 800;
  letter-spacing: .12em;
}

h2 { margin: .15rem 0 0; font-size: 1rem; color: var(--cc-ink); }
.run-status { padding: .25rem .55rem; border-radius: 999px; font-size: .7rem; font-weight: 700; }
.run-status.running { color: var(--cc-accent); background: color-mix(in srgb, var(--cc-accent) 12%, transparent); }
.run-status.success { color: var(--cc-green); background: var(--cc-green-soft); }
.run-status.warning { color: #8a5b00; background: #fff3d6; }
.run-status.danger { color: #a83030; background: #ffe8e8; }
.run-status.muted { color: var(--cc-muted); background: var(--cc-paper); }

.run-meta { margin-top: 1rem; color: var(--cc-muted); font: 600 .68rem/1.2 ui-monospace, monospace; }
.progress-track { height: 4px; margin: .5rem 0 1rem; overflow: hidden; border-radius: 99px; background: var(--cc-border); }
.progress-track i { display: block; height: 100%; background: var(--cc-accent); transition: width .35s ease; }
.task-list { display: grid; gap: .55rem; }
.task-item { display: grid; grid-template-columns: 10px 1fr auto; align-items: center; gap: .55rem; padding: .65rem; border: 1px solid var(--cc-border); border-radius: 10px; background: var(--cc-paper); }
.task-dot { width: 8px; height: 8px; border-radius: 50%; background: #b4b8bf; }
.task-item.running .task-dot { background: var(--cc-accent); box-shadow: 0 0 0 4px color-mix(in srgb, var(--cc-accent) 14%, transparent); }
.task-item.completed .task-dot { background: var(--cc-green); }
.task-item.failed .task-dot, .task-item.timed_out .task-dot { background: #c94545; }
.task-copy { min-width: 0; display: grid; gap: .15rem; }
.task-copy strong { font-size: .78rem; color: var(--cc-ink); }
.task-copy small { color: var(--cc-muted); font-size: .67rem; line-height: 1.35; }
.task-state { font-size: .65rem; color: var(--cc-muted); white-space: nowrap; }
.approval-card { margin-top: .9rem; padding: .8rem; border: 1px solid #efd18a; border-radius: 10px; background: #fffaf0; }
.approval-card strong { display: block; margin: .25rem 0; font-size: .8rem; color: #684600; }
.approval-card p { margin: 0 0 .7rem; color: #775e2b; font-size: .7rem; line-height: 1.45; }
.event-log { margin-top: 1rem; border-top: 1px solid var(--cc-border); padding-top: .75rem; }
.event-log summary { cursor: pointer; color: var(--cc-muted); font-size: .72rem; font-weight: 700; }
.event-row { display: grid; grid-template-columns: 28px 1fr; gap: .35rem; margin-top: .5rem; }
.event-row span { color: var(--cc-muted); font: .62rem ui-monospace, monospace; }
.event-row p { margin: 0; color: var(--cc-ink); font-size: .68rem; line-height: 1.35; }

@media (max-width: 1120px) {
  .agent-panel { width: 100%; flex-basis: auto; max-height: 330px; }
}
</style>
