<template>
  <section v-loading="loading" class="detail-page">
    <video v-if="video?.videoUrl" class="detail-player" :src="video.videoUrl" controls autoplay />
    <div v-else class="detail-player empty-player">暂无视频地址</div>
    <div class="detail-content">
      <button v-if="authorId" class="detail-author" @click="openAuthor">
        <el-avatar :size="42" :src="video?.authorAvatarUrl">{{ authorInitial }}</el-avatar>
        <span>{{ video?.authorNickname || '创作者' }}</span>
      </button>
      <h1>{{ video?.title || '视频详情' }}</h1>
      <p>{{ video?.description || '暂无简介' }}</p>
      <div class="detail-actions">
        <el-button type="primary" :icon="Pointer" @click="like">点赞 {{ video?.likesCount || 0 }}</el-button>
        <el-button :type="favorite ? 'success' : 'default'" @click="toggleFavorite">
          {{ favorite ? '取消收藏' : '收藏' }} {{ video?.favoriteCount || 0 }}
        </el-button>
        <el-tag>播放 {{ video?.viewCount || 0 }}</el-tag>
        <el-tag type="warning">评论 {{ video?.commentCount || 0 }}</el-tag>
      </div>
    </div>
    <div class="detail-content comment-section">
      <div class="section-title">
        <h2>评论</h2>
        <el-segmented v-model="commentSort" :options="commentSortOptions" @change="loadComments" />
      </div>
      <el-input
        v-model="commentContent"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-word-limit
        placeholder="写下你的评论"
      />
      <div class="comment-submit">
        <el-button type="primary" :loading="commentSubmitting" @click="submitComment">发布评论</el-button>
      </div>
      <el-empty v-if="comments.length === 0" description="暂无评论" :image-size="80" />
      <article v-for="comment in comments" :key="comment.id" class="comment-item">
        <div class="comment-head">
          <button class="comment-user" @click="openUser(comment.userId)">
            <el-avatar :size="32" :src="comment.avatarUrl">{{ initial(comment) }}</el-avatar>
            <strong>{{ comment.nickname || `用户 ${comment.userId}` }}</strong>
          </button>
          <small>{{ comment.createTime }}</small>
        </div>
        <p>{{ comment.content }}</p>
        <div class="comment-actions">
          <el-button text size="small" @click="likeComment(comment)">点赞 {{ comment.likesCount || 0 }}</el-button>
          <el-button text size="small" @click="startReply(comment)">回复</el-button>
          <el-button text size="small" @click="removeComment(comment)">删除</el-button>
        </div>
        <div v-if="comment.replies?.length" class="reply-list">
          <article v-for="reply in comment.replies" :key="reply.id" class="reply-item">
            <button class="comment-user" @click="openUser(reply.userId)">
              <el-avatar :size="26" :src="reply.avatarUrl">{{ initial(reply) }}</el-avatar>
              <strong>{{ reply.nickname || `用户 ${reply.userId}` }}</strong>
            </button>
            <p>
              <span v-if="reply.replyToNickname">回复 @{{ reply.replyToNickname }}：</span>{{ reply.content }}
            </p>
            <div class="comment-actions">
              <small>{{ reply.createTime }}</small>
              <el-button text size="small" @click="likeComment(reply)">点赞 {{ reply.likesCount || 0 }}</el-button>
              <el-button text size="small" @click="startReply(reply)">回复</el-button>
              <el-button text size="small" @click="removeComment(reply)">删除</el-button>
            </div>
          </article>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Pointer } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addFavorite, cancelFavorite, checkFavorite, getVideoById, toggleVideoLike } from '../api/video'
import { addComment, deleteComment, listComments, toggleCommentLike } from '../api/comment'

const route = useRoute()
const router = useRouter()
const video = ref(null)
const loading = ref(false)
const favorite = ref(false)
const comments = ref([])
const commentContent = ref('')
const commentSubmitting = ref(false)
const commentSort = ref('time')
const commentSortOptions = [
  { label: '最新', value: 'time' },
  { label: '热门', value: 'hot' }
]
const replyTarget = ref(null)
const authorId = computed(() => video.value?.authorId || video.value?.userId)
const authorInitial = computed(() => (video.value?.authorNickname || 'U').slice(0, 1).toUpperCase())

onMounted(async () => {
  await loadVideo()
  await loadFavorite()
  await loadComments()
})

async function loadVideo() {
  loading.value = true
  try {
    const result = await getVideoById(route.params.id)
    video.value = result.data
  } finally {
    loading.value = false
  }
}

async function like() {
  await toggleVideoLike(video.value.id)
  ElMessage.success('操作成功')
  await loadVideo()
}

async function loadFavorite() {
  const result = await checkFavorite(route.params.id)
  favorite.value = Boolean(result.data)
}

async function toggleFavorite() {
  if (favorite.value) {
    await cancelFavorite(video.value.id)
    ElMessage.success('已取消收藏')
  } else {
    await addFavorite(video.value.id)
    ElMessage.success('收藏成功')
  }
  await loadFavorite()
  await loadVideo()
}

async function loadComments() {
  const result = await listComments({
    videoId: route.params.id,
    page: 1,
    pageSize: 30,
    sort: commentSort.value
  })
  comments.value = result.data?.records || []
}

async function submitComment() {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  commentSubmitting.value = true
  try {
    await addComment({
      videoId: route.params.id,
      parentId: replyTarget.value?.id,
      content: commentContent.value.trim()
    })
    commentContent.value = ''
    replyTarget.value = null
    ElMessage.success('评论已发布')
    await loadComments()
    await loadVideo()
  } finally {
    commentSubmitting.value = false
  }
}

async function likeComment(comment) {
  await toggleCommentLike(comment.id)
  await loadComments()
}

async function removeComment(comment) {
  await ElMessageBox.confirm('确认删除这条评论？', '删除评论', { type: 'warning' })
  await deleteComment(comment.id)
  ElMessage.success('评论已删除')
  await loadComments()
  await loadVideo()
}

function startReply(comment) {
  replyTarget.value = comment
  commentContent.value = `@${comment.nickname || comment.username || ''} `
}

function openAuthor() {
  router.push(`/user/${authorId.value}`)
}

function openUser(userId) {
  if (userId) router.push(`/user/${userId}`)
}

function initial(comment) {
  return (comment.nickname || 'U').slice(0, 1).toUpperCase()
}
</script>
