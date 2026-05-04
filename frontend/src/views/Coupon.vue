<template>
  <section class="coupon-page">
    <div class="page-title">
      <h1>优惠券秒杀</h1>
      <el-button :icon="Refresh" @click="loadCoupons">刷新</el-button>
    </div>
    <el-alert
      v-if="lastMessage"
      :title="lastMessage"
      type="success"
      show-icon
      :closable="false"
    />
    <div v-loading="loading" class="coupon-grid">
      <CouponCard
        v-for="coupon in coupons"
        :key="coupon.id"
        :coupon="coupon"
        :loading="activeCouponId === coupon.id"
        @seckill="handleSeckill"
      />
    </div>
    <el-empty v-if="!loading && coupons.length === 0" description="暂无优惠券" />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getCouponOrderStatus, listCoupons, seckillCoupon } from '../api/coupon'
import CouponCard from '../components/CouponCard.vue'

const coupons = ref([])
const loading = ref(false)
const activeCouponId = ref(null)
const lastMessage = ref('')

onMounted(loadCoupons)

async function loadCoupons() {
  loading.value = true
  try {
    const result = await listCoupons({ status: 1, page: 1, pageSize: 20 })
    coupons.value = (result.data?.records || []).filter((coupon) => {
      const status = coupon.status ?? 1
      return Number(status) === 1 && coupon.isDeleted !== 1 && coupon.deleted !== true
    })
  } finally {
    loading.value = false
  }
}

async function handleSeckill(coupon) {
  activeCouponId.value = coupon.id
  try {
    const result = await seckillCoupon(coupon.id)
    lastMessage.value = result.data || result.msg || '抢券请求已提交，请留意右上角实时通知'
    ElMessage.success(lastMessage.value)
    const statusResult = await getCouponOrderStatus(coupon.id)
    if (statusResult.data?.couponCode) {
      lastMessage.value = `抢券成功：${statusResult.data.couponCode}`
    }
  } finally {
    activeCouponId.value = null
  }
}
</script>
