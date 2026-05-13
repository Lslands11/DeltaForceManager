import request from '../utils/request'

export function getAccountList(params) {
  return request.get('/account/list', { params })
}

export function getAccountById(id) {
  return request.get('/account/queryById', { params: { id } })
}

export function addAccount(data) {
  return request.post('/account/add', data)
}

export function editAccount(data) {
  return request.put('/account/edit', data)
}

export function deleteAccount(id) {
  return request.delete('/account/delete', { params: { id } })
}

export function generateToken() {
  return request.post('/account/generateToken')
}

export function getOcrGameNames() {
  return request.get('/account/ocrGameNames')
}

export function getOcrPresetList() {
  return request.get('/account/ocrPresetList')
}

export function getOcrConfig(gameName) {
  return request.get('/account/ocrConfig', { params: { gameName } })
}

export function saveOcrConfig(data) {
  return request.post('/account/saveOcrConfig', data)
}

export function deleteOcrPreset(id) {
  return request.delete('/account/deleteOcrPreset', { params: { id } })
}
