<template>
  <el-popover placement="bottom-end" width="340" trigger="click">
    <template #reference>
      <el-badge :value="unreadCount || undefined" :max="99" class="notification-badge">
        <el-button circle :type="connected ? 'primary' : 'default'" :icon="Bell" />
      </el-badge>
    </template>

    <div class="notification-panel">
      <div class="notification-title">
        <strong>实时通知</strong>
        <el-tag size="small" :type="connected ? 'success' : 'info'">
          {{ connected ? '已连接' : '未连接' }}
        </el-tag>
      </div>
      <el-empty v-if="messages.length === 0" description="暂无通知" :image-size="72" />
      <div v-else class="notification-list">
        <article v-for="item in messages" :key="item.id" class="notification-item">
          <div class="notification-type">{{ item.type || 'MESSAGE' }}</div>
          <p>{{ item.message || item.content || item.raw }}</p>
          <small>{{ item.time }}</small>
        </article>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { computed, onBeforeUnmount, watch } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { ElNotification } from 'element-plus'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { createNotificationSocket } from '../api/websocket'
import { useUserStore } from '../stores/user'
import { ref } from 'vue'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { userId, token } = storeToRefs(userStore)
const messages = ref([])
const connected = ref(false)
const socketRef = ref(null)
const unreadCount = computed(() => messages.value.length)

function connect() {
  if (!userId.value || !token.value || socketRef.value) return
  socketRef.value = createNotificationSocket(userId.value, {
    onOpen: () => {
      connected.value = true
    },
    onMessage: (payload) => {
      const isFollowingVideo = payload?.type === 'FOLLOWING_VIDEO_PUBLISHED'
      const item = {
        id: `${Date.now()}-${Math.random()}`,
        type: payload?.type,
        message: payload?.message,
        content: isFollowingVideo ? '你关注的人发布了新视频' : payload?.content,
        raw: typeof payload === 'string' ? payload : JSON.stringify(payload),
        time: new Date().toLocaleTimeString()
      }
      messages.value.unshift(item)
      messages.value = messages.value.slice(0, 20)
      if (isFollowingVideo) {
        window.dispatchEvent(new CustomEvent('following-video-published', { detail: payload }))
      }
      ElNotification({
        title: item.type || '新通知',
        message: item.message || item.content || item.raw,
        type: payload?.status === 'SUCCESS' || isFollowingVideo ? 'success' : 'info',
        onClick: () => {
          if (isFollowingVideo && route.name !== 'FollowingFeed') {
            router.push('/following-feed')
          }
        }
      })
    },
    onClose: () => {
      connected.value = false
      socketRef.value = null
    },
    onError: () => {
      connected.value = false
    }
  })
}

watch([userId, token], connect, { immediate: true })

onBeforeUnmount(() => {
  socketRef.value?.close()
})
</script>
