<template>
  <section class="profile-page">
    <div class="profile-head">
      <el-avatar :size="84" :src="user?.avatarUrl">{{ avatarText }}</el-avatar>
      <div>
        <h1>{{ user?.nickname || '用户资料' }}</h1>
        <p>{{ userStore.roleLabel }} · 编辑公开展示资料</p>
      </div>
    </div>
    <el-form class="profile-form" label-position="top">
      <el-form-item label="昵称">
        <el-input v-model="form.nickname" />
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="form.bio" type="textarea" :rows="3" placeholder="TODO: 当前用户接口文档未提供简介字段" />
      </el-form-item>
      <el-form-item label="头像">
        <el-upload :auto-upload="false" :limit="1" accept="image/*" :on-change="handleAvatarChange">
          <el-button>选择头像</el-button>
        </el-upload>
      </el-form-item>
      <div class="profile-actions">
        <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
        <el-button :loading="uploading" @click="saveAvatar">上传头像</el-button>
      </div>
    </el-form>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="角色">{{ userStore.roleLabel }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ user?.createTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ user?.updateTime || '-' }}</el-descriptions-item>
    </el-descriptions>

    <el-form class="profile-form danger-panel" label-position="top">
      <h2>账号安全</h2>
      <el-form-item label="旧密码">
        <el-input v-model="passwordForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="passwordForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
      </el-form-item>
      <div class="profile-actions">
        <el-button type="warning" :loading="changingPassword" @click="submitPassword">修改密码</el-button>
        <el-button type="danger" :loading="deletingAccount" @click="confirmDeleteAccount">注销账号</el-button>
      </div>
    </el-form>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import { changePassword, deleteAccount, updateAvatar, updateUser } from '../api/user'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const { user } = storeToRefs(userStore)
const avatarText = computed(() => (user.value?.nickname || 'U').slice(0, 1).toUpperCase())
const saving = ref(false)
const uploading = ref(false)
const changingPassword = ref(false)
const deletingAccount = ref(false)
const avatarFile = ref(null)
const form = reactive({
  nickname: '',
  bio: ''
})
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

onMounted(() => {
  if (userStore.userId) {
    userStore.fetchUser()
  }
})

watch(
  user,
  (value) => {
    form.nickname = value?.nickname || ''
    form.bio = value?.bio || ''
  },
  { immediate: true }
)

function handleAvatarChange(uploadFile) {
  avatarFile.value = uploadFile.raw
}

async function saveProfile() {
  saving.value = true
  try {
    await updateUser({
      nickname: form.nickname
      // TODO: 当前 openapi.yaml 的 UserUpdateRequest 未提供 bio/简介字段。
    })
    ElMessage.success('资料已更新')
    await userStore.fetchUser()
  } finally {
    saving.value = false
  }
}

async function saveAvatar() {
  if (!avatarFile.value) {
    ElMessage.warning('请选择头像文件')
    return
  }
  uploading.value = true
  try {
    await updateAvatar(avatarFile.value)
    ElMessage.success('头像已更新')
    await userStore.fetchUser()
  } finally {
    uploading.value = false
  }
}

async function submitPassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请完整填写密码表单')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  await ElMessageBox.confirm('修改密码后需要重新登录，确认继续？', '修改密码', { type: 'warning' })
  changingPassword.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.clearSession()
    router.replace('/login')
  } finally {
    changingPassword.value = false
  }
}

async function confirmDeleteAccount() {
  await ElMessageBox.confirm('注销后账号可能无法恢复，确认注销当前账号？', '注销账号', {
    type: 'warning',
    confirmButtonText: '确认注销',
    cancelButtonText: '取消'
  })
  deletingAccount.value = true
  try {
    await deleteAccount()
    ElMessage.success('账号已注销')
    userStore.clearSession()
    router.replace('/login')
  } finally {
    deletingAccount.value = false
  }
}
</script>
