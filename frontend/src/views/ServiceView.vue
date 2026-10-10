<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessageBox, ElMessage } from 'element-plus';
import HeaderNav from '@/components/common/HeaderNav.vue';
import CustomerList from '@/components/service/CustomerList.vue';
import ChatBox from '@/components/chat/ChatBox.vue';
import { conversationApi, registrationApi, userApi } from '@/api';
import { parseJwt } from '@/utils/jwt';

const router = useRouter();
const username = ref('社团助手');
const currentChat = ref(null);
const chatMessages = ref([]);
const loading = ref(false);
const serviceUserId = ref(null);
let pollTimer = null;

// 客户会话列表
const customers = ref([]);

// 报名列表
const registrations = ref([]);

const registrationDetail = reactive({
  registrationId: '',
  tierName: '',
  status: 0,
  createdAt: '',
  userId: '',
  username: '',
  realName: '',
  phone: '',
  activityId: '',
  ticketCode: '',
  activityTitle: '',
  activityType: '',
  clubName: '',
  venue: '',
  registrationStart: '',
  eventStart: '',
  eventEnd: '',
  unitPrice: 0,
  totalPrice: 0
});

// 报名详情弹窗控制
const detailDialogVisible = ref(false);
const detailLoading = ref(false);

// 当前客户ID
const currentCustomerId = computed(() => currentChat.value?.userId || null);

// 初始化数据
const initData = async () => {
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
    serviceUserId.value = userData.userId;

    // 加载客服的会话列表 - 使用listAllConversationsByServiceUserId函数
    const conversationsResponse = await conversationApi.listAllConversationsByServiceUserId(serviceUserId.value);

    if (conversationsResponse.code === 200) {
      console.log('获取到的会话数据:', conversationsResponse.data);

      // 转换会话数据格式为客户列表格式
      customers.value = conversationsResponse.data.map(conv => ({
        id: conv.id,
        userId: conv.userId,
        name: conv.user ? conv.user.username : `用户${conv.userId}`,
        unread: 0, // 未读消息数，需要后端API支持
        lastMessage: conv.lastMessage,
        time: new Date(conv.updatedAt).toLocaleTimeString(),
        conversationUuid: conv.conversationUuid,
        status: conv.status === 0 ? '进行中' :
          conv.status === 1 ? '已结束' :
            conv.status === 2 ? '已转人工' : '未知'
      }));
    } else {
      ElMessage.warning(conversationsResponse.message || '没有获取到会话数据');
    }
  } catch (error) {
    console.error('初始化数据失败:', error);
    ElMessage.error(error.message || '加载数据失败');
  } finally {
    loading.value = false;
  }
};

// 停止轮询
const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
};

// 开始轮询
const startPolling = (customer) => {
  stopPolling();

  pollTimer = setInterval(async () => {
    const res = await conversationApi.getConversationMessages(customer.conversationUuid);
    if (res.code === 200 && res.data?.messages) {
      const messages = res.data.messages;
      const oldLen = chatMessages.value.length; // ⭐ 当前前端已有的消息数量
      const newLen = messages.length;

      if (newLen > oldLen) {
        // 取新增的部分
        const diff = messages.slice(oldLen);
        chatMessages.value.push(...diff.map(msg => ({
          content: msg.text,
          type: msg.speaker === 'assistant' || msg.speaker === 'service' ? 'service' : 'customer',
          time: msg.timestamp ? new Date(msg.timestamp).toLocaleTimeString() : new Date().toLocaleTimeString()
        })));
      }
    }
  }, 1000);
};


