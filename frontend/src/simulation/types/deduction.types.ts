/** 事故推演引擎 — 核心类型定义 */

export type DeductionPhase =
  | 'idle'
  | 'monitoring'
  | 'earlyWarning'
  | 'venting'
  | 'isolation'
  | 'thermalRunaway'
  | 'fireSuppression'
  | 'evacuation'
  | 'contained'
  | 'escalation'
  | 'completed'
  | 'replay'

export type CellStatus = 'normal' | 'warning' | 'danger' | 'critical' | 'failed'

export interface CellState {
  id: number
  temperature: number
  status: CellStatus
  smokeIntensity: number
  fireIntensity: number
  voltage: number
  current: number
}

export interface EnvironmentState {
  temperature: number
  humidity: number
  gasLevel: number
  ventilationOn: boolean
  isolationComplete: boolean
}

export interface DecisionOptionDef {
  id: string
  label: string
  description?: string
  /** 规则引擎：是否正确选项 */
  isOptimal?: boolean
  /** 跳转目标阶段（可选，否则由状态机 guard 决定） */
  targetPhase?: DeductionPhase
  scoreDelta?: number
}

export interface DecisionPointDef {
  id: string
  phase: DeductionPhase
  question: string
  options: DecisionOptionDef[]
  timePressureSec: number
  /** 达到该温度时触发决策（单点/热点电芯） */
  triggerTemp?: number
  /** 达到该推演秒数时触发决策（兜底） */
  triggerTimeSec?: number
  /** 决策序号，按从小到大依次触发 */
  order: number
}

export interface UserDecisionRecord {
  decisionPointId: string
  optionId: string
  elapsedMs: number
  responseTimeMs: number
  scoreDelta: number
}

export interface DeductionContext {
  sessionId: string
  scenarioId: string
  elapsedMs: number
  phase: DeductionPhase
  cells: CellState[]
  environment: EnvironmentState
  decisions: UserDecisionRecord[]
  score: number
  maxTemperature: number
  branch: 'none' | 'venting' | 'isolation' | 'fire' | 'evacuate'
  outcome: 'pending' | 'success' | 'failure'
  pendingDecision: DecisionPointDef | null
  eventSeq: number
}

export type DeductionEvent =
  | { type: 'START'; sessionId: string; scenarioId: string }
  | { type: 'TICK'; deltaMs: number }
  | { type: 'DECISION'; decisionPointId: string; optionId: string }
  | { type: 'TIMEOUT' }
  | { type: 'PAUSE' }
  | { type: 'RESUME' }
  | { type: 'SEEK'; elapsedMs: number }
  | { type: 'REPLAY' }
  | { type: 'FINISH' }

/** 3D / UI 联动事件 */
export type DeductionSceneEventType =
  | 'cell:temperature'
  | 'cell:status'
  | 'fx:smoke'
  | 'fx:fire'
  | 'fx:ventilation'
  | 'fx:isolation'
  | 'env:gasLevel'
  | 'ui:alert'
  | 'camera:focus'
  | 'phase:change'

export interface DeductionSceneEvent {
  seq: number
  elapsedMs: number
  type: DeductionSceneEventType
  payload: Record<string, unknown>
}

/** 事件溯源 — 持久化 & 回放 */
export interface DeductionEventLogEntry {
  seq: number
  sessionId: string
  elapsedMs: number
  eventType: string
  machineState: DeductionPhase
  payload: Record<string, unknown>
  createdAt?: string
}

export interface SimulationSnapshot {
  seq: number
  elapsedMs: number
  phase: DeductionPhase
  cells: CellState[]
  environment: EnvironmentState
  score: number
}

export interface DeductionScenarioDef {
  id: string
  name: string
  description: string
  durationSec: number
  initialConditions: {
    cellCount: number
    initialTemperature: number
    batteryType: string
    capacityMWh: number
  }
  decisionPoints: DecisionPointDef[]
  /** 温度曲线参数 */
  thermalModel: {
    baseRiseRate: number
    runawayThreshold: number
    runawayRate: number
    /** 热扩散触发温度（多电池场景） */
    spreadThreshold?: number
    spreadRate?: number
    hotspotCellId?: number
  }
}

export interface ScoreDimension {
  score: number
  comment: string
}

export interface AiScoreReport {
  totalScore: number
  rating: 'excellent' | 'good' | 'average' | 'poor'
  ruleScore: number
  aiScore: number
  dimensions: {
    timeliness: ScoreDimension
    procedure: ScoreDimension
    decision: ScoreDimension
    outcome: ScoreDimension
  }
  highlights: string[]
  improvements: string[]
  instructorSummary: string
}

export interface DeductionSessionSummary {
  sessionId: string
  scenarioId: string
  scenarioName: string
  outcome: 'success' | 'failure'
  totalScore: number
  rating: string
  durationSec: number
  decisionCount: number
  maxTemperature: number
  finishedAt: string
}
