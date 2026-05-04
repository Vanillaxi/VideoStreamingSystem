import request from '../utils/request'

export function register(data) {
  return request.post('/user/register', data)
}

export function login(data) {
  return request.post('/user/login', data)
}

export function logout() {
  return request.post('/user/logout')
}

export function getUserInfo(id) {
  return request.get('/user/info', { params: { id } })
}

export const getUserProfile = getUserInfo

export function updateUser(data) {
  return request.post('/user/update', data)
}

export function updateAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteAccount() {
  return request.delete('/user/delete')
}

export function promoteUser(userId, role) {
  return request.post('/user/promote', null, { params: { userId, role } })
}

export function removeUser(id) {
  return request.delete('/user/remove', { params: { id } })
}

export function changePassword(data) {
  return request.put('/user/password', data)
}

export function listUsers(params) {
  return request.get('/admin/users', { params })
}

export function searchUsers({ keyword, page, pageSize }) {
  return request.get('/admin/users/search', {
    params: {
      nickname: keyword || undefined,
      username: keyword || undefined,
      page,
      pageSize
    }
  })
}