// 选择客户聊天
const selectCustomer = async (customer) => {
  try {
    loading.value = true;
    currentChat.value = customer;

    // 重置未读消息
    if (customer.unread > 0) {
      customer.unread = 0;
    }

    // 获取与该客户的聊天记录
    const conversationResponse = await conversationApi.getConversation(customer.conversationUuid);

    if (conversationResponse.code === 200) {
      // 获取会话的详细消息记录
      const messagesResponse = await conversationApi.getConversationMessages(customer.conversationUuid);

      if (messagesResponse.code === 200 && messagesResponse.data) {
        console.log('获取到的消息数据:', messagesResponse.data);

        // 只处理新格式：包含 messages 数组的对象
        if (messagesResponse.data.messages && Array.isArray(messagesResponse.data.messages)) {
          chatMessages.value = messagesResponse.data.messages.map(msg => ({
            content: msg.text,
            type: msg.speaker === 'assistant' || msg.speaker === 'service' ? 'service' : 'customer',
            time: msg.timestamp ? new Date(msg.timestamp).toLocaleTimeString() : new Date().toLocaleTimeString()
          }));
        } else {
          // 如果数据结构不符合预期，显示一条系统消息
          chatMessages.value = [
            {
              content: '无法解析会话消息数据',
              type: 'system',
              time: new Date().toLocaleTimeString()
            }
          ];
          console.error('消息数据格式不符合预期:', messagesResponse.data);
        }
      } else {
        // 如果没有消息记录，至少显示一条系统消息
        chatMessages.value = [
          {
            content: customer.lastMessage || '会话开始',
            type: customer.lastMessage ? 'customer' : 'system',
            time: customer.time
          }
        ];
        ElMessage.warning(messagesResponse?.message || '没有获取到聊天记录');
      }

      // 获取客户详细信息
      if (customer.userId) {
        const userInfoResponse = await userApi.getUserInfo(customer.userId);
        if (userInfoResponse.code === 200) {
          // 更新客户信息
          customer.userInfo = userInfoResponse.data;
        }

        // 获取该用户的报名数据
        const regResponse = await registrationApi.listRegistrationsWithDetail(customer.userId);
        if (regResponse.code === 200) {
          registrations.value = regResponse.data;
        } else {
          registrations.value = [];
          ElMessage.warning(regResponse.message || `没有获取到用户 ${customer.name} 的报名数据`);
        }
      } else {
        registrations.value = [];
      }

    }

    // 启动轮询
    startPolling(customer);
  } catch (error) {
    console.error('加载会话失败:', error);
    ElMessage.error('加载会话失败: ' + (error.message || '未知错误'));
  } finally {
    loading.value = false;
  }
};

// 发送消息处理
const handleSendMessage = async (messageText) => {
  if (!currentChat.value) return;

  try {
    // 添加客服消息到界面
    const serviceMsg = {
      content: messageText,
      type: 'service',
      time: new Date().toLocaleTimeString()
    };
    chatMessages.value.push(serviceMsg);

    // 使用新格式发送消息
    const messageData = {
      text: messageText,
      speaker: 'service'
    };

    // 调用API发送消息
    const response = await conversationApi.sendMessage(
      currentChat.value.conversationUuid,
      messageData
    );

    if (response.code !== 200) {
      throw new Error(response.message || '发送消息失败');
    }

    // 更新会话最后一条消息
    const index = customers.value.findIndex(c => c.conversationUuid === currentChat.value.conversationUuid);
    if (index !== -1) {
      customers.value[index].lastMessage = messageText;
      customers.value[index].time = new Date().toLocaleTimeString();
    }
  } catch (error) {
    console.error('发送消息失败:', error);
    ElMessage.error('发送消息失败: ' + (error.message || '未知错误'));
  }
};

// 处理关闭会话
const handleCloseConversation = () => {
  if (!currentChat.value) return;

  ElMessageBox.confirm(
    `确定要关闭与 ${currentChat.value.name} 的会话吗？`,
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

        // 调用API关闭会话
        const response = await conversationApi.closeConversation(currentChat.value.conversationUuid);

        if (response.code === 200) {
          // 添加系统消息，提示会话已关闭
          chatMessages.value.push({
            content: '会话已关闭',
            type: 'system',
            time: new Date().toLocaleTimeString()
          });

          // 更新客户列表中的状态
          const index = customers.value.findIndex(c => c.id === currentChat.value.id);
          if (index !== -1) {
            customers.value[index].status = '已关闭';
          }

          ElMessage.success(`已关闭与 ${currentChat.value.name} 的会话`);
        } else {
          throw new Error(response.message || '关闭会话失败');
        }
      } catch (error) {
        console.error('关闭会话失败:', error);
        ElMessage.error('关闭会话失败');
      } finally {
        loading.value = false;
      }
    })
    .catch(() => {
      // 用户取消关闭会话
    });
};

// 获取当前客户的报名
const getCustomerRegistrations = () => {
  if (!currentChat.value) return [];
  return registrations.value;
};

// 格式化日期时间
const formatDateTime = (dateString) => {
  if (!dateString) return '未知';
  const date = new Date(dateString);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
};

// 获取状态类型
const getStatusType = (status) => {
  return status === 1 ? 'success' : 'danger';
};

// 获取状态名称
const getStatusName = (status) => {
  if (status === 1) return '已报名';
  if (status === 2) return '已取消';
  if (status === 3) return '已参加';
  return '未知';
};

