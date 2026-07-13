<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { LockOutlined, MailOutlined, SettingOutlined, UserOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import {
  changePasswordByCurrentPassword,
  changePasswordByVerificationCode,
  fetchMe,
  sendPasswordVerificationCode,
  updateMyPhone,
} from '@/api/auth'
import { getErrorMessage } from '@/api/http'
import { useAuth } from '@/composables/useAuth'
import { displayValue } from '@/utils/format'
import type { AuthUser } from '@/types/auth'

const auth = useAuth()
const router = useRouter()

const loading = ref(false)
const profile = ref<AuthUser | null>(null)
const passwordTab = ref<'current' | 'email'>('current')
const sendingCode = ref(false)
const countdown = ref(0)
const phoneModalOpen = ref(false)
const phoneSubmitting = ref(false)
const phoneForm = reactive({
  phone: '',
})
let countdownTimer: ReturnType<typeof setInterval> | null = null

const currentForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const emailForm = reactive({
  verificationCode: '',
  newPassword: '',
  confirmPassword: '',
})

const displayName = computed(() => profile.value?.displayName || profile.value?.username || '用户')
const boundEmail = computed(() => profile.value?.email || '-')
const boundPhone = computed(() => displayValue(profile.value?.phone))
const hasBoundEmail = computed(() => Boolean(profile.value?.email))

function syncSessionUser(user: AuthUser) {
  profile.value = user
  if (auth.session.value) {
    auth.session.value = {
      ...auth.session.value,
      user,
    }
  }
}

function openPhoneModal() {
  phoneForm.phone = profile.value?.phone ?? ''
  phoneModalOpen.value = true
}

function closePhoneModal() {
  phoneModalOpen.value = false
}

async function submitPhoneUpdate() {
  const trimmed = phoneForm.phone.trim()
  if (trimmed.length > 32) {
    message.warning('手机号不能超过 32 个字符')
    return Promise.reject(new Error('phone too long'))
  }
  phoneSubmitting.value = true
  try {
    const { data } = await updateMyPhone({ phone: trimmed || null })
    syncSessionUser(data)
    message.success('手机号已更新')
    closePhoneModal()
  } catch (error) {
    message.error(getErrorMessage(error, '手机号更新失败'))
    return Promise.reject(error)
  } finally {
    phoneSubmitting.value = false
  }
}

function validatePasswordPair(newPassword: string, confirmPassword: string): boolean {
  if (newPassword.length < 6 || newPassword.length > 64) {
    message.warning('密码长度为 6-64 个字符')
    return false
  }
  if (newPassword !== confirmPassword) {
    message.warning('两次输入的新密码不一致')
    return false
  }
  return true
}

async function loadProfile() {
  loading.value = true
  try {
    const { data } = await fetchMe()
    profile.value = data.user
    if (auth.session.value) {
      auth.session.value = {
        ...data,
        accessToken: auth.session.value.accessToken ?? data.accessToken ?? null,
      }
    }
  } catch (error) {
    message.error(getErrorMessage(error, '加载账号信息失败'))
  } finally {
    loading.value = false
  }
}

function startCountdown(seconds = 60) {
  countdown.value = seconds
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
  countdownTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0 && countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function handleSendVerificationCode() {
  if (!hasBoundEmail.value) {
    message.warning('当前账号未绑定邮箱，无法发送验证码')
    return
  }
  sendingCode.value = true
  try {
    await sendPasswordVerificationCode()
    message.success('验证码已发送到绑定邮箱')
    startCountdown()
  } catch (error) {
    message.error(getErrorMessage(error, '验证码发送失败'))
  } finally {
    sendingCode.value = false
  }
}

async function handleLogoutAfterPasswordChange() {
  message.success('密码已修改，请重新登录')
  await auth.logout()
  await router.replace('/login')
}

async function submitCurrentPasswordChange() {
  if (!currentForm.currentPassword.trim()) {
    message.warning('请输入原密码')
    return
  }
  if (!validatePasswordPair(currentForm.newPassword, currentForm.confirmPassword)) {
    return
  }
  try {
    await changePasswordByCurrentPassword({
      currentPassword: currentForm.currentPassword,
      newPassword: currentForm.newPassword,
      confirmPassword: currentForm.confirmPassword,
    })
    currentForm.currentPassword = ''
    currentForm.newPassword = ''
    currentForm.confirmPassword = ''
    await handleLogoutAfterPasswordChange()
  } catch (error) {
    message.error(getErrorMessage(error, '修改密码失败'))
  }
}

async function submitEmailPasswordChange() {
  if (!emailForm.verificationCode.trim()) {
    message.warning('请输入验证码')
    return
  }
  if (!validatePasswordPair(emailForm.newPassword, emailForm.confirmPassword)) {
    return
  }
  try {
    await changePasswordByVerificationCode({
      verificationCode: emailForm.verificationCode.trim(),
      newPassword: emailForm.newPassword,
      confirmPassword: emailForm.confirmPassword,
    })
    emailForm.verificationCode = ''
    emailForm.newPassword = ''
    emailForm.confirmPassword = ''
    await handleLogoutAfterPasswordChange()
  } catch (error) {
    message.error(getErrorMessage(error, '修改密码失败'))
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="personal-center">
    <a-spin :spinning="loading">
      <div class="welcome-card">
        <div class="welcome-text">
          <div class="welcome-title">欢迎回来，{{ displayName }}</div>
          <div class="welcome-subtitle">在这里查看账号信息并修改登录密码</div>
        </div>
        <div class="welcome-icon">
          <UserOutlined />
        </div>
      </div>

      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :lg="10">
          <a-card title="账号信息" bordered>
            <a-descriptions :column="1" size="small">
              <a-descriptions-item label="用户名">
                {{ profile?.username || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="显示名称">
                {{ profile?.displayName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="绑定邮箱">
                {{ boundEmail }}
              </a-descriptions-item>
              <a-descriptions-item label="手机号">
                <div class="phone-row">
                  <span>{{ boundPhone }}</span>
                  <a-tooltip title="编辑手机号">
                    <a-button type="text" size="small" class="phone-row__edit" @click="openPhoneModal">
                      <SettingOutlined />
                    </a-button>
                  </a-tooltip>
                </div>
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <a-col :xs="24" :lg="14">
          <a-card title="修改密码" bordered>
            <a-tabs v-model:active-key="passwordTab">
              <a-tab-pane key="current" tab="原密码修改">
                <a-form layout="vertical">
                  <a-form-item label="原密码" required>
                    <a-input-password
                      v-model:value="currentForm.currentPassword"
                      placeholder="请输入当前登录密码"
                      autocomplete="current-password"
                    >
                      <template #prefix><LockOutlined /></template>
                    </a-input-password>
                  </a-form-item>
                  <a-form-item label="新密码" required>
                    <a-input-password
                      v-model:value="currentForm.newPassword"
                      placeholder="6-64 位新密码"
                      autocomplete="new-password"
                    >
                      <template #prefix><LockOutlined /></template>
                    </a-input-password>
                  </a-form-item>
                  <a-form-item label="确认新密码" required>
                    <a-input-password
                      v-model:value="currentForm.confirmPassword"
                      placeholder="请再次输入新密码"
                      autocomplete="new-password"
                    >
                      <template #prefix><LockOutlined /></template>
                    </a-input-password>
                  </a-form-item>
                  <a-button type="primary" @click="submitCurrentPasswordChange">确认修改</a-button>
                </a-form>
              </a-tab-pane>

              <a-tab-pane key="email" tab="邮箱验证码修改">
                <a-alert
                  v-if="!hasBoundEmail"
                  type="warning"
                  show-icon
                  message="当前账号未绑定邮箱，无法使用验证码修改密码"
                  style="margin-bottom: 16px"
                />
                <a-form layout="vertical">
                  <a-form-item label="绑定邮箱">
                    <a-input :value="boundEmail" disabled>
                      <template #prefix><MailOutlined /></template>
                    </a-input>
                  </a-form-item>
                  <a-form-item label="验证码" required>
                    <a-space>
                      <a-input
                        v-model:value="emailForm.verificationCode"
                        placeholder="6 位验证码"
                        maxlength="6"
                        style="width: 180px"
                      />
                      <a-button
                        :disabled="!hasBoundEmail || sendingCode || countdown > 0"
                        :loading="sendingCode"
                        @click="handleSendVerificationCode"
                      >
                        {{ countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}
                      </a-button>
                    </a-space>
                  </a-form-item>
                  <a-form-item label="新密码" required>
                    <a-input-password
                      v-model:value="emailForm.newPassword"
                      placeholder="6-64 位新密码"
                      autocomplete="new-password"
                    >
                      <template #prefix><LockOutlined /></template>
                    </a-input-password>
                  </a-form-item>
                  <a-form-item label="确认新密码" required>
                    <a-input-password
                      v-model:value="emailForm.confirmPassword"
                      placeholder="请再次输入新密码"
                      autocomplete="new-password"
                    >
                      <template #prefix><LockOutlined /></template>
                    </a-input-password>
                  </a-form-item>
                  <a-button
                    type="primary"
                    :disabled="!hasBoundEmail"
                    @click="submitEmailPasswordChange"
                  >
                    确认修改
                  </a-button>
                </a-form>
              </a-tab-pane>
            </a-tabs>
          </a-card>
        </a-col>
      </a-row>

      <a-modal
        v-model:open="phoneModalOpen"
        title="编辑手机号"
        ok-text="提交"
        cancel-text="关闭"
        :confirm-loading="phoneSubmitting"
        destroy-on-close
        @ok="submitPhoneUpdate"
        @cancel="closePhoneModal"
      >
        <a-form layout="vertical">
          <a-form-item label="手机号">
            <a-input
              v-model:value="phoneForm.phone"
              allow-clear
              maxlength="32"
              placeholder="请输入手机号，留空可清除"
            />
          </a-form-item>
          <div class="phone-modal__hint">无需短信验证，留空可清除手机号</div>
        </a-form>
      </a-modal>
    </a-spin>
  </div>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.personal-center {
  padding: @spacing-lg;
  min-height: 100%;
}

.welcome-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: @spacing-lg;
  padding: 24px 28px;
  border-radius: 12px;
  background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
  color: #fff;
}

.welcome-title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 8px;
}

.welcome-subtitle {
  opacity: 0.9;
}

.welcome-icon {
  font-size: 48px;
  opacity: 0.85;
}

.phone-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.phone-row__edit {
  color: @color-text-tertiary;
}

.phone-modal__hint {
  color: @color-text-tertiary;
  font-size: 12px;
}
</style>
