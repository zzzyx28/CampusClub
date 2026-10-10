<script setup>
import {ref, onMounted, onUnmounted} from 'vue';
import {useRouter} from 'vue-router';
import {ElMessage, ElMessageBox} from 'element-plus';
import HeaderNav from '@/components/common/HeaderNav.vue';
import ChatBox from '@/components/chat/ChatBox.vue';
import AgentRunPanel from '@/components/chat/AgentRunPanel.vue';
import {conversationApi, orchestrationApi} from '@/api';
import {parseJwt} from '@/utils/jwt';
import {getToken} from '@/utils/auth';

const router = useRouter();
const username = ref('用户名');
const chatMessages = ref([]);
const conversationActive = ref(true);
const loading = ref(false);
const conversationUuid = ref('');
const currentUserId = ref(null);
const isTransferToService = ref(false);

// SSE 相关
let eventSource = null;
const isReceivingStream = ref(false);
const agentRun = ref(null);
const agentEvents = ref([]);
const approvalLoading = ref(false);
let currentAiMessageIndex = -1;
let lastEventSequence = 0;

let pollTimer = null; // ⭐ 新增：轮询定时器

// 停止轮询
const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
};

// 开始轮询（仅人工咨询模式）
const startPolling = (conversationId) => {
  stopPolling();
  pollTimer = setInterval(async () => {
    try {
      const res = await conversationApi.getConversationMessages(conversationId);
      if (res.code === 200 && res.data?.messages) {
        const messages = res.data.messages;
        const oldLen = chatMessages.value.length;
        const newLen = messages.length;

        if (newLen > oldLen) {
          const diff = messages.slice(oldLen);
          chatMessages.value.push(
            ...diff.map(msg => ({
              content: msg.text,
              type: msg.speaker === 'user' ? 'user' : 'ai', // 这里可能要改成 'service'
              time: msg.timestamp
                ? new Date(msg.timestamp).toLocaleTimeString()
                : new Date().toLocaleTimeString()
            }))
          );
        }
      }
    } catch (e) {
      console.error('轮询消息失败:', e);
    }
  }, 1000);
};

// 初始化会话
const initConversation = async () => {
  try {
    loading.value = true;

    // 从token中解析用户信息
    const token = localStorage.getItem('token');
    if (!token) {
      ElMessage.error('未找到用户信息，请重新登录');
      router.push('/login');
      return;
    }

    const userData = parseJwt(token);
    username.value = userData.username;
    currentUserId.value = userData.userId;

    // 检查是否有进行中的会话
    const activeConvResponse = await conversationApi.getUserActiveConversation(currentUserId.value);

    // 如果有进行中的会话，加载会话消息
    if (activeConvResponse.data) {
      const conversation = activeConvResponse.data;
      conversationUuid.value = conversation.conversationUuid;
      if (conversation.status === 2) {
        isTransferToService.value = true;
        startPolling(conversation.conversationUuid); // ⭐ 开始轮询
      }

      // 获取会话消息记录
      try {
        const messagesResponse = await conversationApi.getConversationMessages(conversation.conversationUuid);
        if (messagesResponse.code === 200 && messagesResponse.data && messagesResponse.data.messages) {
          chatMessages.value = messagesResponse.data.messages.map(msg => ({
            content: msg.text,
            type: msg.speaker === 'user' ? 'user' : 'ai',
            time: msg.timestamp ? new Date(msg.timestamp).toLocaleTimeString() : new Date().toLocaleTimeString()
          }));
        } else {
          // 如果没有消息记录，添加欢迎消息
          chatMessages.value = [
            {
              content: '欢迎回来，有什么可以帮助您的？',
              type: 'ai',
              time: new Date().toLocaleTimeString()
            }
          ];
        }
      } catch (error) {
        console.error('获取会话消息失败:', error);
        chatMessages.value = [
          {
            content: '欢迎回来，有什么可以帮助您的？',
            type: 'ai',
            time: new Date().toLocaleTimeString()
          }
        ];
      }
    } else {
      // 如果没有进行中的会话，创建新的会话
      const createResponse = await conversationApi.createConversation({
        userId: currentUserId.value,
        initialMessage: '您好，欢迎使用智能社团助手，有什么可以帮助您的？'
      });

      // 保存会话UUID
      conversationUuid.value = createResponse.data.conversationUuid;

      // 添加初始消息
      chatMessages.value = [
        {
          content: '您好，欢迎使用智能社团助手，有什么可以帮助您的？',
          type: 'ai',
          time: new Date().toLocaleTimeString()
        }
      ];
    }
  } catch (error) {
    console.error('初始化会话失败:', error);
    ElMessage.error(error.message || '初始化会话失败');

    // 创建本地会话，防止页面崩溃
    chatMessages.value = [
      {
        content: '欢迎使用智能社团助手，有什么可以帮助您的？(本地模式)',
        type: 'ai',
        time: new Date().toLocaleTimeString()
      }
    ];
  } finally {
    loading.value = false;
  }
};

