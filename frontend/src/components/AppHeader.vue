<template>
  <header class="app-header">
    <router-link class="brand" to="/">
      <span class="brand-mark">VS</span>
      <span>VideoStream</span>
    </router-link>

    <nav class="nav-links">
      <router-link to="/">发现</router-link>
      <router-link to="/following-feed">动态</router-link>
      <router-link to="/publish">发布</router-link>
      <router-link to="/coupon">抢券</router-link>
      <router-link to="/favorites">收藏</router-link>
      <router-link v-if="userStore.isAdmin" to="/admin">管理</router-link>
      <router-link to="/profile">我的</router-link>
    </nav>

    <div class="header-actions">
      <el-input
        v-model="keyword"
        class="header-search"
        placeholder="搜索视频"
        clearable
        @keyup.enter="search"
      />
      <NotificationBell />
      <el-dropdown trigger="click" @command="handleCommand">
        <button class="user-chip">
          <el-avatar :size="32" :src="userStore.user?.avatarUrl">
            {{ avatarText }}
          </el-avatar>
          <span>{{ userStore.user?.nickname || userStore.user?.username || '用户' }}</span>
          <small class="role-chip">{{ userStore.roleLabel }}</small>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人资料</el-dropdown-item>
            <el-dropdown-item v-if="userStore.isAdmin" command="admin">管理面板</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import NotificationBell from './NotificationBell.vue'

const router = useRouter()
const userStore = useUserStore()
const keyword = ref('')

const avatarText = computed(() => (userStore.user?.nickname || userStore.user?.username || 'U').slice(0, 1).toUpperCase())

onMounted(() => {
  if (userStore.token && !userStore.user && userStore.userId) {
    userStore.fetchUser()
  }
})

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'admin') {
    router.push('/admin')
    return
  }
  await userStore.logout()
  router.push('/login')
}

function search() {
  router.push({ name: 'Home', query: { q: keyword.value.trim() } })
}
</script>
