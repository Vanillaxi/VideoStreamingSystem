import request from '../utils/request'

export function listCoupons(params) {
  return request.get('/coupon/list', { params })
}

export function getCouponDetail(couponId) {
  return request.get('/coupon/detail', { params: { couponId } })
}

export function seckillCoupon(couponId) {
  return request.post('/coupon/seckill/preDeduct', { couponId })
}

export function getCouponOrderStatus(couponId) {
  return request.get('/coupon/order/status', { params: { couponId } })
}

export function createCoupon(data) {
  return request.post('/admin/coupon/create', data)
}

export function listAdminCoupons(params) {
  return request.get('/admin/coupon/list', { params })
}

export function deleteCoupon(couponId) {
  return request.delete('/admin/coupon/delete', { params: { couponId } })
}