// 记录消息到后端
const logMessage = async (messageText, speaker) => {
  try {
    if (!conversationUuid.value) return;

    const messageData = {
      text: messageText,
      speaker: speaker,
      timestamp: new Date().toISOString()
    };

    await conversationApi.sendMessage(conversationUuid.value, messageData);
  } catch (error) {
    console.error('记录消息失败:', error);
  }
};

const closeAgentStream = () => {
  if (eventSource) {
    eventSource.close();
    eventSource = null;
  }
  isReceivingStream.value = false;
};

const updateTaskState = (agentEvent) => {
  const task = agentRun.value?.tasks?.find(item => item.taskId === agentEvent.taskId);
  if (!task) return;
  if (agentEvent.type === 'TASK_STARTED') task.status = 'RUNNING';
  if (['TASK_COMPLETED', 'TASK_FAILED'].includes(agentEvent.type)) {
    task.status = agentEvent.payload?.status || (agentEvent.type === 'TASK_COMPLETED' ? 'COMPLETED' : 'FAILED');
    task.attempts = agentEvent.payload?.attempts;
    task.latencyMs = agentEvent.payload?.latencyMs;
  }
};

const setCurrentAiContent = (content) => {
  if (currentAiMessageIndex >= 0 && chatMessages.value[currentAiMessageIndex]) {
    chatMessages.value[currentAiMessageIndex].content = content;
  }
};

const finishAgentRun = (answer, status) => {
  closeAgentStream();
  if (agentRun.value) agentRun.value.status = status;
  const finalAnswer = answer || '本次 Agent 编排没有返回可用结果。';
  setCurrentAiContent(finalAnswer);
  logMessage(finalAnswer, 'assistant');
};

const handleAgentEvent = (agentEvent) => {
  if (!agentEvent?.sequence || agentEvent.sequence <= lastEventSequence) return;
  lastEventSequence = agentEvent.sequence;
  agentEvents.value.push(agentEvent);
  updateTaskState(agentEvent);

  if (agentEvent.type === 'PLAN_STARTED' && agentRun.value) agentRun.value.status = 'RUNNING';
  if (agentEvent.type === 'APPROVAL_GRANTED' && agentRun.value) agentRun.value.status = 'RUNNING';

  if (agentEvent.type === 'APPROVAL_REQUIRED') {
    closeAgentStream();
    agentRun.value.status = 'WAITING_APPROVAL';
    const draft = agentEvent.payload?.draftAnswer || '只读核验已完成。';
    setCurrentAiContent(`${draft}\n\n该操作会修改报名数据，请在右侧协作轨迹中批准或拒绝。`);
  }

  if (agentEvent.type === 'RUN_COMPLETED') {
    finishAgentRun(agentEvent.payload?.answer, agentEvent.payload?.status || 'COMPLETED');
  }
  if (agentEvent.type === 'RUN_FAILED') {
    finishAgentRun(agentEvent.payload?.answer || '本次 Agent 编排失败，请稍后重试。', 'FAILED');
  }
  if (agentEvent.type === 'RUN_CANCELLED') {
    finishAgentRun(agentEvent.payload?.answer || '本次操作已取消。', 'CANCELLED');
  }
};

