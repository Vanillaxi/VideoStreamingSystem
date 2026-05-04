<template>
  <section class="home-page">
    <div class="page-title">
      <h1>关注动态</h1>
      <el-button :icon="Refresh" @click="reload">刷新</el-button>
    </div>

    <el-alert
      v-if="hasNewDynamic"
      title="有新动态，点击刷新"
      type="success"
      show-icon
      @click="reload"
    />

    <div v-loading="loading" class="video-grid">
      <VideoCard v-for="video in videos" :key="video.id" :video="video" />
    </div>
    <el-empty v-if="!loading && videos.length === 0" description="你关注的人还没有发布新视频" />
    <div class="load-more">
      <el-button v-if="hasNext" :loading="loadingMore" @click="loadMore">加载更多</el-button>
    </div>
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getFollowingVideoFeed } from '../api/video'
import VideoCard from '../components/VideoCard.vue'

const videos = ref([])
const cursor = ref({})
const hasNext = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const hasNewDynamic = ref(false)

onMounted(() => {
  reload()
  window.addEventListener('following-video-published', handleFollowingVideoPublished)
})

onBeforeUnmount(() => {
  window.removeEventListener('following-video-published', handleFollowingVideoPublished)
})

async function reload() {
  loading.value = true
  hasNewDynamic.value = false
  cursor.value = {}
  try {
    const result = await getFollowingVideoFeed({ pageSize: 20 })
    applyResult(result.data, false)
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loadingMore.value = true
  try {
    const result = await getFollowingVideoFeed({ pageSize: 20, ...cursor.value })
    applyResult(result.data, true)
  } finally {
    loadingMore.value = false
  }
}

function applyResult(data, append) {
  const records = data?.records || []
  videos.value = append ? [...videos.value, ...records] : records
  hasNext.value = Boolean(data?.hasNext)
  cursor.value = {
    cursorCreateTime: data?.nextCreateTime,
    cursorId: data?.nextId
  }
}

function handleFollowingVideoPublished() {
  hasNewDynamic.value = true
}
</script>
