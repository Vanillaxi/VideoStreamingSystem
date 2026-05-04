import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStoredUser, getToken, getTokenRole } from '../utils/auth'
import { isAdminRole } from '../utils/role'

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/Home.vue'), meta: { requiresAuth: true } },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  { path: '/video/:id', name: 'VideoDetail', component: () => import('../views/VideoDetail.vue'), meta: { requiresAuth: true } },
  { path: '/publish', name: 'PublishVideo', component: () => import('../views/PublishVideo.vue'), meta: { requiresAuth: true } },
  { path: '/following-feed', name: 'FollowingFeed', component: () => import('../views/FollowingFeed.vue'), meta: { requiresAuth: true } },
  { path: '/coupon', name: 'Coupon', component: () => import('../views/Coupon.vue'), meta: { requiresAuth: true } },
  { path: '/favorites', name: 'Favorites', component: () => import('../views/Favorites.vue'), meta: { requiresAuth: true } },
  { path: '/admin', name: 'Admin', component: () => import('../views/Admin.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/profile', name: 'Profile', component: () => import('../views/Profile.vue'), meta: { requiresAuth: true } },
  { path: '/user/:userId', name: 'UserProfile', component: () => import('../views/UserProfile.vue'), meta: { requiresAuth: true } },
  { path: '/relations/:userId', name: 'UserRelations', component: () => import('../views/UserRelations.vue'), meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !getToken()) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !isAdminRole(getTokenRole())) {
    const storedUser = getStoredUser()
    if (isAdminRole(storedUser?.role)) {
      return true
    }
    ElMessage.warning('无权限访问管理员页面')
    return { name: 'Home' }
  }
  if ((to.name === 'Login' || to.name === 'Register') && getToken()) {
    return { name: 'Home' }
  }
  return true
})

export default router
