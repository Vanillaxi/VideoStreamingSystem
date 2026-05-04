<template>
  <article class="coupon-card">
    <div>
      <el-tag :type="coupon.status === 1 ? 'success' : 'info'" effect="dark">
        {{ coupon.status === 1 ? '进行中' : '未启用' }}
      </el-tag>
      <h3>{{ coupon.title || '优惠券' }}</h3>
      <p>库存 {{ coupon.stock ?? '-' }}</p>
    </div>
    <div class="coupon-time">
      <span>{{ coupon.startTime || '开始时间待定' }}</span>
      <span>{{ coupon.endTime || '结束时间待定' }}</span>
    </div>
    <el-button type="danger" :loading="loading" @click="$emit('seckill', coupon)">
      立即抢券
    </el-button>
  </article>
</template>

<script setup>
defineProps({
  coupon: {
    type: Object,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  }
})

defineEmits(['seckill'])
</script>