const viewRegistrationDetail = async (registrationId) => {
  detailLoading.value = true;
  detailDialogVisible.value = true;

  try {
    const response = await registrationApi.getRegistrationDetail(registrationId);
    if (response.code === 200) {
      const data = response.data;
      Object.assign(registrationDetail, {
        registrationId: data.registrationId || data.id,
        tierName: data.tierName || '',
        status: data.status,
        createdAt: data.createdAt,
        userId: data.userId,
        username: data.username || '',
        realName: data.realName || '',
        phone: data.phone || '',
        activityId: data.activityId,
        ticketCode: data.ticketCode || '',
        activityTitle: data.activityTitle || '',
        activityType: data.activityType || '',
        clubName: data.clubName || '',
        venue: data.venue || '',
        registrationStart: data.registrationStart || '',
        eventStart: data.eventStart || '',
        eventEnd: data.eventEnd || '',
        unitPrice: data.unitPrice ?? 0,
        totalPrice: data.totalPrice ?? 0
      });
    } else {
      throw new Error(response.message || '获取报名详情失败');
    }
  } catch (error) {
    console.error('获取报名详情失败:', error);
    ElMessage.error('获取报名详情失败');
    detailDialogVisible.value = false;
  } finally {
    detailLoading.value = false;
  }
};

const cancelRegistrationAction = async (reg) => {
  if (reg.status !== 1) {
    ElMessage.warning('只能取消已报名的记录');
    return;
  }

  try {
    ElMessageBox.confirm(
      `确定要取消报名 ${reg.id} 吗？`,
      '取消报名',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    ).then(async () => {
      try {
        const response = await registrationApi.cancelRegistration(reg.id);
        if (response.code === 200) {
          ElMessage.success('报名已取消');
          const index = registrations.value.findIndex((r) => r.id === reg.id);
          if (index !== -1) {
            registrations.value[index].status = 2;
          }
          if (detailDialogVisible.value && registrationDetail.registrationId === reg.id) {
            registrationDetail.status = 2;
          }
        } else {
          throw new Error(response.message || '取消报名失败');
        }
      } catch (error) {
        ElMessage.error('取消报名失败: ' + (error.message || '未知错误'));
      }
    }).catch(() => {});
  } catch {
    ElMessage.error('操作失败');
  }
};

// 组件挂载时初始化数据
onMounted(initData);
onUnmounted(stopPolling);
</script>

