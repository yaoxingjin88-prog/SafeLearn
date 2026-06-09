/** 时间轴式事故推演 — 创新架构类型 */

export type TimelinePhase =
  | 'normal'
  | 'warning'
  | 'thermal_runaway'
  | 'spread'
  | 'explosion'
  | 'debrief'

export type BranchPath = 'optimal' | 'delayed' | 'escalated' | 'catastrophic'

export type OutcomeType = 'contained' | 'partial_loss' | 'major_accident' | 'pending'

export interface TelemetryPoint {
  t: number
  temp: number
  voltage: number
  soc: number
  h2: number
  smoke: number
}

export interface SceneVisualState {
  containerHeat: number[]
  smokeLevel: number
  fireLevel: number
  explosionFlash: number
  alarmActive: boolean
  ventilationOn: boolean
  isolationDone: boolean
}

export interface DecisionOption {
  id: string
  label: string
  consequence: string
  riskDelta: number
  branchHint?: BranchPath
  isOptimal: boolean
}

export interface DecisionGate {
  id: string
  nodeKey: string
  offsetMin: number
  phase: TimelinePhase
  question: string
  regulationRef?: string
  timeLimitSec: number
  options: DecisionOption[]
}

export interface TimelineNode {
  key: string
  phase: TimelinePhase
  title: string
  narrative: string
  offsetMin: number
  scene: SceneVisualState
  telemetry: TelemetryPoint
}

export interface CourseRecommendation {
  courseId?: string
  title: string
  reason: string
}

export interface TimelineScenario {
  id: string
  code: string
  title: string
  subtitle: string
  caseRef: string
  location: string
  accidentDate: string
  durationMinutes: number
  nodes: TimelineNode[]
  decisions: DecisionGate[]
  courseLinks: CourseRecommendation[]
  fishbone: Record<string, string[]>
  fiveWhy: string[]
}

export interface UserDecisionLog {
  gateId: string
  optionId: string
  isCorrect: boolean
  responseMs: number
  riskDelta: number
  consequence: string
  atOffsetMin: number
}

export interface TimelineEngineState {
  sessionId: string
  scenarioId: string
  phase: TimelinePhase
  currentNodeKey: string
  offsetMin: number
  riskIndex: number
  branch: BranchPath
  outcome: OutcomeType
  paused: boolean
  pendingDecision: DecisionGate | null
  decisions: UserDecisionLog[]
  telemetryHistory: TelemetryPoint[]
  scene: SceneVisualState
  playbackSpeed: number
  status: 'idle' | 'running' | 'decision' | 'finished'
}

export interface DimensionScore {
  key: string
  label: string
  score: number
  max: number
  comment: string
}

export interface DebriefReport {
  totalScore: number
  rating: string
  dimensions: DimensionScore[]
  instructorComment: string
  strengths: string[]
  weaknesses: string[]
  recommendations: CourseRecommendation[]
  fishbone: Record<string, string[]>
  fiveWhy: string[]
  branch: BranchPath
  outcome: OutcomeType
}
