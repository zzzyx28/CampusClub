<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import HeaderNav from '@/components/common/HeaderNav.vue';
import { userApi, registrationApi, conversationApi } from '@/api';
import { getCurrentUser, isLoggedIn, logout } from '@/utils/auth';
import { getMemberLevelName } from '@/utils/jwt';

const router = useRouter();
const activeTab = ref('info'); // 默认显示个人信息标签页
const loading = ref(false);
const passwordLoading = ref(false);

// 用户信息
const userInfo = reactive({
  id: '',
  username: '',
  role: '',
  email: '',
  phone: '',
  joinTime: '',
  realName: '',
  idCard: '',
  memberLevel: 1,
  memberLevelName: '新成员'
});

// 是否为普通用户
const isNormalUser = computed(() => userInfo.role === 'user');

// 会员等级对应的类型
const memberLevelType = computed(() => {
  switch (userInfo.memberLevel) {
    case 1:
      return ''; // 普通会员
    case 2:
      return 'info'; // 白银会员
    case 3:
      return 'warning'; // 黄金会员
    case 4:
      return 'success'; // 白金会员
    case 5:
      return 'danger'; // 钻石会员
    default:
      return '';
  }
});

// 会员等级对应的图标
const memberLevelIcon = computed(() => {
  switch (userInfo.memberLevel) {
    case 1:
      return 'User';
    case 2:
      return 'Medal';
    case 3:
      return 'Trophy';
    case 4:
      return 'First';
    case 5:
      return 'Star';
    default:
      return 'User';
  }
});

// 编辑状态
const isEditing = ref(false);

// 暂存编辑中的用户信息
const editingUserInfo = reactive({
  email: '',
  phone: '',
  realName: '',
  idCard: ''
});

// 修改密码对话框
const passwordDialogVisible = ref(false);
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});
const passwordFormRef = ref(null);

// 密码表单验证规则
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};

// 报名数据
const registrations = ref([]);
const conversations = ref([]);
const detailDialogVisible = ref(false);
const detailLoading = ref(false);

const registrationDetail = reactive({
  registrationId: '',
  userId: '',
  username: '',
  realName: '',
  phone: '',
  activityId: '',
  ticketCode: '',
  activityTitle: '',
  clubName: '',
  activityType: '',
  venue: '',
  registrationStart: '',
  eventStart: '',
  eventEnd: '',
  tierName: '',
  unitPrice: 0,
  totalPrice: 0,
  status: 0,
  createdAt: ''
});

// 获取用户信息
const getUserData = async () => {
  try {
    loading.value = true;
    
    // 检查是否登录
    if (!isLoggedIn()) {
      ElMessage.error('用户未登录，请先登录');
      router.push('/login');
      return;
    }
    
    // 从JWT中获取当前用户基本信息
    const currentUser = getCurrentUser();
    if (!currentUser) {
      ElMessage.error('无法获取用户信息，请重新登录');
      router.push('/login');
      return;
    }
    
    // 更新用户基本信息
    Object.assign(userInfo, {
      id: currentUser.id,
      username: currentUser.username,
      role: currentUser.role
    });
    
    // 调用API获取用户详情
    const response = await userApi.getUserInfo(currentUser.id);
    
    if (response && response.code === 200 && response.data) {
      const userData = response.data;
      
      // 更新用户详细信息
      Object.assign(userInfo, {
        email: userData.email || '',
        phone: userData.phone || '',
        realName: userData.realName || '',
        idCard: userData.idCard || '',
        joinTime: userData.createTime || '',
        memberLevel: userData.memberLevel || 1,
        memberLevelName: getMemberLevelName(userData.memberLevel || 1)
      });
      
      // 初始化编辑表单
      Object.assign(editingUserInfo, {
        email: userInfo.email,
        phone: userInfo.phone,
        realName: userInfo.realName,
        idCard: userInfo.idCard
      });
    } else {
      throw new Error(response?.message || '获取用户信息失败');
    }
    
    // 如果是普通用户，获取报名和会话数据
    if (isNormalUser.value) {
      // 获取报名数据
      const regResponse = await registrationApi.listRegistrationsWithDetail(currentUser.id);
      if (regResponse && regResponse.code === 200) {
        registrations.value = (regResponse.data || []).map((reg) => ({
          ...reg,
          statusText: reg.status === 1 ? '已报名' : reg.status === 2 ? '已取消' : reg.status === 3 ? '已参加' : '未知'
        }));
      }
      
      // 获取会话数据
      const conversationResponse = await conversationApi.listConversationsByUserId(currentUser.id);
      if (conversationResponse && conversationResponse.code === 200) {
        conversations.value = conversationResponse.data || [];
      } else {
        console.error('获取会话数据失败:', conversationResponse);
      }
    }
  } catch (error) {
    console.error('获取用户数据失败:', error);
    ElMessage.error(error.message || '获取用户数据失败');
  } finally {
    loading.value = false;
  }
};

