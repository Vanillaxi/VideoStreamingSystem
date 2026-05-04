<template>
  <section class="auth-page">
    <div class="auth-visual">
      <h1>VideoStream</h1>
      <p>进入你的视频信息流，接收关注动态和秒杀结果。</p>
    </div>
    <el-form ref="formRef" class="auth-form" :model="loginForm" :rules="rules" @submit.prevent @keyup.enter="handleLogin">
      <h2>登录</h2>
      <el-form-item prop="username">
        <el-input v-model="loginForm.username" size="large" placeholder="用户名" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="loginForm.password" size="large" type="password" placeholder="密码" show-password />
      </el-form-item>
      <el-button type="primary" size="large" :loading="userStore.loading" @click="handleLogin">登录</el-button>
      <p class="auth-switch">还没有账号？<router-link to="/register">立即注册</router-link></p>
    </el-form>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const loginForm = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  console.log('点击登录', loginForm)
  try {
    await formRef.value.validate()
    console.log('准备请求 /user/login')
    await userStore.login({
      username: loginForm.username,
      password: loginForm.password
    })
    ElMessage.success('登录成功')
    router.replace(route.query.redirect || '/')
  } catch (error) {
    if (error?.fields) return
    ElMessage.error(error?.msg || error?.message || '登录失败')
  }
}
</script>
