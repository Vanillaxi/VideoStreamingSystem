import request from '../utils/request'

export function getVideoById(id) {
  return request.get('/video/get/id', { params: { id } })
}

export const getVideoDetail = getVideoById

export function searchVideosByTitle(params) {
  return request.get('/video/get/title', { params })
}

export function getVideosByCategory(params) {
  return request.get('/video/get/category', { params })
}

export function getHotVideos() {
  return request.get('/video/list/hot')
}

export function getHotCursorVideos(params) {
  return request.get('/video/list/hot/cursor', { params })
}

export function getVideoFeed(params) {
  return request.get('/video/feed/cursor', { params })
}

export function getFollowingVideoFeed(params) {
  return request.get('/video/feed/following', { params })
}

export function getNewestVideos(params) {
  return request.get('/video/list/new', { params })
}

export function listVideos(params = {}) {
  if (params.categoryId) {
    return getVideosByCategory(params)
  }
  if (params.title) {
    return searchVideosByTitle(params)
  }
  return getVideoFeed(params)
}

export function publishVideo({ title, description, categoryId, file }) {
  const formData = new FormData()
  formData.append('title', title)
  if (description) formData.append('description', description)
  if (categoryId) formData.append('categoryId', categoryId)
  formData.append('file', file)
  return request.post('/video/post', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// TODO: 当前 openapi.yaml 未提供单独的视频封面上传接口。
export function uploadVideoCover() {
  return Promise.reject(new Error('接口文档未提供视频封面上传接口'))
}

export function toggleVideoLike(videoId) {
  return request.post('/video/changeLikes', null, { params: { videoId } })
}

export const likeVideo = toggleVideoLike
// TODO: 后端当前只有 /video/changeLikes 切换点赞状态，没有单独取消点赞接口。
export const unlikeVideo = toggleVideoLike

export function addFavorite(videoId) {
  return request.post('/favorite/add', null, { params: { videoId } })
}

export function cancelFavorite(videoId) {
  return request.delete('/favorite/cancel', { params: { videoId } })
}

export function checkFavorite(videoId) {
  return request.get('/favorite/check', { params: { videoId } })
}

export function listFavoriteVideos(params) {
  return request.get('/favorite/list', { params })
}

export const getFavoriteVideos = listFavoriteVideos

// TODO: 当前 openapi.yaml 只提供管理员播放量刷库接口，未提供用户侧播放量上报接口。
export function reportVideoView() {
  return Promise.resolve({ code: 1, data: null, msg: '接口文档未提供播放量上报接口' })
}

export function listCategories() {
  return request.get('/category/list')
}