// 开始编辑
const startEditing = () => {
  isEditing.value = true;
};

// 保存编辑
const saveEditing = async () => {
  try {
    loading.value = true;
    
    const currentUser = getCurrentUser();
    if (!currentUser) {
      ElMessage.error('用户未登录，请重新登录');
      router.push('/login');
      return;
    }
    
    // 调用API更新用户信息
    const response = await userApi.updateUser(currentUser.id, {
      email: editingUserInfo.email,
      phone: editingUserInfo.phone,
      realName: editingUserInfo.realName,
      idCard: editingUserInfo.idCard
    });
    
    if (response && response.code === 200) {
      // 更新本地用户信息
      userInfo.email = editingUserInfo.email;
      userInfo.phone = editingUserInfo.phone;
      userInfo.realName = editingUserInfo.realName;
      userInfo.idCard = editingUserInfo.idCard;
      
      ElMessage.success(response.message || '资料更新成功');
      isEditing.value = false;
    } else {
      throw new Error(response?.message || '更新用户信息失败');
    }
  } catch (error) {
    console.error('更新用户信息失败:', error);
    ElMessage.error(error.message || '更新用户信息失败');
  } finally {
    loading.value = false;
  }
};

// 取消编辑
const cancelEditing = () => {
  // 重置编辑表单
  editingUserInfo.email = userInfo.email;
  editingUserInfo.phone = userInfo.phone;
  editingUserInfo.realName = userInfo.realName;
  editingUserInfo.idCard = userInfo.idCard;
  isEditing.value = false;
};

// 打开修改密码对话框
const openPasswordDialog = () => {
  // 重置密码表单
  passwordForm.oldPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
  passwordDialogVisible.value = true;
};

// 提交修改密码
const submitPasswordChange = async () => {
  try {
    await passwordFormRef.value.validate();
    
    passwordLoading.value = true;
    
    const currentUser = getCurrentUser();
    if (!currentUser) {
      ElMessage.error('用户未登录，请重新登录');
      return;
    }
    
    // 调用修改密码API
    const response = await userApi.modifyPassword({
      username: currentUser.username,
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    });
    
    // 检查响应状态
    if (response) {
      if (response.code === 200) {
        // 真正成功的情况
        ElMessage.success(response.message || '密码修改成功，请重新登录');
        
        // 关闭对话框
        passwordDialogVisible.value = false;
        
        // 清空token并退出登录
        logout();
        
        // 显示提示信息，延迟跳转到登录页
        setTimeout(() => {
          router.push('/');
        }, 1500);
      } else if (response.code === 401) {
        // 旧密码错误
        ElMessage.error(response.msg || '旧密码错误');
      } else {
        // 其他错误情况
        throw new Error(response.msg || '修改密码失败');
      }
    } else {
      throw new Error('服务器响应异常');
    }
  } catch (error) {
    console.error('修改密码失败:', error);
    ElMessage.error(error.message || '修改密码失败');
  } finally {
    passwordLoading.value = false;
  }
};

const getUserName = (conversation) => {
  return conversation.user ? conversation.user.username : `用户${conversation.userId}`;
};

const getServiceName = (conversation) => {
  return conversation.serviceUser ? conversation.serviceUser.username : 
         conversation.serviceUserName || '社团助手';
};

