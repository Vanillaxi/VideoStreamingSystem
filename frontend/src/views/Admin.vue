<template>
  <section class="admin-page">
    <div class="page-title">
      <h1>管理面板</h1>
      <el-tag type="warning">{{ userStore.roleLabel }}</el-tag>
    </div>

    <el-alert
      title="前端只控制入口和按钮显示，管理员权限仍以后端接口校验为准。"
      type="info"
      show-icon
      :closable="false"
    />

    <el-tabs v-model="activeTab">
      <el-tab-pane label="用户管理" name="users">
        <section class="admin-section">
          <div class="admin-toolbar">
            <el-input
              v-model="userKeyword"
              placeholder="搜索昵称或 username"
              clearable
              @keyup.enter="loadUsers(1)"
            />
            <el-button type="primary" @click="loadUsers(1)">搜索</el-button>
            <el-button @click="resetUserSearch">重置</el-button>
          </div>

          <el-table v-loading="userLoading" :data="users" border>
            <el-table-column label="头像" width="82">
              <template #default="{ row }">
                <el-avatar :size="42" :src="row.avatarUrl">{{ initial(row.nickname) }}</el-avatar>
              </template>
            </el-table-column>
            <el-table-column prop="nickname" label="昵称" min-width="140" />
            <el-table-column prop="username" label="username" min-width="150" />
            <el-table-column label="角色" width="120">
              <template #default="{ row }">{{ roleText(row.role) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="createTime" label="创建时间" min-width="180" />
            <el-table-column label="操作" min-width="300" fixed="right">
              <template #default="{ row }">
                <div class="admin-actions">
                  <el-select
                    v-model="roleDrafts[row.userId]"
                    :disabled="!canManageUser(row)"
                    size="small"
                    style="width: 132px"
                  >
                    <el-option label="普通用户" :value="0" />
                    <el-option label="会员" :value="1" />
                    <el-option
                      v-if="userStore.isSuperAdmin"
                      label="管理员"
                      :value="2"
                    />
                    <el-option
                      v-if="userStore.isSuperAdmin"
                      label="香草管理员"
                      :value="3"
                    />
                  </el-select>
                  <el-button
                    size="small"
                    type="primary"
                    :disabled="!canUpdateRole(row)"
                    @click="confirmPromote(row)"
                  >
                    修改角色
                  </el-button>
                  <el-button
                    size="small"
                    type="danger"
                    :disabled="!canManageUser(row)"
                    @click="confirmRemove(row)"
                  >
                    封禁/注销
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="userPage"
            v-model:page-size="userPageSize"
            :total="userTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadUsers(1)"
            @current-change="loadUsers"
          />
        </section>
      </el-tab-pane>

      <el-tab-pane label="优惠券管理" name="coupons">
        <section class="admin-section">
          <el-form class="admin-form" :model="couponForm" label-position="top">
            <el-form-item label="标题">
              <el-input v-model="couponForm.title" placeholder="例如：满100减50" />
            </el-form-item>
            <el-form-item label="库存">
              <el-input-number v-model="couponForm.stock" :min="1" />
            </el-form-item>
            <el-form-item label="秒杀时间">
              <el-date-picker
                v-model="couponTimeRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
            <el-button type="primary" :loading="creatingCoupon" @click="createNewCoupon">创建优惠券</el-button>
          </el-form>

          <div class="admin-toolbar">
            <el-select v-model="couponStatus" placeholder="状态" clearable style="width: 160px">
              <el-option label="全部" :value="null" />
              <el-option label="可用" :value="1" />
              <el-option label="已停用" :value="0" />
            </el-select>
            <el-button @click="loadCoupons(1)">刷新</el-button>
          </div>

          <el-table v-loading="couponLoading" :data="coupons" border>
            <el-table-column prop="title" label="优惠券标题" min-width="180" />
            <el-table-column prop="stock" label="库存" width="100" />
            <el-table-column prop="startTime" label="开始时间" min-width="180" />
            <el-table-column prop="endTime" label="结束时间" min-width="180" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">
                  {{ couponStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button
                  size="small"
                  type="danger"
                  :disabled="row.status !== 1"
                  @click="confirmDeleteCoupon(row)"
                >
                  停用
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="couponPage"
            v-model:page-size="couponPageSize"
            :total="couponTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadCoupons(1)"
            @current-change="loadCoupons"
          />
        </section>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createCoupon, deleteCoupon, listAdminCoupons } from '../api/coupon'
import { listUsers, promoteUser, removeUser, searchUsers } from '../api/user'
import { useUserStore } from '../stores/user'
import { roleText } from '../utils/role'

const userStore = useUserStore()
const activeTab = ref('users')

const userKeyword = ref('')
const userLoading = ref(false)
const users = ref([])
const userTotal = ref(0)
const userPage = ref(1)
const userPageSize = ref(10)
const roleDrafts = reactive({})

const coupons = ref([])
const couponTotal = ref(0)
const couponPage = ref(1)
const couponPageSize = ref(10)
const couponStatus = ref(null)
const couponLoading = ref(false)
const creatingCoupon = ref(false)
const couponTimeRange = ref([])
const couponForm = reactive({
  title: '',
  stock: 1
})

onMounted(() => {
  loadUsers()
  loadCoupons()
})

watch(couponStatus, () => loadCoupons(1))

async function loadUsers(page = userPage.value) {
  userLoading.value = true
  userPage.value = page
  try {
    const params = { page: userPage.value, pageSize: userPageSize.value }
    const result = userKeyword.value.trim()
      ? await searchUsers({ keyword: userKeyword.value.trim(), ...params })
      : await listUsers(params)
    users.value = result.data?.records || []
    userTotal.value = result.data?.total || 0
    users.value.forEach((user) => {
      roleDrafts[user.userId] = Number(user.role ?? 0)
    })
  } finally {
    userLoading.value = false
  }
}

function resetUserSearch() {
  userKeyword.value = ''
  loadUsers(1)
}

function canManageUser(row) {
  return Number(row.role ?? 0) < userStore.role
}

function canUpdateRole(row) {
  if (!canManageUser(row)) return false
  const nextRole = Number(roleDrafts[row.userId])
  if (userStore.isSuperAdmin) return nextRole >= 0 && nextRole <= 3
  return nextRole >= 0 && nextRole < 2
}

async function confirmPromote(row) {
  const nextRole = Number(roleDrafts[row.userId])
  await ElMessageBox.confirm(
    `确认将「${row.nickname || row.username || '该用户'}」角色修改为「${roleText(nextRole)}」？`,
    '修改角色',
    { type: 'warning' }
  )
  await promoteUser(row.userId, nextRole)
  ElMessage.success('角色已更新')
  await loadUsers()
}

async function confirmRemove(row) {
  await ElMessageBox.confirm(
    `确认封禁/注销「${row.nickname || row.username || '该用户'}」？该操作不可轻易恢复。`,
    '危险操作',
    {
      type: 'warning',
      confirmButtonText: '确认处理',
      cancelButtonText: '取消'
    }
  )
  await removeUser(row.userId)
  ElMessage.success('用户已处理')
  await loadUsers()
}

async function loadCoupons(page = couponPage.value) {
  couponLoading.value = true
  couponPage.value = page
  try {
    const params = {
      page: couponPage.value,
      pageSize: couponPageSize.value
    }
    if (couponStatus.value !== null && couponStatus.value !== undefined) {
      params.status = couponStatus.value
    }
    const result = await listAdminCoupons(params)
    coupons.value = result.data?.records || []
    couponTotal.value = result.data?.total || 0
  } finally {
    couponLoading.value = false
  }
}

async function createNewCoupon() {
  if (!couponForm.title || !couponTimeRange.value?.length) {
    ElMessage.warning('请填写优惠券标题和时间')
    return
  }
  creatingCoupon.value = true
  try {
    await createCoupon({
      title: couponForm.title,
      stock: couponForm.stock,
      startTime: couponTimeRange.value[0],
      endTime: couponTimeRange.value[1]
    })
    ElMessage.success('优惠券已创建')
    couponForm.title = ''
    couponForm.stock = 1
    couponTimeRange.value = []
    await loadCoupons(1)
  } finally {
    creatingCoupon.value = false
  }
}

async function confirmDeleteCoupon(coupon) {
  await ElMessageBox.confirm(`确认停用优惠券「${coupon.title || coupon.id}」？`, '停用优惠券', {
    type: 'warning'
  })
  await deleteCoupon(coupon.id)
  ElMessage.success('优惠券已停用')
  await loadCoupons()
}

function couponStatusText(status) {
  return Number(status) === 1 ? '可用' : '已停用'
}

function initial(text) {
  return (text || 'U').slice(0, 1).toUpperCase()
}
</script>
