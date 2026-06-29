<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LockOutlined, UserOutlined } from '@ant-design/icons-vue'
import axios from 'axios'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import loginBg from '@/assets/auth/login-bg.png'
import loginLogo from '@/assets/auth/login-logo.png'
import loginDecoTech from '@/assets/auth/login-deco-tech.png'
import { useAuth } from '@/composables/useAuth'
import { forgotPassword } from '@/api/auth'
import { getErrorMessage } from '@/api/http'
import {
  clearRememberedUsername,
  loadRememberedUsername,
  saveRememberedUsername,
} from '@/utils/loginRemember'

type AuthTab = 'login' | 'register' | 'forgot'

const route = useRoute()
const router = useRouter()
const auth = useAuth()

function resolveTabFromQuery(tab: unknown): AuthTab {
  if (tab === 'register') {
    return 'register'
  }
  if (tab === 'forgot') {
    return 'forgot'
  }
  return 'login'
}

const activeTab = ref<AuthTab>(resolveTabFromQuery(route.query.tab))
const submitting = ref(false)
const rememberPassword = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
})

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  displayName: '',
  email: '',
})

const forgotForm = reactive({
  username: '',
  email: '',
  newPassword: '',
  confirmPassword: '',
})

const panelHeading = computed(() => {
  if (activeTab.value === 'register') {
    return '欢迎注册'
  }
  if (activeTab.value === 'forgot') {
    return '重置密码'
  }
  return '欢迎登录'
})

const loginRules: Record<string, Rule[]> = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const registerRules = computed<Record<string, Rule[]>>(() => ({
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 32, message: '账号长度为 3-32 个字符', trigger: 'blur' },
  ],
  displayName: [
    { required: true, message: '请输入显示名称', trigger: 'blur' },
    { max: 64, message: '显示名称不能超过 64 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度为 6-64 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: async (_rule, value) => {
        if (value !== registerForm.password) {
          throw new Error('两次输入的密码不一致')
        }
      },
      trigger: 'blur',
    },
  ],
  email: [
    {
      validator: async (_rule, value: string) => {
        const trimmed = value?.trim()
        if (!trimmed) {
          return
        }
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) {
          throw new Error('邮箱格式不正确')
        }
      },
      trigger: 'blur',
    },
  ],
}))

const forgotRules = computed<Record<string, Rule[]>>(() => ({
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度为 6-64 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: async (_rule, value) => {
        if (value !== forgotForm.newPassword) {
          throw new Error('两次输入的密码不一致')
        }
      },
      trigger: 'blur',
    },
  ],
}))

const loginCanSubmit = computed(
  () => loginForm.username.trim().length > 0 && loginForm.password.length > 0,
)

const registerCanSubmit = computed(() => {
  const username = registerForm.username.trim()
  const displayName = registerForm.displayName.trim()
  return (
    username.length >= 3
    && username.length <= 32
    && displayName.length > 0
    && displayName.length <= 64
    && registerForm.password.length >= 6
    && registerForm.password.length <= 64
    && registerForm.confirmPassword === registerForm.password
  )
})

const forgotCanSubmit = computed(() => (
  forgotForm.username.trim().length > 0
  && forgotForm.email.trim().length > 0
  && forgotForm.newPassword.length >= 6
  && forgotForm.confirmPassword === forgotForm.newPassword
))

function syncTabToUrl(tab: AuthTab) {
  router.replace({
    query: {
      ...route.query,
      tab,
    },
  })
}

function switchTab(tab: AuthTab) {
  activeTab.value = tab
  syncTabToUrl(tab)
}

watch(
  () => route.query.tab,
  (tab) => {
    activeTab.value = resolveTabFromQuery(tab)
  },
)

onMounted(() => {
  const remembered = loadRememberedUsername()
  if (remembered) {
    loginForm.username = remembered
    rememberPassword.value = true
  }
})

async function handleForgotPassword() {
  submitting.value = true
  try {
    await forgotPassword({
      username: forgotForm.username.trim(),
      email: forgotForm.email.trim(),
      newPassword: forgotForm.newPassword,
    })
    message.success('密码已重置，请使用新密码登录')
    forgotForm.newPassword = ''
    forgotForm.confirmPassword = ''
    switchTab('login')
  } catch (error) {
    message.error(getErrorMessage(error, '重置密码失败'))
  } finally {
    submitting.value = false
  }
}

function getLoginErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    if (!error.response) {
      return '无法连接后端服务，请确认 backend 已在 8080 端口启动'
    }
    if (error.response.status >= 500) {
      return '后端服务未就绪，请稍候片刻或查看 backend 终端窗口'
    }
  }
  return getErrorMessage(error, '登录失败，请检查账号和密码')
}

