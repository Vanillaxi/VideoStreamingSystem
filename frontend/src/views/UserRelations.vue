<template>
  <section class="relations-page">
    <el-tabs v-model="activeTab" @tab-change="loadUsers">
      <el-tab-pane label="关注" name="following" />
      <el-tab-pane label="粉丝" name="followers" />
      <el-tab-pane label="互粉" name="mutuals" />
    </el-tabs>
    <div v-loading="loading" class="user-list">
      <article v-for="item in users" :key="item.targetUserId" class="user-list-item">
        <button class="user-list-main" @click="openUser(item.targetUserId)">
          <el-avatar :size="48" :src="item.avatarUrl">{{ initial(item) }}</el-avatar>
          <span>
            <strong>{{ item.nickname || '用户' }}</strong>
            <small>{{ item.bio || item.introduction || '暂无简介' }}</small>
          </span>
        </button>
        <el-button
          v-if="String(item.targetUserId) !== String(userStore.userId)"
          :type="item.isFollowed ? 'success' : 'primary'"
          @click="toggleFollow(item)"
        >
          {{ item.isFollowed ? '取消关注' : '关注' }}
        </el-button>
      </article>
    </div>
    <el-empty v-if="!loading && users.length === 0" description="暂无用户" />
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changeFollow, listFollowers, listFollowings, listFriends } from '../api/follow'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const activeTab = ref(route.query.tab || 'following')
const users = ref([])
const loading = ref(false)

onMounted(loadUsers)
watch(() => route.params.userId, loadUsers)

async function loadUsers() {
  loading.value = true
  try {
    const params = { userId: route.params.userId, page: 1, pageSize: 30 }
    const api = activeTab.value === 'followers' ? listFollowers : activeTab.value === 'mutuals' ? listFriends : listFollowings
    const result = await api(params)
    users.value = result.data?.records || []
  } finally {
    loading.value = false
  }
}

async function toggleFollow(item) {
  await changeFollow(item.targetUserId)
  ElMessage.success(item.isFollowed ? '已取消关注' : '关注成功')
  await loadUsers()
}

function openUser(id) {
  router.push(`/user/${id}`)
}

function initial(item) {
  return (item.nickname || 'U').slice(0, 1).toUpperCase()
}
</script>