// 订阅持久化运行事件；断线重连通过事件序号避免重复消费。
const connectAgentEvents = (runId) => {
  closeAgentStream();
  isReceivingStream.value = true;
  const params = new URLSearchParams({ after: String(lastEventSequence) });
  const token = getToken();
  if (token) params.set('token', token);
  const baseApi = import.meta.env.VITE_APP_BASE_API || '/api';
  eventSource = new EventSource(`${baseApi}/agent-runs/${runId}/events?${params.toString()}`);
  eventSource.onopen = () => { isReceivingStream.value = true; };
  eventSource.onmessage = (event) => {
    try {
      handleAgentEvent(JSON.parse(event.data));
    } catch (error) {
      console.error('解析 Agent 事件失败:', error);
    }
  };
  eventSource.onerror = (error) => {
    console.warn('Agent 事件流暂时断开，浏览器将自动重连', error);
  };
};

const cancelActiveAgentRun = async () => {
  if (!agentRun.value || ['COMPLETED', 'PARTIALLY_COMPLETED', 'FAILED', 'CANCELLED'].includes(agentRun.value.status)) return;
  try {
    const response = await orchestrationApi.cancelRun(agentRun.value.id);
    agentRun.value = response.data;
  } catch (error) {
    console.warn('取消 Agent 运行失败:', error);
  } finally {
    closeAgentStream();
  }
};

