import request from '../utils/request'

export function getBalanceList(params) {
  return request.get('/balance/list', { params })
}

export function manualInput(data) {
  return request.post('/balance/manualInput', data)
}

export function deleteBalance(id) {
  return request.delete('/balance/delete', { params: { id } })
}
