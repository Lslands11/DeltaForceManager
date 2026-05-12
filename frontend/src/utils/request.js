import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import JSONBig from 'json-bigint'

const jsonBig = JSONBig({ storeAsString: true })

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  transformResponse: [function (data) {
    try {
      return jsonBig.parse(data)
    } catch {
      return data
    }
  }]
})

// 请求拦截器 - 添加 Authorization header
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.success) {
      return res
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
      } else if (status === 403) {
        ElMessage.error('权限不足')
      } else {
        ElMessage.error(data?.message || error.message || '网络错误')
      }
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
