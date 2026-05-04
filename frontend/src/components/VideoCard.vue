<template>
  <article class="video-card" @click="$router.push(`/video/${video.id}`)">
    <div class="video-cover">
      <video v-if="video.videoUrl" :src="video.videoUrl" muted preload="metadata" />
      <div v-else class="cover-fallback">{{ titleInitial }}</div>
      <span class="hot-badge">热度 {{ formatNumber(video.hotScore || video.viewCount || 0) }}</span>
    </div>
    <div class="video-info">
      <h3>{{ video.title || '未命名视频' }}</h3>
      <button v-if="authorId" class="author-row" @click.stop="$router.push(`/user/${authorId}`)">
        <el-avatar :size="24" :src="video.authorAvatarUrl || video.avatarUrl">
          {{ authorInitial }}
        </el-avatar>
        <span>{{ video.authorNickname || video.nickname || '创作者' }}</span>
      </button>
      <p>{{ video.description || '这个创作者还没有填写简介。' }}</p>
      <div class="video-meta">
        <span>{{ formatNumber(video.likesCount) }} 赞</span>
        <span>{{ formatNumber(video.commentCount) }} 评论</span>
        <span>{{ formatNumber(video.viewCount) }} 播放</span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  video: {
    type: Object,
    required: true
  }
})

const titleInitial = computed(() => (props.video.title || 'V').slice(0, 1).toUpperCase())
const authorId = computed(() => props.video.authorId || props.video.userId)
const authorInitial = computed(() => (props.video.authorNickname || props.video.nickname || 'U').slice(0, 1).toUpperCase())

function formatNumber(value = 0) {
  const number = Number(value || 0)
  if (number >= 10000) return `${(number / 10000).toFixed(1)}w`
  return number.toLocaleString()
}
</script>
