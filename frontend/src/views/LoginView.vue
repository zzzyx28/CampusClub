<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
import { setToken } from '@/utils/auth'
import logoUrl from '@/assets/logo.svg'

const router = useRouter()
const loading = ref(false)

// 表单数据
const loginForm = reactive({
  username: '',
  password: '',
  role: 'user', // 默认角色为用户
})

// 角色选项
const roleOptions = [
  { label: '用户', value: 'user' },
  { label: '社团助手', value: 'service' },
  { label: '管理员', value: 'admin' },
]

// 登录方法
const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  try {
    // 设置加载状态
    loading.value = true

    // 调用登录API
    const response = await authApi.login({
      username: loginForm.username,
      password: loginForm.password,
      role: loginForm.role,
    })

    // 登录成功，保存token
    const token = response.data
    if (!token) {
      throw new Error('登录失败，未获取到有效的token')
    }

    // 只保存token，不保存用户信息
    setToken(token)

    // 提示成功
    ElMessage.success('登录成功')

    // 根据选择的角色跳转到不同页面
    if (loginForm.role === 'user') {
      router.push('/user')
    } else if (loginForm.role === 'service') {
      router.push('/service')
    } else if (loginForm.role === 'admin') {
      router.push('/admin')
    }
  } catch (error) {
    // 显示错误消息
    console.error('登录失败:', error)
    ElMessage.error(error.message || '登录失败，请检查用户名和密码')
  } finally {
    // 无论成功失败，都取消加载状态
    loading.value = false
  }
}

// 跳转到注册页面
const goToRegister = () => {
  router.push('/register')
}

// 跳转到忘记密码页面
const goToForgotPassword = () => {
  router.push('/forgot-password')
}
</script>

<template>
  <div class="login-container cc-page-bg">
    <div class="login-box">
      <div class="role-selector">
        <p class="role-eyebrow">选择身份</p>
        <h2>欢迎回来</h2>
        <p class="role-hint">不同角色将进入对应工作台</p>
        <div class="role-options">
          <div
            v-for="role in roleOptions"
            :key="role.value"
            class="role-option"
            :class="{ active: loginForm.role === role.value }"
            @click="loginForm.role = role.value"
          >
            <el-icon v-if="role.value === 'user'" class="role-icon"><User /></el-icon>
            <el-icon v-if="role.value === 'service'" class="role-icon"><Service /></el-icon>
            <el-icon v-if="role.value === 'admin'" class="role-icon"><Setting /></el-icon>
            {{ role.label }}
          </div>
        </div>
      </div>

      <div class="login-form">
        <div class="login-content">
          <div class="brand-row">
            <img :src="logoUrl" alt="Logo" class="brand-logo" />
            <div>
              <h2>CampusClub</h2>
              <p class="brand-tagline">发现活动 · 智能报名 · 社团协作</p>
            </div>
          </div>

          <el-form>
            <el-form-item class="form-item">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" />
            </el-form-item>

            <el-form-item class="form-item">
              <el-input
                v-model="loginForm.password"
                placeholder="请输入密码"
                prefix-icon="Lock"
                type="password"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-form-item class="form-item">
              <el-button type="primary" @click="handleLogin" class="login-button" :loading="loading"
                >登录</el-button
              >
            </el-form-item>

            <div class="login-options">
              <el-link type="primary" @click="goToRegister">注册账号</el-link>
              <el-link type="primary" @click="goToForgotPassword">忘记密码</el-link>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
  position: absolute;
  inset: 0;
  padding: 1.5rem;
}

.login-box {
  width: min(920px, 96%);
  min-height: 520px;
  display: flex;
  border-radius: var(--sa-radius-lg);
  overflow: hidden;
  box-shadow: var(--sa-shadow-md);
  border: 1px solid var(--cc-border);
}

.role-selector {
  width: 320px;
  flex-shrink: 0;
  background: linear-gradient(160deg, #c84b31 0%, #a83d28 55%, #2f5d4e 100%);
  color: #fff;
  padding: 2.5rem 1.75rem;
  display: flex;
  flex-direction: column;
}

.role-eyebrow {
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  opacity: 0.85;
  margin-bottom: 0.5rem;
}

.role-selector h2 {
  margin-bottom: 0.5rem;
  font-family: var(--sa-font-heading);
  font-weight: 700;
  font-size: 1.75rem;
  color: #fff;
  letter-spacing: -0.02em;
}

.role-hint {
  font-size: 0.875rem;
  opacity: 0.88;
  margin-bottom: 2rem;
  line-height: 1.5;
}

.role-options {
  display: flex;
  flex-direction: column;
  width: 100%;
  gap: 0.6rem;
}

.role-option {
  padding: 0.85rem 1rem;
  border-radius: var(--sa-radius-sm);
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 500;
  transition: var(--sa-transition);
  display: flex;
  align-items: center;
  gap: 0.65rem;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.06);
}

.role-icon {
  font-size: 18px;
}

.role-option:hover {
  background: rgba(255, 255, 255, 0.14);
  transform: translateX(3px);
}

.role-option.active {
  background: var(--cc-surface);
  color: var(--cc-accent);
  font-weight: 600;
  border-color: transparent;
  box-shadow: var(--sa-shadow-md);
}

.login-form {
  flex: 1;
  background: var(--cc-surface);
  padding: 2.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.login-content {
  width: 100%;
  max-width: 360px;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 2.5rem;
}

.brand-logo {
  height: 48px;
  width: 48px;
}

.brand-row h2 {
  font-family: var(--sa-font-heading);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--cc-ink);
  margin: 0;
}

.brand-tagline {
  font-size: 0.8rem;
  color: var(--cc-muted);
  margin-top: 0.25rem;
}

.login-form :deep(.el-form) {
  width: 100%;
}

.form-item {
  margin-bottom: 1.25rem;
}

.login-button {
  width: 100%;
  height: 48px;
  font-size: 1rem;
  margin-top: 0.5rem;
}

.login-options {
  display: flex;
  justify-content: space-between;
  margin-top: 1.25rem;
  font-size: 0.9rem;
}

@media (max-width: 768px) {
  .login-box {
    flex-direction: column;
    min-height: auto;
  }

  .role-selector {
    width: 100%;
    padding: 1.5rem;
  }

  .role-options {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .role-option {
    flex: 1;
    min-width: 90px;
    justify-content: center;
  }

  .login-form {
    padding: 1.5rem;
  }
}
</style>
