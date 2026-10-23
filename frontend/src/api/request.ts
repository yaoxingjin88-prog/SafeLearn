import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

function loginPath(): string {
  const path = window.location.pathname
  return path.startsWith('/user') ? '/user/login' : '/login'
}

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 仅开发环境且显式开启 VITE_ENABLE_MOCK=true 时启用 mock
if (import.meta.env.DEV && import.meta.env.VITE_ENABLE_MOCK === 'true') {
  import('@/mock').then(({ setupMock }) => setupMock(service))
}

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
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
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        window.location.href = loginPath()
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = loginPath()
      return Promise.reject(error)
    }
    if (error.response?.status === 403) {
      ElMessage.error(error.response?.data?.message || '权限不足')
      return Promise.reject(error)
    }
    console.error('API error:', error)
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service
