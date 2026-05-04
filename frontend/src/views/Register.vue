<template>
  <section class="auth-page">
    <div class="auth-visual register-visual">
      <h1>创建账号</h1>
      <p>注册后即可浏览视频、参与抢券，并接收实时通知。</p>
    </div>
    <el-form ref="formRef" class="auth-form" :model="form" :rules="rules" @keyup.enter="submit">
      <h2>注册</h2>
      <el-form-item prop="username">
        <el-input v-model="form.username" size="large" placeholder="用户名" />
      </el-form-item>
      <el-form-item prop="nickname">
        <el-input v-model="form.nickname" size="large" placeholder="昵称" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="form.password" size="large" type="password" placeholder="密码" show-password />
      </el-form-item>
      <el-button type="primary" size="large" :loading="loading" @click="submit">注册</el-button>
      <p class="auth-switch">已有账号？<router-link to="/login">去登录</router-link></p>
    </el-form>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api/user'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', nickname: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少 6 位', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功，请登录')
    router.replace('/login')
  } finally {
    loading.value = false
  }
}
</script>