// 查看报名详情
const viewRegistrationDetail = async (registrationId) => {
  try {
    detailLoading.value = true;
    detailDialogVisible.value = true;
    const response = await registrationApi.getRegistrationDetail(registrationId);
    if (response && response.code === 200 && response.data) {
      Object.assign(registrationDetail, {
        registrationId: response.data.registrationId || registrationId,
        userId: response.data.userId || '',
        username: response.data.username || '',
        realName: response.data.realName || '',
        phone: response.data.phone || '',
        activityId: response.data.activityId || '',
        ticketCode: response.data.ticketCode || '',
        activityTitle: response.data.activityTitle || '',
        clubName: response.data.clubName || '',
        activityType: response.data.activityType || '',
        venue: response.data.venue || '',
        registrationStart: response.data.registrationStart || '',
        eventStart: response.data.eventStart || '',
        eventEnd: response.data.eventEnd || '',
        tierName: response.data.tierName || '',
        unitPrice: response.data.unitPrice ?? 0,
        totalPrice: response.data.totalPrice ?? 0,
        status: response.data.status || 0,
        createdAt: response.data.createdAt || ''
      });
    } else {
      throw new Error(response?.message || '获取报名详情失败');
    }
  } catch (error) {
    ElMessage.error(error.message || '获取报名详情失败');
    detailDialogVisible.value = false;
  } finally {
    detailLoading.value = false;
  }
};

// 会话详情对话框
const dialogVisible = ref(false);
const currentConversation = ref(null);
const conversationMessages = ref([]);
const loadingMessages = ref(false);

// 查看会话详情
const viewConversationDetail = async (conversation) => {
  currentConversation.value = conversation;
  dialogVisible.value = true;
  loadingMessages.value = true;
  
  try {
    // 获取会话消息记录
    const response = await conversationApi.getConversationMessages(conversation.conversationUuid);
    if (response.code === 200) {
      // 适配API返回的数据格式
      if (response.data && response.data.messages) {
        // 使用API返回的messages数组
        conversationMessages.value = response.data.messages.map(msg => ({
          userId: msg.speaker === 'user' ? conversation.userId : null,
          message: msg.text,
          createdAt: msg.timestamp
        }));
      } else {
        conversationMessages.value = [];
      }
    } else {
      throw new Error(response.message || '获取会话详情失败');
    }
  } catch (error) {
    console.error('获取会话详情失败:', error);
    ElMessage.error('获取会话详情失败: ' + (error.message || '未知错误'));
    conversationMessages.value = [];
  } finally {
    loadingMessages.value = false;
  }
};

// 返回上一页
const goBack = () => {
  router.back();
};

// 格式化日期时间
const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return '未知';
  
  try {
    const date = new Date(dateTimeStr);
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    });
  } catch (error) {
    console.error('日期格式化错误:', error);
    return dateTimeStr;
  }
};

// 获取报名状态名称
const getStatusName = (status) => {
  switch (status) {
    case 1:
      return '已报名';
    case 2:
      return '已取消';
    case 3:
      return '已参加';
    default:
      return '未知状态';
  }
};

// 获取报名状态对应的标签类型
const getStatusType = (status) => {
  switch (status) {
    case 1:
      return 'success';
    case 2:
      return 'danger';
    default:
      return 'info';
  }
};

// 组件挂载时获取用户数据
onMounted(() => {
  getUserData();
});
</script>

