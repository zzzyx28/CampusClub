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

// 转接社团助手
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
        人工咨询
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
  max-width: 820px;
  margin: 0 auto;
  background: var(--cc-surface);
  border-radius: var(--sa-radius-lg);
  border: 1px solid var(--cc-border);
  box-shadow: var(--sa-shadow-card);
  overflow: hidden;
}

.chat-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: var(--cc-paper);
  border-bottom: 1px solid var(--cc-border);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  background: var(--cc-paper);
}

.message {
  max-width: 78%;
  padding: 0.75rem 1rem;
  border-radius: 14px;
  position: relative;
}

.message-user {
  align-self: flex-end;
  background: var(--cc-accent);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-ai,
.message-customer {
  align-self: flex-start;
  background: var(--cc-surface);
  color: var(--cc-ink);
  border: 1px solid var(--cc-border);
  border-bottom-left-radius: 4px;
}

.message-system {
  align-self: center;
  background: var(--cc-green-soft);
  color: var(--cc-green);
  max-width: 90%;
  text-align: center;
  padding: 0.5rem 1rem;
  border-radius: 999px;
  font-size: 0.8rem;
  border: 1px solid rgba(47, 93, 78, 0.2);
}

.message-content {
  word-break: break-word;
  line-height: 1.55;
  font-size: 0.95rem;
}

.message-time {
  font-size: 0.68rem;
  margin-top: 0.35rem;
  text-align: right;
  opacity: 0.75;
}

.message-ai .message-time,
.message-customer .message-time {
  color: var(--cc-muted);
  opacity: 1;
}

.empty-chat {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--cc-paper);
}

.welcome-message {
  text-align: center;
  padding: 2.5rem 1.5rem;
  max-width: 24rem;
}

.welcome-title {
  font-family: var(--sa-font-heading);
  font-size: clamp(1.4rem, 3.5vw, 1.85rem);
  color: var(--cc-ink);
  margin-bottom: 0.75rem;
  font-weight: 700;
}

.welcome-subtitle {
  font-size: 0.95rem;
  color: var(--cc-muted);
  line-height: 1.65;
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: 0.75rem;
  padding: 1rem;
  background: var(--cc-surface);
  border-top: 1px solid var(--cc-border);
}

.input-container .el-input {
  flex: 1;
}

.input-container .el-button {
  height: 44px;
  min-width: 80px;
}
</style>