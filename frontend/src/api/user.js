import request from '../utils/request'

export function getUserList(params) {
  return request.get('/user/list', { params })
}

export function addUser(data) {
  return request.post('/user/add', data)
}

export function editUser(data) {
  return request.put('/user/edit', data)
}

export function deleteUser(id) {
  return request.delete('/user/delete', { params: { id } })
}

export function resetPassword(id, newPassword) {
  return request.put('/user/resetPassword', null, { params: { id, newPassword } })
}
