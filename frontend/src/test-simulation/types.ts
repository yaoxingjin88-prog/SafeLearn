/** 储能测试作业事故推演 — 类型定义 */

export type SimPhase =
  | 'background'
  | 'testing'
  | 'anomaly'
  | 'thermal_runaway'
  | 'flashover'
  | 'emergency'
  | 'investigation'
  | 'debrief'

export type SimOutcome = 'controlled' | 'partial' | 'catastrophic' | 'pending'

export type RiskLevel = 'low' | 'medium' | 'high' | 'critical'

export interface TelemetrySnapshot {
  clock: string
  envTemp: number
  packTemp: number
  voltage: number
  current: number
  soc: number
  soh: number
  insulation: string
  gasPpm: number
  vocPpm: number
  pressure: number
  alarmCount: number
}

export interface SceneState {
  packHeat: number
  packColor: 'normal' | 'warning' | 'danger' | 'critical'
  smoke: number
  flash: number
  ventOn: boolean
  personnelEvacuated: boolean
}

export interface TimelineNode {
  key: string
  phase: SimPhase
  clock: string
  /** 相对 14:00 的模拟分钟偏移 */
  offsetMin: number
  title: string
  narrative: string
  telemetry: TelemetrySnapshot
  scene: SceneState
}

export interface DecisionOption {
  id: string
  label: string
  riskDelta: number
  isOptimal: boolean
  consequence: string
  branch?: 'continue' | 'early_success' | 'escalate' | 'catastrophe'
}

export interface DecisionGate {
  id: string
  phase: SimPhase
  nodeKey: string
  title: string
  question: string
  regulationRef: string
  timeLimitSec: number
  options: DecisionOption[]
}

export interface EvidenceItem {
  id: string
  phase: SimPhase
  title: string
  type: 'video' | 'bms' | 'thermal' | 'gas' | 'ops'
  summary: string
  unlocked: boolean
}

export interface CourseLink {
  title: string
  reason: string
}

export interface TestSimScenario {
  id: string
  code: string
  caseId: string
  title: string
  subtitle: string
  location: string
  accidentDate: string
  projectName: string
  testPurpose: string
  testObject: string
  testEnv: string
  personnel: string
  durationMinutes: number
  nodes: TimelineNode[]
  decisions: DecisionGate[]
  evidence: EvidenceItem[]
  fishbone: Record<string, string[]>
  fiveWhy: string[]
  fta: { label: string; children?: string[] }[]
  courseLinks: CourseLink[]
  mentorFaqs: { q: string; a: string; ref?: string }[]
}

export interface DecisionLog {
  gateId: string
  optionId: string
  isCorrect: boolean
  responseMs: number
  consequence: string
}

export interface SimEngineState {
  sessionId: string
  phase: SimPhase
  currentNodeKey: string
  clock: string
  offsetMin: number
  riskIndex: number
  riskLevel: RiskLevel
  outcome: SimOutcome
  status: 'idle' | 'running' | 'decision' | 'finished'
  paused: boolean
  pendingDecision: DecisionGate | null
  decisions: DecisionLog[]
  telemetryHistory: TelemetrySnapshot[]
  scene: SceneState
  evidenceUnlocked: string[]
  playbackSpeed: number
}

export interface SimScoreReport {
  totalScore: number
  rating: string
  dimensions: { key: string; label: string; score: number; comment: string }[]
  strengths: string[]
  weaknesses: string[]
  instructorSummary: string
  reportTitle: string
  courseLinks: CourseLink[]
}
