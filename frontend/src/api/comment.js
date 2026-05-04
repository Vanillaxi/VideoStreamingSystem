import request from '../utils/request'

export function listComments(params) {
  return request.get('/comment/list', { params })
}

export function listCommentsCursor(params) {
  return request.get('/comment/list/cursor', { params })
}

export function addComment({ videoId, parentId, content }) {
  return request.post('/comment/add', null, {
    params: { videoId, parentId, content }
  })
}

export function deleteComment(commentId) {
  return request.delete('/comment/delete', { params: { commentId } })
}

export function toggleCommentLike(commentId) {
  return request.post('/comment/update', null, { params: { commentId } })
}