<template>
  <div class="service-container">
    <!-- 使用通用顶部导航栏组件 -->
    <HeaderNav :username="username" />

    <!-- 主要内容区域 -->
    <main class="main-content">
      <!-- 左侧区域：会话列表和聊天区域 -->
      <div class="left-section">
        <!-- 会话列表 -->
        <div class="conversation-list">
          <h3 class="list-title">会话列表</h3>
          <CustomerList
            :customers="customers"
            :current-customer-id="currentCustomerId"
            @select="selectCustomer"
          />
        </div>

        <!-- 聊天区域 -->
        <div class="chat-area">
          <div v-if="currentChat" class="chat-header">
            <span class="customer-name">{{ currentChat.name }}</span>
            <el-button
              type="danger"
              size="small"
              @click="handleCloseConversation"
              class="close-chat-btn"
              :disabled="currentChat.status === '已关闭'"
            >
              关闭会话
            </el-button>
          </div>

          <!-- 使用聊天组件 -->
          <div class="chat-wrapper" v-if="currentChat">
            <ChatBox
              :messages="chatMessages"
              placeholder="请输入回复内容"
              empty-text="暂无消息"
              user-type="service"
              :disabled="currentChat.status === '已关闭'"
              @send="handleSendMessage"
              @closeConversation="handleCloseConversation"
            />
          </div>

          <div v-if="!currentChat" class="no-chat">
            <el-empty description="请选择一个客户开始聊天" />
          </div>
        </div>
      </div>

      <!-- 右侧区域：客户信息和订单管理 -->
      <div class="right-section">
        <!-- 客户信息区域 -->
        <div class="customer-info-area">
          <h2 class="section-title">客户信息</h2>
          <div v-if="currentChat && currentChat.userInfo" class="customer-details">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="客户姓名">{{ currentChat.userInfo.realName || currentChat.name }}</el-descriptions-item>
              <el-descriptions-item label="联系方式">{{ currentChat.userInfo.phone || '未提供' }}</el-descriptions-item>
              <el-descriptions-item label="邮箱地址">{{ currentChat.userInfo.email || '未提供' }}</el-descriptions-item>
              <el-descriptions-item label="社团活跃度">
                {{
                  currentChat.userInfo.memberLevel === 1 ? '新成员' :
                    currentChat.userInfo.memberLevel === 2 ? '活跃' :
                      currentChat.userInfo.memberLevel === 3 ? '骨干' :
                        currentChat.userInfo.memberLevel === 4 ? '核心' :
                          currentChat.userInfo.memberLevel === 5 ? '荣誉' : '未知'
                }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
          <div v-else class="empty-info">
            <el-empty description="请选择一个客户查看详情" />
          </div>
        </div>

        <!-- 报名管理区域 -->
        <div class="registration-management-area">
          <h2 class="section-title">报名管理</h2>
          <div v-if="currentChat" class="customer-registrations">
            <el-table :data="getCustomerRegistrations()" style="width: 100%; height: calc(100% - 40px)" v-loading="loading">
              <el-table-column prop="id" label="报名号" width="80" />
              <el-table-column prop="activityId" label="活动ID" width="80" />
              <el-table-column prop="status" label="状态" width="80">
                <template #default="scope">
                  <el-tag :type="getStatusType(scope.row.status)">
                    {{ getStatusName(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="报名时间" width="160">
                <template #default="scope">
                  {{ formatDateTime(scope.row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="160">
                <template #default="scope">
                  <el-button size="small" type="primary" @click="viewRegistrationDetail(scope.row.id)">详情</el-button>
                  <el-button size="small" type="danger" @click="cancelRegistrationAction(scope.row)" :disabled="scope.row.status !== 1">取消</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="getCustomerRegistrations().length === 0" class="no-registrations">
              <el-empty description="暂无报名数据" />
            </div>
          </div>
          <div v-else class="empty-info">
            <el-empty description="请选择一个客户查看报名" />
          </div>
        </div>
      </div>
    </main>

    <el-dialog v-model="detailDialogVisible" title="报名详情" width="650px" destroy-on-close>
      <div v-loading="detailLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="报名ID" :span="2">{{ registrationDetail.registrationId }}</el-descriptions-item>
          <el-descriptions-item label="票档">{{ registrationDetail.tierName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(registrationDetail.status)">{{ getStatusName(registrationDetail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="报名时间" :span="2">{{ formatDateTime(registrationDetail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="用户">{{ registrationDetail.realName || registrationDetail.username }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ registrationDetail.phone || '未提供' }}</el-descriptions-item>
          <el-descriptions-item label="活动" :span="2">{{ registrationDetail.activityTitle }}</el-descriptions-item>
          <el-descriptions-item label="社团">{{ registrationDetail.clubName }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ registrationDetail.activityType }}</el-descriptions-item>
          <el-descriptions-item label="地点">{{ registrationDetail.venue }}</el-descriptions-item>
          <el-descriptions-item label="活动开始">{{ formatDateTime(registrationDetail.eventStart) }}</el-descriptions-item>
          <el-descriptions-item label="票价">¥{{ registrationDetail.unitPrice }}</el-descriptions-item>
          <el-descriptions-item label="总价">¥{{ registrationDetail.totalPrice }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button type="danger" @click="cancelRegistrationAction({ id: registrationDetail.registrationId, status: registrationDetail.status })" :disabled="registrationDetail.status !== 1">
            取消报名
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.service-container {
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
  background: var(--cc-paper);
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
  height: calc(100% - var(--cc-header-h, 60px));
}

.left-section {
  width: 60%;
  height: 100%;
  display: flex;
  border-right: 1px solid var(--cc-border);
}

.conversation-list {
  width: 280px;
  height: 100%;
  background: var(--cc-surface);
  border-right: 1px solid var(--cc-border);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.list-title {
  padding: 1rem 1.125rem;
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  font-family: var(--sa-font-heading);
  border-bottom: 1px solid var(--cc-border);
  background: var(--cc-paper);
  color: var(--cc-ink);
}

.chat-header {
  height: 56px;
  padding: 0 1.25rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--cc-surface);
  border-bottom: 1px solid var(--cc-border);
}

.customer-name {
  font-weight: 600;
  font-size: 1rem;
  color: var(--cc-ink);
  font-family: var(--sa-font-heading);
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.close-chat-btn {
  opacity: 0.8;
  transition: opacity 0.3s;
}

.close-chat-btn:hover {
  opacity: 1;
}

.chat-wrapper {
  flex: 1;
  overflow: hidden;
  height: calc(100% - 60px);
}

.no-chat {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  background: transparent;
  height: 100%;
}

/* 右侧区域样式 */
.right-section {
  width: 40%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.customer-info-area {
  flex: 1;
  padding: 1.25rem;
  background-color: var(--cc-surface);
  border-bottom: 1px solid var(--cc-border);
  overflow: auto;
}

.registration-management-area {
  flex: 1;
  padding: 20px;
  background-color: white;
  overflow: auto;
}

.section-title {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 1.05rem;
  font-weight: 600;
  font-family: var(--sa-font-heading, 'Poppins', sans-serif);
  color: var(--sa-text, #0f172a);
}

.empty-info {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px 0;
}

.customer-details {
  margin-bottom: 20px;
}

.customer-registrations {
  height: calc(100% - 40px);
  display: flex;
  flex-direction: column;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
