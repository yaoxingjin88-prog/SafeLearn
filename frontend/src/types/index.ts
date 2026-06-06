// 用户相关
export interface UserInfo {
  id: string
  username: string
  email: string
  role: 'admin' | 'trainee'
  company?: string
  department?: string
  avatar?: string
}

export interface LoginForm {
  username: string
  password: string
}

// 难度等级
export const DifficultyLevel = {
  BASIC: 'BASIC',
  INTERMEDIATE: 'INTERMEDIATE',
  ADVANCED: 'ADVANCED',
} as const

export type DifficultyLevel = typeof DifficultyLevel[keyof typeof DifficultyLevel]

// 技能树相关
export interface SkillTreeNode {
  id: string
  courseId: string
  courseTitle: string
  title: string
  difficultyLevel: DifficultyLevel
  prerequisiteIds: string[]
  unlocked: boolean
  completed: boolean
  progress: number
}

export interface SkillTreeConnection {
  from: string
  to: string
}

export interface SkillTreeData {
  levels: {
    BASIC: SkillTreeNode[]
    INTERMEDIATE: SkillTreeNode[]
    ADVANCED: SkillTreeNode[]
  }
  connections: SkillTreeConnection[]
}

// 课程相关
export interface Course {
  id: string
  title: string
  description: string
  coverImage: string
  category: 'basic' | 'battery' | 'thermal' | 'fire' | 'bms' | 'case'
  chapters: Chapter[]
  totalDuration: number
  progress?: number
  status?: string
  chapterCount?: number
  createdAt: string
  updatedAt: string
}

export interface Chapter {
  id: string
  courseId: string
  title: string
  content: string
  videoUrl?: string
  duration: number
  order: number
  difficultyLevel?: number
  difficultyLabel?: string
  prerequisiteIds?: string[]
  scenarioId?: string
  unlocked?: boolean
}

export interface UserProgress {
  userId: string
  courseId: string
  chapterId: string
  progress: number
  completed?: boolean
  masteryLevel?: number
  completedAt?: string
  lastAccessAt: string
}

// 推演相关
export interface AccidentScenario {
  id: string
  name: string
  description: string
  difficulty: 'easy' | 'medium' | 'hard'
  initialConditions: {
    batteryCount: number
    initialTemperature: number
    batteryType: string
    capacity: number
  }
  events: ScenarioEvent[]
  duration: number
}

export interface ScenarioEvent {
  id: string
  triggerTime: number
  type: 'temperature_rise' | 'smoke' | 'fire' | 'explosion' | 'gas_leak'
  location: { x: number; y: number; z: number }
  parameters: {
    temperature?: number
    spreadRate?: number
    gasConcentration?: number
  }
  description: string
}

export interface SimulationState {
  timestamp: number
  batteryCells: BatteryCellState[]
  environment: {
    temperature: number
    humidity: number
    gasLevel: number
  }
  alerts: Alert[]
}

export interface BatteryCellState {
  id: string
  position: { x: number; y: number; z: number }
  temperature: number
  status: 'normal' | 'warning' | 'danger' | 'failed'
  voltage: number
  current: number
}

export interface Alert {
  id: string
  level: 'info' | 'warning' | 'danger'
  message: string
  timestamp: number
}

// 训练相关
export interface TrainingScenario {
  id: string
  name: string
  description: string
  difficulty: number
  difficultyLabel?: string
  timeLimit: number
  decisionPoints: DecisionPoint[]
  prerequisiteIds?: string[]
  unlocked?: boolean
}

export interface DecisionPoint {
  id: string
  scenarioId: string
  triggerCondition: string
  question: string
  options: DecisionOption[]
  timePressure: number
}

export interface DecisionOption {
  id: string
  text: string
  score: number
  consequence: string
  nextDecisionPointId?: string
  isCorrect: boolean
}

export interface TrainingRecord {
  id: string
  userId: string
  scenarioId: string
  startTime: string
  endTime: string
  decisions: {
    decisionPointId: string
    selectedOptionId: string
    responseTime: number
    score: number
  }[]
  totalScore: number
  rating: 'excellent' | 'good' | 'average' | 'poor'
  feedback: string
}

// 案例相关
export interface AccidentCase {
  id: string
  title: string
  location: string
  date: string
  type: 'fire' | 'explosion' | 'gas_leak' | 'thermal_runaway'
  severity: 'minor' | 'moderate' | 'major' | 'critical'
  description: string
  timeline: TimelineEvent[]
  causeAnalysis: string
  lossEstimate: string
  lessonsLearned: string
  references: string[]
  images: string[]
}

export interface TimelineEvent {
  id: string
  time: string
  title: string
  description: string
  type: 'detection' | 'alarm' | 'response' | 'escalation' | 'resolution'
  importance: 'low' | 'medium' | 'high'
}

// AI问答相关
export interface QARecord {
  id: string
  userId: string
  question: string
  answer: string
  sources: { id: string; title: string; relevance: number }[]
  rating?: number
  createdAt: string
}

// 通用类型
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

export interface PaginatedData<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}
