<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getCurrentUser, logout, isLoggedIn } from '@/utils/auth';

const router = useRouter();
const userInfo = ref(null);

// 初始化获取用户信息
onMounted(() => {
  checkLoginStatus();
});

// 检查登录状态
const checkLoginStatus = () => {
  if (!isLoggedIn()) {
    userInfo.value = null;
    return;
  }
  
  // 获取当前登录用户信息
  userInfo.value = getCurrentUser();
};

// 处理退出登录
const handleLogout = () => {
  ElMessageBox.confirm(
    '确定要退出登录吗？',
    '提示',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // 调用退出登录函数
    logout();
    
    // 提示用户退出成功
    ElMessage.success('退出登录成功');
    
    // 重置用户信息
    userInfo.value = null;
    
    // 重定向到登录页
    router.push('/login');
  }).catch(() => {
    // 用户取消退出
  });
};

// 返回首页
const goHome = () => {
  router.push('/');
};

// 转到个人中心
const goToProfile = () => {
  router.push('/profile');
};

// 转到登录页
const goToLogin = () => {
  router.push('/login');
};
</script>

<template>
  <header class="header">
    <div class="logo" @click="goHome">
      <img src="@/assets/logo.png" alt="SmartShop" class="logo-img" />
      <span class="logo-text">SmartShop 智能电商</span>
    </div>
    
    <div class="user-area">
      <template v-if="userInfo">
        <div class="user-info-dropdown">
          <el-dropdown trigger="click">
            <span class="user-info-btn">
              <el-avatar :size="30" icon="User" />
              <span class="username">{{ userInfo.username }}</span>
              <el-icon><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goToProfile">
                  <el-icon><user /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout" class="logout-item">
                  <el-icon class="logout-icon"><switch-button /></el-icon>
                  <span class="logout-text">退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </template>
      <template v-else>
        <el-button class="login-cta" round @click="goToLogin">登录 / 注册</el-button>
      </template>
    </div>
  </header>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 28px;
  background: var(--sa-header-gradient, linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%));
  color: #fff;
  box-shadow: 0 4px 24px rgba(30, 58, 138, 0.25);
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  flex-shrink: 0;
}

.logo {
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 12px;
  transition: opacity 0.2s ease;
}

.logo:hover {
  opacity: 0.92;
}

.logo-img {
  height: 36px;
  margin-right: 0;
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.15));
}

.logo-text {
  font-family: var(--sa-font-heading, 'Poppins', sans-serif);
  font-size: 1.125rem;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.menu {
  flex: 1;
  display: flex;
  justify-content: center;
}

:deep(.el-menu--horizontal) {
  border-bottom: none;
}

:deep(.el-menu--horizontal > .el-menu-item) {
  height: 64px;
  line-height: 64px;
  border-bottom: none;
  color: white;
}

:deep(.el-menu--horizontal > .el-menu-item.is-active) {
  border-bottom: 2px solid #fbbf24;
  color: #fbbf24;
}

.user-area {
  display: flex;
  align-items: center;
}

.user-info-btn {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: white;
}

.username {
  margin: 0 8px;
}

/* 退出登录按钮样式 */
:deep(.logout-item) {
  color: #f56c6c !important;
}

:deep(.logout-item:hover) {
  background-color: #fef0f0 !important;
}

.logout-icon {
  color: #f56c6c !important;
}

.logout-text {
  color: #f56c6c !important;
}

.login-cta {
  cursor: pointer;
  background: #fff !important;
  color: var(--sa-primary-bright, #2563eb) !important;
  border: none !important;
  font-weight: 600;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
}

.login-cta:hover {
  background: #f8fafc !important;
  color: var(--sa-primary, #1e3a8a) !important;
}
</style>
