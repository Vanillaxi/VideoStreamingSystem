import request from '../utils/request'

export function listFollowings(params) {
  return request.get('/follow/followings', { params })
}

export function listFollowers(params) {
  return request.get('/follow/followers', { params })
}

export function listFriends(params) {
  return request.get('/follow/friends', { params })
}

export function isFollow(followingId) {
  return request.get('/follow/isFollow', { params: { followingId } })
}

export function changeFollow(followingId) {
  return request.post('/follow/changeFollow', null, { params: { followingId } })
}

// TODO: 后端当前只有 /follow/changeFollow 切换关注状态，没有单独关注/取消关注接口。
export const followUser = changeFollow
export const unfollowUser = changeFollow
