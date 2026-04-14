<script setup>
import { ref, defineProps, defineEmits } from 'vue';

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  placeholder: {
    type: String,
    default: '请输入消息'
  },
  emptyText: {
    type: String,
    default: '暂无消息'
  },
  userType: {
    type: String,
    default: 'user' // 'user' 或 'service'
  },
  loading: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['send', 'closeConversation', 'transferToService']);

const message = ref('');

// 发送消息
const sendMessage = () => {
  if (message.value.trim() === '' || props.disabled) return;
  
  emit('send', message.value);
  message.value = '';
};

// 快速回复按钮点击
const quickReply = (text) => {
  if (props.disabled) return;
  message.value = text;
  sendMessage();
};

// 关闭会话
const closeConversation = () => {
  emit('closeConversation');
};

// 转接人工客服
const transferToService = () => {
  emit('transferToService');
};
</script>

<template>
  <div class="chat-box">
    <!-- 操作按钮区域 - 仅在用户界面显示 -->
    <div class="chat-actions" v-if="userType === 'user'">
      <el-button 
        type="primary" 
        size="small" 
        @click="transferToService" 
        class="action-btn"
        :loading="loading"
        :disabled="disabled"
      >
        人工客服
      </el-button>
      <el-button 
        type="danger" 
        size="small" 
        @click="closeConversation" 
        class="action-btn"
        :loading="loading"
        :disabled="disabled"
      >
        关闭会话
      </el-button>
    </div>
    
    <!-- 聊天消息区域 -->
    <div class="chat-messages" v-if="messages.length > 0">
      <div 
        v-for="(msg, index) in messages" 
        :key="index" 
        class="message"
        :class="{ 
          'message-user': (userType === 'user' && msg.type === 'user') || (userType === 'service' && msg.type === 'service'), 
          'message-ai': (userType === 'user' && msg.type === 'ai'),
          'message-customer': (userType === 'service' && msg.type === 'customer'),
          'message-system': msg.type === 'system'
        }"
      >
        <div class="message-content">{{ msg.content }}</div>
        <div class="message-time">{{ msg.time }}</div>
      </div>
    </div>
    
    <!-- 空聊天提示 -->
    <div class="empty-chat" v-else>
      <div v-if="userType === 'user'" class="welcome-message">
        <h1 class="welcome-title">有什么能帮您？</h1>
        <p class="welcome-subtitle">您可以询问任何问题，我会尽力为您解答</p>
      </div>
      <el-empty v-else :description="emptyText">
        <slot name="quick-actions">
          <el-button type="primary" @click="quickReply('您好，有什么可以帮助您的？')">发送问候</el-button>
        </slot>
      </el-empty>
    </div>
    
    <!-- 输入框 -->
    <div class="input-container">
      <el-input
        v-model="message"
        :placeholder="placeholder"
        type="textarea"
        :rows="3"
        :disabled="disabled"
        @keyup.enter.ctrl="sendMessage"
      />
      <el-button 
        type="primary" 
        @click="sendMessage" 
        :disabled="message.trim() === '' || disabled"
        :loading="loading"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.chat-box {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  box-sizing: border-box;
  max-width: 800px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.92);
  border-radius: var(--sa-radius-lg, 16px);
  border: 1px solid var(--sa-border, #e2e8f0);
  box-shadow: var(--sa-shadow-card, 0 4px 24px rgba(30, 58, 138, 0.07));
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  overflow: hidden;
}

.chat-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 16px;
  background: var(--sa-bg-elevated, #f8fafc);
  border-bottom: 1px solid var(--sa-border, #e2e8f0);
}

.action-btn {
  cursor: pointer;
  transition: box-shadow 0.2s ease, opacity 0.2s ease;
}

.action-btn:hover:not(:disabled) {
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.12);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  width: 100%;
  box-sizing: border-box;
  background: linear-gradient(180deg, #f8fafc 0%, #eff6ff 100%);
}

.message {
  max-width: 80%;
  margin-bottom: 15px;
  padding: 12px 16px;
  border-radius: 12px;
  position: relative;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.message-user {
  align-self: flex-end;
  background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%);
  color: white;
  margin-left: auto;
}

.message-ai, .message-customer {
  align-self: flex-start;
  background-color: rgba(255, 255, 255, 0.95);
  color: var(--sa-text, #0f172a);
  border: 1px solid var(--sa-border, #e2e8f0);
}

.message-system {
  align-self: center;
  background-color: #ecfdf5;
  color: #15803d;
  max-width: 90%;
  text-align: center;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  border: 1px solid #bbf7d0;
}

.message-content {
  word-break: break-word;
  line-height: 1.5;
}

.message-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 6px;
  text-align: right;
}

.message-ai .message-time,
.message-customer .message-time {
  color: var(--sa-text-muted, #475569);
}

.empty-chat {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  width: 100%;
  background: linear-gradient(180deg, #f8fafc 0%, #eff6ff 100%);
}

.welcome-message {
  text-align: center;
  padding: 40px 24px;
}

.welcome-title {
  font-family: var(--sa-font-heading, 'Poppins', sans-serif);
  font-size: clamp(1.5rem, 4vw, 2rem);
  color: var(--sa-text, #0f172a);
  margin-bottom: 12px;
  font-weight: 600;
}

.welcome-subtitle {
  font-size: 1rem;
  color: var(--sa-text-muted, #475569);
  margin: 0;
  max-width: 28em;
  margin-inline: auto;
  line-height: 1.6;
}

.input-container {
  display: flex;
  align-items: flex-end;
  padding: 16px;
  background: rgba(255, 255, 255, 0.96);
  border-top: 1px solid var(--sa-border, #e2e8f0);
  width: 100%;
  box-sizing: border-box;
}

.input-container .el-input {
  margin-right: 12px;
  flex: 1;
}

.input-container :deep(.el-textarea__inner) {
  border-radius: 12px;
  min-height: 88px !important;
}

.input-container .el-button {
  height: 44px;
  min-width: 88px;
  border-radius: 12px;
  cursor: pointer;
}

/* 滚动条样式 */
.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>