const createAgentRun = async (message) => {
  const idempotencyKey = globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random()}`;
  const response = await orchestrationApi.startRun({
    message,
    conversationId: conversationUuid.value
  }, idempotencyKey);
  agentRun.value = response.data;
  agentEvents.value = [];
  lastEventSequence = 0;
  chatMessages.value.push({
    content: '正在拆解任务并调度专职 Agent…',
    type: 'ai',
    time: new Date().toLocaleTimeString()
  });
  currentAiMessageIndex = chatMessages.value.length - 1;
  connectAgentEvents(agentRun.value.id);
};

const handleApproval = async (approved) => {
  if (!agentRun.value || approvalLoading.value) return;
  try {
    approvalLoading.value = true;
    const response = await orchestrationApi.decideApproval(agentRun.value.id, approved);
    agentRun.value = response.data;
    if (approved) {
      setCurrentAiContent('审批已通过，事务 Agent 正在核对并执行…');
      connectAgentEvents(agentRun.value.id);
    } else {
      finishAgentRun(response.data.finalAnswer, 'CANCELLED');
    }
  } catch (error) {
    ElMessage.error(error.message || '审批操作失败');
  } finally {
    approvalLoading.value = false;
  }
};

// 发送消息处理
const handleSendMessage = async (messageText) => {
  if (!conversationActive.value) {
    ElMessage.warning('会话已关闭，无法发送消息');
    return;
  }

  try {
    // 添加用户消息到界面
    const userMsg = {
      content: messageText,
      type: 'user',
      time: new Date().toLocaleTimeString()
    };
    chatMessages.value.push(userMsg);

    // 记录用户消息到后端
    await logMessage(messageText, 'user');

    if (!isTransferToService.value) {
      loading.value = true;
      await createAgentRun(messageText);
    }

  } catch (error) {
    console.error('发送消息失败:', error);
    ElMessage.error('发送消息失败: ' + (error.message || '未知错误'));
  } finally {
    loading.value = false;
  }
};

// 处理关闭会话
const handleCloseConversation = async () => {
  ElMessageBox.confirm(
    '确定要关闭当前会话吗？关闭后将无法继续发送消息。',
    '关闭会话',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {
      try {
        loading.value = true;

        await cancelActiveAgentRun();
        stopPolling(); // ⭐ 停止轮询

        if (conversationUuid.value) {
          // 调用后端API关闭会话
          await conversationApi.closeConversation(conversationUuid.value);
        }

        conversationActive.value = false;

        // 添加系统消息，提示会话已关闭
        chatMessages.value.push({
          content: '会话已关闭。如需继续咨询，请刷新页面开始新的会话。',
          type: 'system',
          time: new Date().toLocaleTimeString()
        });

        ElMessage.success('会话已关闭');
      } catch (error) {
        console.error('关闭会话失败:', error);
        ElMessage.error(error.message || '关闭会话失败');
      } finally {
        loading.value = false;
      }
    })
    .catch(() => {
      // 用户取消关闭会话
    });
};

// 处理转人工咨询
const handleTransferToService = async () => {
  ElMessageBox.confirm(
    '确定要转接到人工咨询吗？转接后将由人工咨询为您服务。',
    '转接人工咨询',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    }
  )
    .then(async () => {
      try {
        loading.value = true;

        if (!conversationUuid.value) {
          ElMessage.warning('无效的会话');
          return;
        }

        await cancelActiveAgentRun();

        // 调用后端API转接到人工咨询
        const response = await conversationApi.transferConversation(conversationUuid.value, {
          serviceUserId: null // 系统自动分配社团助手
        });

        if (response.code === 200) {
          // 添加系统消息
          chatMessages.value.push({
            content: '正在为您转接人工咨询，请稍候...',
            type: 'system',
            time: new Date().toLocaleTimeString()
          });

          // 记录转接消息
          await logMessage('用户请求转接人工咨询', 'service');

          isTransferToService.value = true;
          // ⭐ 开始轮询社团助手消息
          startPolling(conversationUuid.value);

          ElMessage.success('已转接到人工咨询，请等待社团助手接入');
        } else {
          throw new Error(response.message || '转接失败');
        }
      } catch (error) {
        console.error('转接社团助手失败:', error);
        ElMessage.error(error.message || '转接社团助手失败');
      } finally {
        loading.value = false;
      }
    })
    .catch(() => {
      // 用户取消转接
    });
};

// 组件卸载时清理SSE连接
onUnmounted(() => {
  if (eventSource) {
    eventSource.close();
    eventSource = null;
  }
  stopPolling(); // ⭐ 记得清理定时器
});

// 组件挂载时初始化会话
onMounted(() => {
  initConversation();
});
</script>

<template>
  <div class="user-container">
    <!-- 使用通用顶部导航栏组件 -->
    <HeaderNav :username="username"/>

    <!-- 主要内容区域 -->
    <main class="main-content cc-page-bg">
      <div class="agent-workspace">
        <div class="chat-container">
          <ChatBox
            :messages="chatMessages"
            placeholder="描述一个需要 Agent 团队协作的问题"
            empty-text="有什么能帮您？"
            user-type="user"
            :loading="loading"
            :disabled="!conversationActive || isReceivingStream || agentRun?.status === 'WAITING_APPROVAL'"
            @send="handleSendMessage"
            @closeConversation="handleCloseConversation"
            @transferToService="handleTransferToService"
          />

          <div v-if="isReceivingStream" class="stream-status">
            <el-icon class="is-loading">
              <Loading/>
            </el-icon>
            <span>Agent 团队协作中...</span>
          </div>
        </div>

        <AgentRunPanel
          :run="agentRun"
          :events="agentEvents"
          :action-loading="approvalLoading"
          @approve="handleApproval(true)"
          @reject="handleApproval(false)"
        />
      </div>
    </main>
  </div>
</template>

<style scoped>
.user-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 1.25rem;
  width: 100%;
  box-sizing: border-box;
  overflow: auto;
}

.agent-workspace {
  width: 100%;
  max-width: 1260px;
  min-height: 0;
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: stretch;
  justify-content: center;
  gap: 1rem;
}

.stream-status {
  position: absolute;
  bottom: 1rem;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.25rem;
  background: var(--cc-surface);
  border-radius: 999px;
  color: var(--cc-accent);
  font-size: 0.875rem;
  font-weight: 600;
  border: 1px solid var(--cc-border);
  box-shadow: var(--sa-shadow-md);
  z-index: 10;
}

.chat-container {
  width: 100%;
  max-width: 900px;
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
}

@media (max-width: 1120px) {
  .agent-workspace {
    height: auto;
    min-height: 100%;
    flex-direction: column;
  }

  .chat-container {
    min-height: 620px;
    max-width: none;
  }
}

.stream-status .el-icon {
  font-size: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-content {
    padding: 10px;
  }

  .chat-container {
    max-width: 100%;
  }

  .stream-status {
    bottom: 10px;
    left: 10px;
    right: 10px;
    transform: none;
    font-size: 13px;
    padding: 10px 16px;
  }
}
</style>