<template>
  <div class="profile-container">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="back-button" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </div>
      <div class="page-title">个人中心</div>
      <div class="placeholder"></div>
    </header>
    
    <!-- 个人资料卡片 -->
    <div class="profile-content">
      <el-card class="profile-card" v-loading="loading">
        <div class="profile-header">
          <el-avatar :size="80" icon="User" />
          <div class="profile-title">
            <h2>{{ userInfo.username }}</h2>
            <div class="user-tags">
              <el-tag>{{ userInfo.role }}</el-tag>
              <el-tag 
                :type="memberLevelType" 
                class="member-level-tag"
                :effect="userInfo.memberLevel > 2 ? 'dark' : 'light'"
              >
                <el-icon class="member-icon"><component :is="memberLevelIcon" /></el-icon>
                {{ userInfo.memberLevelName }}
              </el-tag>
            </div>
          </div>
        </div>
        
        <el-divider />
        
        <!-- 标签页切换 -->
        <el-tabs v-model="activeTab" class="profile-tabs">
          <!-- 个人信息标签页 - 所有用户都有 -->
          <el-tab-pane label="个人信息" name="info">
            <div class="profile-data-container">
              <!-- 查看模式 -->
              <div v-if="!isEditing" class="profile-info">
                <div class="info-row">
                  <div class="info-label">用户名</div>
                  <div class="info-value">{{ userInfo.username }}</div>
                </div>
                <div class="info-row">
                  <div class="info-label">邮箱</div>
                  <div class="info-value">{{ userInfo.email }}</div>
                </div>
                <div class="info-row">
                  <div class="info-label">电话</div>
                  <div class="info-value">{{ userInfo.phone }}</div>
                </div>
                <div class="info-row">
                  <div class="info-label">真实姓名</div>
                  <div class="info-value">{{ userInfo.realName }}</div>
                </div>
                <div class="info-row">
                  <div class="info-label">身份证号</div>
                  <div class="info-value">{{ userInfo.idCard }}</div>
                </div>
                <div class="info-row">
                  <div class="info-label">注册时间</div>
                  <div class="info-value">{{ userInfo.joinTime }}</div>
                </div>
                <div class="info-row">
                  <div class="info-label">社团活跃度</div>
                  <div class="info-value">
                    <el-tag 
                      :type="memberLevelType" 
                      class="member-level-tag"
                      :effect="userInfo.memberLevel > 2 ? 'dark' : 'light'"
                    >
                      <el-icon class="member-icon"><component :is="memberLevelIcon" /></el-icon>
                      {{ userInfo.memberLevelName }}
                    </el-tag>
                    <div class="member-benefits">
                      <span v-if="userInfo.memberLevel >= 2">{{ 10 - userInfo.memberLevel }}折</span>
                      <span v-if="userInfo.memberLevel >= 2">活动提醒</span>
                      <span v-if="userInfo.memberLevel >= 3">优先报名</span>
                      <span v-if="userInfo.memberLevel >= 4">专属咨询</span>
                      <span v-if="userInfo.memberLevel >= 5">专属客服</span>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 编辑模式 -->
              <div v-else class="profile-edit">
                <div class="info-row">
                  <div class="info-label">用户名</div>
                  <div class="info-value readonly">{{ userInfo.username }}</div>
                </div>
                <div class="info-row">
                  <div class="info-label">邮箱</div>
                  <div class="info-value">
                    <el-input v-model="editingUserInfo.email" />
                  </div>
                </div>
                <div class="info-row">
                  <div class="info-label">电话</div>
                  <div class="info-value">
                    <el-input v-model="editingUserInfo.phone" />
                  </div>
                </div>
                <div class="info-row">
                  <div class="info-label">真实姓名</div>
                  <div class="info-value">
                    <el-input v-model="editingUserInfo.realName" />
                  </div>
                </div>
                <div class="info-row">
                  <div class="info-label">身份证号</div>
                  <div class="info-value">
                    <el-input v-model="editingUserInfo.idCard" />
                  </div>
                </div>
                <div class="info-row">
                  <div class="info-label">注册时间</div>
                  <div class="info-value readonly">{{ userInfo.joinTime }}</div>
                </div>
                <div class="info-row">
                  <div class="info-label">社团活跃度</div>
                  <div class="info-value readonly">
                    <el-tag 
                      :type="memberLevelType" 
                      class="member-level-tag"
                      :effect="userInfo.memberLevel > 2 ? 'dark' : 'light'"
                    >
                      <el-icon class="member-icon"><component :is="memberLevelIcon" /></el-icon>
                      {{ userInfo.memberLevelName }}
                    </el-tag>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="profile-actions">
              <template v-if="!isEditing">
                <el-button type="primary" @click="startEditing">修改资料</el-button>
                <el-button @click="openPasswordDialog">修改密码</el-button>
              </template>
              <template v-else>
                <el-button type="primary" @click="saveEditing" :loading="loading">保存</el-button>
                <el-button @click="cancelEditing">取消</el-button>
              </template>
            </div>
          </el-tab-pane>
          
          <el-tab-pane label="我的报名" name="registrations" v-if="isNormalUser">
            <div class="registrations-container">
              <el-table :data="registrations" style="width: 100%">
                <el-table-column prop="id" label="报名号" min-width="120" />
                <el-table-column prop="activityId" label="活动ID" min-width="120" />
                <el-table-column prop="statusText" label="状态" min-width="120">
                  <template #default="scope">
                    <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.statusText }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="报名时间" min-width="180" />
                <el-table-column label="操作" min-width="120" fixed="right">
                  <template #default="scope">
                    <el-button size="small" @click="viewRegistrationDetail(scope.row.id)">查看详情</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
          
          <!-- 历史会话标签页 - 只有普通用户才有 -->
          <el-tab-pane label="历史会话" name="conversations" v-if="isNormalUser">
            <div class="conversations-container">
              <el-table :data="conversations" style="width: 100%">
                <el-table-column prop="serviceUserName" label="社团助手" min-width="120" />
                <el-table-column prop="lastMessage" label="最后消息" min-width="200" show-overflow-tooltip />
                <el-table-column prop="updatedAt" label="时间" min-width="160" />
                <el-table-column prop="status" label="状态" min-width="120">
                  <template #default="scope">
                    <el-tag type="info">{{ scope.row.status === 0 ? '进行中' : 
                                          scope.row.status === 1 ? '已结束' :
                                          scope.row.status === 2 ? '已转接' : '未知' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" min-width="120" fixed="right">
                  <template #default="scope">
                    <el-button size="small" @click="viewConversationDetail(scope.row)">查看详情</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
    
    <!-- 修改密码对话框 -->
    <el-dialog
      title="修改密码"
      v-model="passwordDialogVisible"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
      >
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
            placeholder="请输入当前密码"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            placeholder="请输入新密码"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitPasswordChange" :loading="passwordLoading">确认</el-button>
        </span>
      </template>
    </el-dialog>
    
    <el-dialog v-model="detailDialogVisible" title="报名详情" width="650px" destroy-on-close>
      <div v-loading="detailLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="报名ID" :span="2">{{ registrationDetail.registrationId }}</el-descriptions-item>
          <el-descriptions-item label="票档">{{ registrationDetail.tierName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(registrationDetail.status)">{{ getStatusName(registrationDetail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="报名时间" :span="2">{{ formatDateTime(registrationDetail.createdAt) }}</el-descriptions-item>
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
        </span>
      </template>
    </el-dialog>

    <!-- 会话详情对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      title="会话详情" 
      width="700px"
      destroy-on-close
    >
      <div v-loading="loadingMessages" class="conversation-detail">
        <div v-if="currentConversation" class="conversation-info">
          <p><strong>会话ID:</strong> {{ currentConversation.conversationUuid }}</p>
          <p><strong>用户:</strong> {{ getUserName(currentConversation) }}</p>
          <p><strong>客服:</strong> {{ getServiceName(currentConversation) }}</p>
          <p><strong>状态:</strong> {{ getStatusName(currentConversation.status) }}</p>
          <p><strong>更新时间:</strong> {{ formatDateTime(currentConversation.updatedAt) }}</p>
        </div>
        
        <el-divider content-position="center">会话记录</el-divider>
        
        <div class="conversation-messages">
          <div 
            v-for="(message, index) in conversationMessages" 
            :key="index"
            class="message-item"
            :class="{ 
              'user-message': message.userId === currentConversation.userId,
              'service-message': message.userId !== currentConversation.userId
            }"
          >
            <div class="message-header">
              <span class="message-sender">
                {{ message.userId === currentConversation.userId ? getUserName(currentConversation) : getServiceName(currentConversation) }}
              </span>
              <span class="message-time">{{ formatDateTime(message.createdAt) }}</span>
            </div>
            <div class="message-content">{{ message.message }}</div>
          </div>
          
          <div v-if="conversationMessages.length === 0" class="no-messages">
            暂无会话记录
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  background: var(--cc-paper);
  background-image:
    radial-gradient(ellipse 70% 50% at 50% 0%, rgba(200, 75, 49, 0.06), transparent 55%);
  box-sizing: border-box;
  position: absolute;
  inset: 0;
}

.header {
  height: var(--cc-header-h, 60px);
  background: var(--cc-surface);
  color: var(--cc-ink);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1.5rem;
  border-bottom: 1px solid var(--cc-border);
  box-shadow: var(--sa-shadow-sm);
  width: 100%;
  flex-shrink: 0;
}

.back-button:hover {
  background-color: var(--cc-paper);
}

.page-title {
  font-family: var(--sa-font-heading);
  font-size: 1.125rem;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--cc-ink);
}

.placeholder {
  width: 72px;
}

.profile-content {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 40px 20px;
  width: 100%;
  box-sizing: border-box;
  overflow: auto;
}

.profile-card {
  width: 100%;
  max-width: 900px;
  border-radius: var(--sa-radius-lg, 16px) !important;
  box-shadow: var(--sa-shadow-card, 0 4px 24px rgba(30, 58, 138, 0.07)) !important;
  border: 1px solid var(--sa-border, #e2e8f0) !important;
}

.profile-header {
  display: flex;
  align-items: center;
}

.profile-title {
  margin-left: 20px;
}

.profile-title h2 {
  margin: 0 0 5px 0;
}

.user-tags {
  display: flex;
  gap: 10px;
}

.member-level-tag {
  display: flex;
  align-items: center;
  font-weight: bold;
  padding: 0 10px;
}

.member-level-tag.el-tag--info {
  background-color: #e9e9eb;
  border-color: #d3d4d6;
}

.member-level-tag.el-tag--warning {
  background-color: #faecd8;
  border-color: #e6a23c;
  color: #b88230;
}

.member-level-tag.el-tag--success {
  background-color: #f0f9eb;
  border-color: #85ce61;
  color: #529b2e;
}

.member-level-tag.el-tag--danger {
  background-color: #fef0f0;
  border-color: #f56c6c;
  color: #c45656;
}

.member-level-tag.el-tag--warning.el-tag--dark {
  background-color: #e6a23c;
  border-color: #e6a23c;
  color: #ffffff;
}

.member-level-tag.el-tag--success.el-tag--dark {
  background-color: #67c23a;
  border-color: #67c23a;
  color: #ffffff;
}

.member-level-tag.el-tag--danger.el-tag--dark {
  background-color: #f56c6c;
  border-color: #f56c6c;
  color: #ffffff;
}

.member-icon {
  margin-right: 5px;
}

.member-benefits {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.member-benefits span {
  font-size: 12px;
  color: var(--sa-text-muted, #475569);
  background-color: var(--cc-accent-soft);
  padding: 2px 8px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
}

.profile-tabs {
  margin-top: 20px;
}

.profile-data-container {
  min-height: 240px;
  margin: 20px 0;
}

.profile-info,
.profile-edit {
  display: flex;
  flex-direction: column;
}

.info-row {
  display: flex;
  min-height: 50px;
  margin-bottom: 10px;
  align-items: flex-start;
  padding: 8px 0;
}

.info-label {
  width: 100px;
  color: var(--sa-text-muted, #475569);
  flex-shrink: 0;
  padding-top: 8px;
}

.info-value {
  flex: 1;
  color: var(--sa-text, #0f172a);
}

.info-value .el-input {
  width: 100%;
}

.readonly {
  line-height: 40px;
  color: #909399;
}

.profile-actions {
  display: flex;
  justify-content: flex-start;
  gap: 15px;
  margin-top: 20px;
}

.registrations-container,
.conversations-container {
  margin: 20px 0;
  width: 100%;
  overflow-x: auto;
}

/* 确保表格占满容器 */
.registrations-container .el-table,
.conversations-container .el-table {
  width: 100% !important;
}

/* 表格行样式 */
.el-table .el-table__row {
  cursor: pointer;
  transition: background-color 0.3s;
}

.el-table .el-table__row:hover {
  background-color: var(--cc-paper);
}

.conversation-detail {
  min-height: 300px;
  max-height: 500px;
  overflow-y: auto;
}

.conversation-info {
  margin-bottom: 20px;
}

.conversation-messages {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.message-item {
  padding: 10px;
  border-radius: 8px;
  max-width: 80%;
}

.user-message {
  align-self: flex-end;
  background-color: var(--cc-accent-soft);
  border: 1px solid rgba(37, 99, 235, 0.2);
}

.service-message {
  align-self: flex-start;
  background-color: var(--sa-bg-elevated, #f8fafc);
  border: 1px solid var(--sa-border, #e2e8f0);
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
  font-size: 12px;
  color: #666;
}

.message-content {
  word-break: break-word;
}

.no-messages {
  text-align: center;
  color: #999;
  padding: 20px;
}

:deep(.el-descriptions__label) {
  width: 120px;
  font-weight: bold;
}

:deep(.el-descriptions__content) {
  line-height: 1.5;
}

:deep(.el-descriptions__body .el-descriptions__table .el-descriptions__cell) {
  padding: 12px 10px;
}

@media (max-width: 768px) {
  .profile-card {
    width: 100%;
  }
  
  .info-row {
    flex-direction: column;
  }
  
  .info-label {
    width: 100%;
    margin-bottom: 5px;
  }
}
</style> 