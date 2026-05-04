import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { clearAuth, getToken } from './auth'

const PUBLIC_URLS = ['/user/login', '/user/register']

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const isPublicUrl = PUBLIC_URLS.includes(config.url)
  const token = getToken()
  if (token && !isPublicUrl) {
    config.headers.Authorization = `Bearer ${token}`
  }
  const fullURL = `${config.baseURL || ''}${config.url || ''}`
  console.log('[API Request]', {
    method: (config.method || 'get').toUpperCase(),
    url: fullURL,
    params: config.params,
    data: config.data
  })
  return config
})

request.interceptors.response.use(
  (response) => {
    const contentType = response.headers?.['content-type'] || ''
    if (!contentType.includes('application/json')) {
      return response.data
    }

    const result = response.data
    if (result?.code === 0) {
      ElMessage.error(result.msg || '请求失败')
      return Promise.reject(result)
    }
    console.log('[API Response]', {
      url: `${response.config.baseURL || ''}${response.config.url || ''}`,
      data: result
    })
    return result
  },
  (error) => {
    console.log('[API Error]', {
      url: `${error.config?.baseURL || ''}${error.config?.url || ''}`,
      params: error.config?.params,
      data: error.config?.data,
      response: error.response?.data
    })
    if (error.response?.status === 401) {
      clearAuth()
      ElMessage.warning('登录已过期，请重新登录')
      router.replace({ name: 'Login' })
    } else {
      ElMessage.error(error.response?.data?.msg || error.msg || error.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

export default request
