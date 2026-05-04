<template>
  <section v-loading="loading" class="user-page">
    <div class="user-hero">
      <el-avatar :size="92" :src="profile?.avatarUrl">{{ avatarText }}</el-avatar>
      <div>
        <h1>{{ profile?.nickname || '用户主页' }}</h1>
        <p>{{ profile?.bio || profile?.introduction || '这个用户还没有填写简介。' }}</p>
        <div class="relation-stats">
          <button @click="openRelations('following')">关注 {{ profile?.followCount || 0 }}</button>
          <button @click="openRelations('followers')">粉丝 {{ profile?.fanCount || 0 }}</button>
          <button @click="openRelations('mutuals')">互粉 {{ profile?.mutualFollowCount || 0 }}</button>
        </div>
      </div>
      <el-button v-if="!isSelf" :type="profile?.isFollowed ? 'success' : 'primary'" @click="toggleFollow">
        {{ profile?.isFollowed ? '取消关注' : '关注' }}
      </el-button>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserProfile } from '../api/user'
import { changeFollow } from '../api/follow'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const profile = ref(null)
const userId = computed(() => route.params.userId)
const isSelf = computed(() => String(userStore.userId) === String(userId.value))
const avatarText = computed(() => (profile.value?.nickname || 'U').slice(0, 1).toUpperCase())

onMounted(loadProfile)
watch(() => route.params.userId, loadProfile)

async function loadProfile() {
  loading.value = true
  try {
    const result = await getUserProfile(userId.value)
    profile.value = result.data
  } finally {
    loading.value = false
  }
}

async function toggleFollow() {
  await changeFollow(userId.value)
  ElMessage.success(profile.value?.isFollowed ? '已取消关注' : '关注成功')
  await loadProfile()
}

function openRelations(tab) {
  router.push({ name: 'UserRelations', params: { userId: userId.value }, query: { tab } })
}
</script>
