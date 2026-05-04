<template>
  <section class="publish-page">
    <div class="page-title">
      <h1>发布视频</h1>
    </div>
    <el-form ref="formRef" class="publish-form" :model="form" :rules="rules" label-position="top">
      <el-form-item label="视频标题" prop="title">
        <el-input v-model="form.title" maxlength="80" show-word-limit placeholder="填写视频标题" />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="选择分类" clearable>
          <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="form.description" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
      <el-form-item label="视频文件" prop="file">
        <el-upload
          drag
          :auto-upload="false"
          :limit="1"
          accept="video/*"
          :on-change="handleVideoChange"
          :on-remove="handleVideoRemove"
        >
          <div class="upload-text">选择或拖拽视频文件</div>
        </el-upload>
      </el-form-item>
      <el-form-item label="封面">
        <el-upload drag :auto-upload="false" :limit="1" accept="image/*" :on-change="handleCoverChange">
          <div class="upload-text">选择封面图片</div>
        </el-upload>
        <p class="form-tip">TODO: 当前接口文档未提供封面上传字段或接口，暂不随发布请求提交。</p>
      </el-form-item>
      <el-button type="primary" size="large" :loading="submitting" @click="submit">发布</el-button>
    </el-form>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listCategories, publishVideo } from '../api/video'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const categories = ref([])
const form = reactive({
  title: '',
  categoryId: null,
  description: '',
  file: null,
  cover: null
})
const rules = {
  title: [{ required: true, message: '请输入视频标题', trigger: 'blur' }],
  file: [{ required: true, message: '请选择视频文件', trigger: 'change' }]
}

onMounted(async () => {
  const result = await listCategories()
  categories.value = result.data || []
})

function handleVideoChange(uploadFile) {
  form.file = uploadFile.raw
}

function handleVideoRemove() {
  form.file = null
}

function handleCoverChange(uploadFile) {
  form.cover = uploadFile.raw
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const result = await publishVideo(form)
    ElMessage.success('发布成功')
    router.push(`/video/${result.data?.id}`)
  } finally {
    submitting.value = false
  }
}
</script>
