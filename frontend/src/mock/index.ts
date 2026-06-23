import type { AxiosInstance } from 'axios'
import axios from 'axios'
import {
  mockUsers, mockCourses, mockScenarios, mockTrainingScenarios,
  mockTrainingRecords, mockTrainingReport, mockCases,
  mockDashboardStats, mockRecentCourses, mockAdminStats, mockAdminDashboard, mockAIResponse,
} from './data'

// Mock 开关 - 设为 false 则走真实 API，设为 true 则强制 mock
// 设为 'auto' 则自动检测后端是否可用
export const MOCK_MODE: 'auto' | 'mock' | 'real' = 'real'

let backendAvailable: boolean | null = null

// 用于真实请求的独立 axios 实例（不带 mock adapter）
const realAxios = axios.create()

async function checkBackend(): Promise<boolean> {
  if (backendAvailable !== null) return backendAvailable
  try {
    await realAxios.get('/api/health', { timeout: 2000 })
    backendAvailable = true
  } catch {
    backendAvailable = false
  }
  return backendAvailable
}

function ok(data: any) {
  return { code: 200, message: 'success', data }
}

function match(path: string, pattern: string): Record<string, string> | null {
  const pathParts = path.split('/').filter(Boolean)
  const patternParts = pattern.split('/').filter(Boolean)
  if (pathParts.length !== patternParts.length) return null
  const params: Record<string, string> = {}
  for (let i = 0; i < pathParts.length; i++) {
    if (patternParts[i].startsWith(':')) {
      params[patternParts[i].slice(1)] = pathParts[i]
    } else if (pathParts[i] !== patternParts[i]) {
      return null
    }
  }
  return params
}

function handleMock(url: string, method: string, data?: any, params?: Record<string, unknown>): any {
  // Auth
  if (url === 'auth/login' && method === 'post') {
    return ok({ token: 'mock-jwt-token-' + Date.now() })
  } else if (url === 'auth/register' && method === 'post') {
    return ok(null)
  } else if (url === 'auth/user-info' || url === 'auth/me') {
    return ok(mockUsers[0])
  } else if (url === 'auth/refresh') {
    return ok({ token: 'mock-jwt-refreshed-' + Date.now() })
  }
  // Dashboard
  else if (url === 'dashboard/stats') {
    return ok(mockDashboardStats)
  } else if (url === 'dashboard/recent-courses') {
    return ok(mockRecentCourses)
  }
  // Courses
  else if (url === 'courses' && method === 'get') {
    const category = data?.category
    const keyword = data?.keyword
    let list = [...mockCourses]
    if (category && category !== 'all') list = list.filter(c => c.category === category)
    if (keyword) list = list.filter(c => c.title.includes(keyword) || c.description.includes(keyword))
    return ok({ items: list, total: list.length })
  } else if (match(url, 'courses/:id') && method === 'get') {
    const { id } = match(url, 'courses/:id')!
    return ok(mockCourses.find(c => c.id === id) || mockCourses[0])
  } else if (match(url, 'courses/:courseId/chapters/:chapterId')) {
    const { courseId, chapterId } = match(url, 'courses/:courseId/chapters/:chapterId')!
    const course = mockCourses.find(c => c.id === courseId) || mockCourses[0]
    const chapter = course.chapters.find((ch: any) => ch.id === chapterId) || course.chapters[0]
    const chapters = course.chapters.map((ch: any) => ({ id: ch.id, title: ch.title }))
    return ok({ chapter, chapters })
  }
  // Simulation
  else if (url === 'simulation/scenarios' || url === 'scenarios') {
    return ok(mockScenarios)
  } else if (match(url, 'scenarios/:id')) {
    const { id } = match(url, 'scenarios/:id')!
    return ok(mockScenarios.find(s => s.id === id) || mockScenarios[0])
  }
  // Training
  else if (url === 'training/scenarios') {
    return ok(mockTrainingScenarios)
  } else if (match(url, 'training/scenarios/:id')) {
    const { id } = match(url, 'training/scenarios/:id')!
    return ok(mockTrainingScenarios.find(s => s.id === id) || mockTrainingScenarios[0])
  } else if (url === 'training/records') {
    const page = Number(params?.page) || 1
    const pageSize = Number(params?.pageSize) || 10
    const start = (page - 1) * pageSize
    const items = mockTrainingRecords.slice(start, start + pageSize)
    return ok({ items, total: mockTrainingRecords.length, page, pageSize, totalPages: Math.ceil(mockTrainingRecords.length / pageSize) })
  } else if (match(url, 'training/records/:id')) {
    return ok(mockTrainingReport)
  } else if (url === 'training/start' && method === 'post') {
    return ok({ recordId: 'new-' + Date.now() })
  } else if (url === 'training/decision' && method === 'post') {
    return ok({ score: 30, consequence: '操作正确', nextDecisionPoint: null })
  }
  // Cases
  else if (url === 'cases') {
    return ok(mockCases)
  } else if (match(url, 'cases/:id')) {
    const { id } = match(url, 'cases/:id')!
    return ok(mockCases.find(c => c.id === id) || mockCases[0])
  }
  // AI
  else if (url === 'ai/ask' && method === 'post') {
    return ok({
      answer: mockAIResponse(data?.question || ''),
      sources: [
        { id: '1', title: '储能电站安全管理规定', relevance: 0.95 },
        { id: '2', title: '锂离子电池储能系统技术规范', relevance: 0.88 },
      ],
      relatedQuestions: ['热失控前兆有哪些？', '消防系统如何选择？'],
    })
  } else if (url === 'ai/history') {
    return ok({ items: [], total: 0 })
  }
  // Admin
  else if (url === 'admin/users' && method === 'get') {
    return ok({ items: mockUsers, total: mockUsers.length })
  } else if (url === 'admin/users' && method === 'post') {
    return ok({ id: 'new-' + Date.now(), ...data, createdAt: new Date().toISOString() })
  } else if (match(url, 'admin/users/:id') && method === 'put') {
    return ok({ success: true })
  } else if (match(url, 'admin/users/:id') && method === 'delete') {
    return ok({ success: true })
  } else if (url === 'admin/stats') {
    return ok(mockAdminStats)
  } else if (url === 'admin/dashboard' && method === 'get') {
    return ok(mockAdminDashboard())
  }
  // Progress
  else if (url === 'progress' && method === 'post') {
    return ok({ success: true })
  }
  // Fallback
  else {
    console.warn(`[Mock] 未匹配的请求: ${method.toUpperCase()} ${url}`)
    return ok(null)
  }
}

export function setupMock(axiosInstance: AxiosInstance) {
  if (MOCK_MODE === 'real') return

  axiosInstance.defaults.adapter = async (config) => {
    const url = (config.url || '').replace(/^\//, '')
    const method = (config.method || 'get').toLowerCase()

    // auto 模式下，先检查后端是否可用
    if (MOCK_MODE === 'auto') {
      const available = await checkBackend()
      if (available) {
        // 后端可用，走真实请求（用独立 axios 实例，避免递归）
        const resp = await realAxios.request(config)
        return {
          data: resp.data,
          status: resp.status,
          statusText: resp.statusText,
          headers: resp.headers,
          config,
          request: resp.request,
        } as any
      }
    }

    const requestData = config.data ? JSON.parse(config.data) : undefined
    const responseData = handleMock(url, method, requestData, config.params as Record<string, unknown> | undefined)

    return {
      data: responseData,
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    } as any
  }
}
