<template>
  <section class="home-page">
    <div class="feed-toolbar">
      <el-input v-model="keyword" class="search-input" size="large" placeholder="搜索视频标题" clearable @keyup.enter="loadBySearch">
        <template #append>
          <el-button :icon="Search" @click="loadBySearch" />
        </template>
      </el-input>
      <el-segmented v-model="sort" :options="sortOptions" @change="reloadFeed" />
    </div>

    <div class="category-row">
      <el-check-tag :checked="activeCategory === null" @change="selectCategory(null)">全部</el-check-tag>
      <el-check-tag
        v-for="category in categories"
        :key="category.id"
        :checked="activeCategory === category.id"
        @change="selectCategory(category.id)"
      >
        {{ category.name }}
      </el-check-tag>
    </div>

    <div v-loading="loading" class="video-grid">
      <VideoCard v-for="video in videos" :key="video.id" :video="video" />
    </div>
    <el-empty v-if="!loading && videos.length === 0" description="暂时没有视频" />
    <div class="load-more">
      <el-button v-if="hasNext" :loading="loadingMore" @click="loadMore">加载更多</el-button>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getVideoFeed, getVideosByCategory, listCategories, searchVideosByTitle } from '../api/video'
import VideoCard from '../components/VideoCard.vue'

const route = useRoute()
const keyword = ref('')
const sort = ref('hot')
const sortOptions = [
  { label: '热门', value: 'hot' },
  { label: '最新', value: 'time' }
]
const categories = ref([])
const activeCategory = ref(null)
const videos = ref([])
const cursor = ref({})
const hasNext = ref(false)
const loading = ref(false)
const loadingMore = ref(false)

onMounted(async () => {
  const categoryResult = await listCategories()
  categories.value = categoryResult.data || []
  keyword.value = route.query.q || ''
  if (keyword.value) {
    await loadBySearch()
  } else {
    await reloadFeed()
  }
})

watch(
  () => route.query.q,
  async (value) => {
    keyword.value = value || ''
    if (keyword.value) {
      await loadBySearch()
    } else {
      await reloadFeed()
    }
  }
)

async function reloadFeed() {
  loading.value = true
  cursor.value = {}
  try {
    const result = await getVideoFeed({ sort: sort.value, pageSize: 20 })
    applyCursorResult(result.data, false)
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loadingMore.value = true
  try {
    const result = await getVideoFeed({ sort: sort.value, pageSize: 20, ...cursor.value })
    applyCursorResult(result.data, true)
  } finally {
    loadingMore.value = false
  }
}

async function loadBySearch() {
  activeCategory.value = null
  loading.value = true
  try {
    const result = await searchVideosByTitle({ title: keyword.value, page: 1, pageSize: 30, sort: sort.value })
    videos.value = result.data?.records || []
    hasNext.value = false
  } finally {
    loading.value = false
  }
}

async function selectCategory(categoryId) {
  activeCategory.value = categoryId
  if (categoryId === null) {
    await reloadFeed()
    return
  }
  loading.value = true
  try {
    const result = await getVideosByCategory({ categoryId, page: 1, pageSize: 30, sort: sort.value })
    videos.value = result.data?.records || []
    hasNext.value = false
  } finally {
    loading.value = false
  }
}

function applyCursorResult(data, append) {
  const records = data?.records || []
  videos.value = append ? [...videos.value, ...records] : records
  hasNext.value = Boolean(data?.hasNext)
  cursor.value = {
    cursorHotScore: data?.nextHotScore,
    cursorCreateTime: data?.nextCreateTime,
    cursorId: data?.nextId
  }
}
</script>
