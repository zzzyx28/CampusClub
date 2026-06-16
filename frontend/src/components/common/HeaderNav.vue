<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getCurrentUser, logout, isLoggedIn } from '@/utils/auth';
import logoUrl from '@/assets/logo.svg';

const router = useRouter();
const userInfo = ref(null);

onMounted(() => {
  checkLoginStatus();
});

const checkLoginStatus = () => {
  if (!isLoggedIn()) {
    userInfo.value = null;
    return;
  }
  userInfo.value = getCurrentUser();
};

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      logout();
      ElMessage.success('退出登录成功');
      userInfo.value = null;
      router.push('/login');
    })
    .catch(() => {});
};

const goHome = () => {
  const user = getCurrentUser();
  if (user?.role === 'admin') router.push('/admin');
  else if (user?.role === 'service') router.push('/service');
  else if (user?.role === 'user') router.push('/user');
  else router.push('/login');
};

const goToProfile = () => router.push('/profile');
const goToLogin = () => router.push('/login');
</script>

<template>
  <header class="header">
    <div class="logo" @click="goHome">
      <img :src="logoUrl" alt="CampusClub" class="logo-img" />
      <div class="logo-text-group">
        <span class="logo-text">CampusClub</span>
        <span class="logo-sub">校园社团助手</span>
      </div>
    </div>

    <div class="user-area">
      <template v-if="userInfo">
        <el-dropdown trigger="click">
          <span class="user-info-btn">
            <el-avatar :size="32" class="user-avatar">{{ userInfo.username?.[0]?.toUpperCase() }}</el-avatar>
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
      </template>
      <template v-else>
        <el-button class="login-cta" @click="goToLogin">登录 / 注册</el-button>
      </template>
    </div>
  </header>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--cc-header-h, 60px);
  padding: 0 1.5rem;
  background: var(--cc-surface);
  color: var(--cc-ink);
  border-bottom: 1px solid var(--cc-border);
  box-shadow: var(--sa-shadow-sm);
  flex-shrink: 0;
  position: relative;
  z-index: 100;
}

.header::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 1.5rem;
  width: 3rem;
  height: 2px;
  background: var(--cc-accent);
  border-radius: 1px;
}

.logo {
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 0.75rem;
  transition: opacity 0.2s ease;
}

.logo:hover {
  opacity: 0.85;
}

.logo-img {
  height: 36px;
  width: 36px;
}

.logo-text-group {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.logo-text {
  font-family: var(--sa-font-heading);
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--cc-ink);
  letter-spacing: -0.02em;
}

.logo-sub {
  font-size: 0.7rem;
  font-weight: 500;
  color: var(--cc-muted);
  letter-spacing: 0.04em;
}

.user-area {
  display: flex;
  align-items: center;
}

.user-info-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  color: var(--cc-ink);
  padding: 0.35rem 0.6rem;
  border-radius: var(--sa-radius-sm);
  transition: background-color 0.2s ease;
}

.user-info-btn:hover {
  background: var(--cc-paper);
}

.user-avatar {
  background: var(--cc-green) !important;
  color: #fff !important;
  font-weight: 700;
  font-size: 14px;
}

.username {
  font-weight: 600;
  font-size: 0.9rem;
}

:deep(.logout-item) {
  color: var(--cc-accent) !important;
}

:deep(.logout-item:hover) {
  background-color: var(--cc-accent-soft) !important;
}

.logout-icon,
.logout-text {
  color: var(--cc-accent) !important;
}

.login-cta {
  background: var(--cc-accent) !important;
  color: #fff !important;
  border: none !important;
  font-weight: 600;
  padding: 0.5rem 1.25rem;
  border-radius: var(--sa-radius-sm) !important;
}

.login-cta:hover {
  background: var(--cc-accent-hover) !important;
  color: #fff !important;
}
</style>
