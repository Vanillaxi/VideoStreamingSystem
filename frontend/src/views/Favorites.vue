<template>
  <section class="home-page">
    <div class="page-title">
      <h1>我的收藏</h1>
      <el-button @click="loadFavorites">刷新</el-button>
    </div>
    <div v-loading="loading" class="video-grid">
      <VideoCard v-for="video in videos" :key="video.id" :video="video" />
    </div>
    <el-empty v-if="!loading && videos.length === 0" description="还没有收藏视频" />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getFavoriteVideos } from '../api/video'
import VideoCard from '../components/VideoCard.vue'

const videos = ref([])
const loading = ref(false)

onMounted(loadFavorites)

async function loadFavorites() {
  loading.value = true
  try {
    const result = await getFavoriteVideos({ page: 1, pageSize: 30 })
    videos.value = result.data?.records || []
  } finally {
    loading.value = false
  }
}
</script>