async function handleLogin() {
  submitting.value = true
  try {
    await auth.login({
      username: loginForm.username.trim(),
      password: loginForm.password,
    })
    if (rememberPassword.value) {
      saveRememberedUsername(loginForm.username)
    } else {
      clearRememberedUsername()
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/warehouse/material-ledger'
    await router.replace(redirect)
  } catch (error) {
    loginForm.password = ''
    message.error(getLoginErrorMessage(error))
  } finally {
    submitting.value = false
  }
}

async function handleRegister() {
  submitting.value = true
  try {
    await auth.register({
      username: registerForm.username.trim(),
      password: registerForm.password,
      displayName: registerForm.displayName.trim(),
      ...(registerForm.email.trim() ? { email: registerForm.email.trim() } : {}),
    })
    message.success('注册成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/warehouse/material-ledger'
    await router.replace(redirect)
  } catch (error) {
    message.error(getErrorMessage(error, '注册失败'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page" :style="{ backgroundImage: `url(${loginBg})` }">
    <div class="login-card">
      <section class="login-brand">
        <img class="login-brand__logo" :src="loginLogo" alt="项目管理平台 Logo" />
        <h1 class="login-brand__title">项目管理平台</h1>
        <p class="login-brand__subtitle">Project Management Platform</p>
        <img class="login-brand__deco" :src="loginDecoTech" alt="" aria-hidden="true" />
      </section>

      <section class="login-form-panel">
        <h2 class="login-form-panel__heading">{{ panelHeading }}</h2>

        <div v-if="activeTab !== 'forgot'" class="login-tabs" role="tablist" aria-label="登录方式">
          <button
            type="button"
            class="login-tabs__item"
            :class="{ 'login-tabs__item--active': activeTab === 'register' }"
            role="tab"
            :aria-selected="activeTab === 'register'"
            @click="switchTab('register')"
          >
            注册用户
          </button>
          <button
            type="button"
            class="login-tabs__item"
            :class="{ 'login-tabs__item--active': activeTab === 'login' }"
            role="tab"
            :aria-selected="activeTab === 'login'"
            @click="switchTab('login')"
          >
            登录账号
          </button>
        </div>

        <a-form
          v-if="activeTab === 'login'"
          class="login-form"
          layout="vertical"
          :model="loginForm"
          :rules="loginRules"
          @finish="handleLogin"
        >
          <a-form-item name="username">
            <a-input v-model:value="loginForm.username" size="large" placeholder="请输入账号" autocomplete="username">
              <template #prefix><UserOutlined class="login-form__icon" /></template>
            </a-input>
          </a-form-item>
          <a-form-item name="password">
            <a-input-password v-model:value="loginForm.password" size="large" placeholder="请输入密码" autocomplete="current-password">
              <template #prefix><LockOutlined class="login-form__icon" /></template>
            </a-input-password>
          </a-form-item>
          <div class="login-form__options">
            <a-checkbox v-model:checked="rememberPassword">记住密码</a-checkbox>
            <a-button type="link" class="login-form__forgot" @click="switchTab('forgot')">忘记密码</a-button>
          </div>
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            block
            class="login-form__submit"
            :loading="submitting"
            :disabled="!loginCanSubmit"
          >
            登录
          </a-button>
        </a-form>

        <a-form
          v-else-if="activeTab === 'forgot'"
          class="login-form"
          layout="vertical"
          :model="forgotForm"
          :rules="forgotRules"
          @finish="handleForgotPassword"
        >
          <a-form-item name="username">
            <a-input v-model:value="forgotForm.username" size="large" placeholder="请输入账号" autocomplete="username">
              <template #prefix><UserOutlined class="login-form__icon" /></template>
            </a-input>
          </a-form-item>
          <a-form-item name="email">
            <a-input v-model:value="forgotForm.email" size="large" placeholder="请输入注册邮箱" autocomplete="email">
              <template #prefix><UserOutlined class="login-form__icon" /></template>
            </a-input>
          </a-form-item>
          <a-form-item name="newPassword">
            <a-input-password v-model:value="forgotForm.newPassword" size="large" placeholder="请输入新密码（至少 6 位）" autocomplete="new-password">
              <template #prefix><LockOutlined class="login-form__icon" /></template>
            </a-input-password>
          </a-form-item>
          <a-form-item name="confirmPassword">
            <a-input-password v-model:value="forgotForm.confirmPassword" size="large" placeholder="请再次输入新密码" autocomplete="new-password">
              <template #prefix><LockOutlined class="login-form__icon" /></template>
            </a-input-password>
          </a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            block
            class="login-form__submit"
            :loading="submitting"
            :disabled="!forgotCanSubmit"
          >
            重置密码
          </a-button>
        </a-form>

        <a-form
          v-else
          class="login-form"
          layout="vertical"
          :model="registerForm"
          :rules="registerRules"
          @finish="handleRegister"
        >
          <a-form-item name="username">
            <a-input v-model:value="registerForm.username" size="large" placeholder="请输入账号（3-32 个字符）" autocomplete="username">
              <template #prefix><UserOutlined class="login-form__icon" /></template>
            </a-input>
          </a-form-item>
          <a-form-item name="displayName">
            <a-input v-model:value="registerForm.displayName" size="large" placeholder="请输入显示名称">
              <template #prefix><UserOutlined class="login-form__icon" /></template>
            </a-input>
          </a-form-item>
          <a-form-item name="email">
            <a-input v-model:value="registerForm.email" size="large" placeholder="邮箱（选填，用于找回密码）" autocomplete="email">
              <template #prefix><UserOutlined class="login-form__icon" /></template>
            </a-input>
          </a-form-item>
          <a-form-item name="password">
            <a-input-password v-model:value="registerForm.password" size="large" placeholder="请输入密码（至少 6 位）" autocomplete="new-password">
              <template #prefix><LockOutlined class="login-form__icon" /></template>
            </a-input-password>
          </a-form-item>
          <a-form-item name="confirmPassword">
            <a-input-password v-model:value="registerForm.confirmPassword" size="large" placeholder="请再次输入密码" autocomplete="new-password">
              <template #prefix><LockOutlined class="login-form__icon" /></template>
            </a-input-password>
          </a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            block
            class="login-form__submit"
            :loading="submitting"
            :disabled="!registerCanSubmit"
          >
            注册并登录
          </a-button>
        </a-form>

        <p class="login-form-panel__footer">
          <template v-if="activeTab === 'login'">
            没有账户？
            <a-button type="link" class="login-form-panel__register" @click="switchTab('register')">点击注册</a-button>
          </template>
          <template v-else-if="activeTab === 'register'">
            已有账户？
            <a-button type="link" class="login-form-panel__register" @click="switchTab('login')">返回登录</a-button>
          </template>
          <template v-else>
            想起密码了？
            <a-button type="link" class="login-form-panel__register" @click="switchTab('login')">返回登录</a-button>
          </template>
        </p>
      </section>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background-color: #1a4fa3;
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
}

.login-card {
  display: flex;
  width: min(960px, 100%);
  min-height: 520px;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 18px 48px rgba(15, 52, 112, 0.22);
}

.login-brand {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 40px 32px;
  border-right: 1px solid #f0f0f0;
}

.login-brand__logo {
  width: 168px;
  max-width: 70%;
  height: auto;
}

.login-brand__title {
  margin: 0;
  color: #2f6fdd;
  font-size: 34px;
  font-weight: 700;
  letter-spacing: 2px;
  line-height: 1.2;
  text-align: center;
}

.login-brand__subtitle {
  margin: 0;
  color: #4f86e0;
  font-size: 15px;
  letter-spacing: 0.5px;
  text-align: center;
}

.login-brand__deco {
  width: min(260px, 85%);
  max-height: 180px;
  margin-top: 8px;
  object-fit: contain;
}

.login-form-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px 56px;
}

.login-form-panel__heading {
  margin: 0 0 28px;
  color: #2f6fdd;
  font-size: 30px;
  font-weight: 700;
  text-align: center;
}

.login-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
  margin-bottom: 28px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #d9e6ff;
}

.login-tabs__item {
  height: 44px;
  border: none;
  background: #eef4ff;
  color: #6b8fd6;
  font-size: 15px;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.login-tabs__item--active {
  background: #2f6fdd;
  color: #fff;
  font-weight: 600;
}

.login-form__icon {
  color: #8aaee8;
}

.login-form__options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.login-form__forgot {
  padding: 0;
  height: auto;
  color: #2f6fdd;
}

.login-form__submit {
  margin-top: 8px;
  height: 44px;
  border-radius: 8px;
  background: #2f6fdd;
  font-size: 16px;
  font-weight: 600;
}

.login-form__submit:hover:not(:disabled) {
  background: #2563c7;
}

.login-form-panel__footer {
  margin: 20px 0 0;
  text-align: center;
  color: rgba(0, 0, 0, 0.65);
}

.login-form-panel__register {
  padding: 0;
  height: auto;
  color: #2f6fdd;
}

@media (max-width: 768px) {
  .login-card {
    flex-direction: column;
    min-height: auto;
  }

  .login-brand {
    border-right: none;
    border-bottom: 1px solid #f0f0f0;
    padding: 24px 20px 20px;
    gap: 10px;
  }

  .login-brand__logo {
    width: 120px;
  }

  .login-brand__title {
    font-size: 26px;
  }

  .login-brand__subtitle {
    font-size: 13px;
  }

  .login-brand__deco {
    width: min(200px, 70%);
    max-height: 100px;
    margin-top: 4px;
  }

  .login-form-panel {
    padding: 32px 24px 40px;
  }

  .login-form-panel__heading {
    font-size: 26px;
  }
}

@media (max-width: 480px) {
  .login-brand__deco {
    display: none;
  }
}
</style>
