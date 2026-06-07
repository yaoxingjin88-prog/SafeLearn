import { setup, assign } from 'xstate'
import type {
  DeductionContext,
  DeductionEvent,
  DeductionPhase,
  CellState,
  DecisionPointDef,
  UserDecisionRecord,
} from '../types/deduction.types'
import { singleBatteryThermalRunaway } from '../scenarios/singleBatteryThermalRunaway'

function createInitialCells(count: number, temp: number): CellState[] {
  return Array.from({ length: count }, (_, i) => ({
    id: i,
    temperature: temp,
    status: 'normal' as const,
    smokeIntensity: 0,
    fireIntensity: 0,
    voltage: 3.2,
    current: 0.5,
  }))
}

function findDecisionPoint(phase: DeductionPhase): DecisionPointDef | null {
  return singleBatteryThermalRunaway.decisionPoints.find(dp => dp.phase === phase) ?? null
}

export const thermalRunawayMachine = setup({
  types: {
    context: {} as DeductionContext,
    events: {} as DeductionEvent,
  },
  actions: {
    initSession: assign(({ event }) => {
      if (event.type !== 'START') return {}
      const { cellCount, initialTemperature } = singleBatteryThermalRunaway.initialConditions
      return {
        sessionId: event.sessionId,
        scenarioId: event.scenarioId,
        elapsedMs: 0,
        phase: 'monitoring' as DeductionPhase,
        cells: createInitialCells(cellCount, initialTemperature),
        environment: {
          temperature: 25,
          humidity: 45,
          gasLevel: 100,
          ventilationOn: false,
          isolationComplete: false,
        },
        decisions: [] as UserDecisionRecord[],
        score: 0,
        maxTemperature: initialTemperature,
        branch: 'none' as const,
        outcome: 'pending' as const,
        pendingDecision: null,
        eventSeq: 0,
      }
    }),
    advanceTime: assign(({ context, event }) => {
      if (event.type !== 'TICK') return {}
      const deltaMs = event.deltaMs
      const elapsedMs = context.elapsedMs + deltaMs
      const model = singleBatteryThermalRunaway.thermalModel
      const cells = context.cells.map(cell => {
        let rate = model.baseRiseRate
        if (cell.temperature >= model.runawayThreshold) rate = model.runawayRate
        if (context.environment.ventilationOn) rate *= 0.6
        if (context.environment.isolationComplete) rate *= 0.5
        const temperature = Math.min(800, cell.temperature + rate * (deltaMs / 1000))
        let status = cell.status
        if (temperature >= 200) status = 'critical'
        else if (temperature >= 120) status = 'danger'
        else if (temperature >= 60) status = 'warning'
        else status = 'normal'
        const smokeIntensity = temperature >= 80 ? Math.min(1, (temperature - 80) / 120) : 0
        const fireIntensity = temperature >= 200 ? Math.min(1, (temperature - 200) / 300) : 0
        return { ...cell, temperature, status, smokeIntensity, fireIntensity }
      })
      const maxTemperature = Math.max(...cells.map(c => c.temperature), context.maxTemperature)
      const gasLevel = Math.min(800, context.environment.gasLevel + deltaMs / 150)
      return {
        elapsedMs,
        cells,
        maxTemperature,
        eventSeq: context.eventSeq + 1,
        environment: { ...context.environment, gasLevel },
      }
    }),
    setPendingDecision: assign(({ context }) => ({
      pendingDecision: findDecisionPoint(context.phase),
    })),
    clearPendingDecision: assign({ pendingDecision: null }),
    recordDecision: assign(({ context, event }) => {
      if (event.type !== 'DECISION') return {}
      const dp = context.pendingDecision
      if (!dp) return {}
      const option = dp.options.find(o => o.id === event.optionId)
      if (!option) return {}
      const record: UserDecisionRecord = {
        decisionPointId: event.decisionPointId,
        optionId: event.optionId,
        elapsedMs: context.elapsedMs,
        responseTimeMs: context.elapsedMs,
        scoreDelta: option.scoreDelta ?? 0,
      }
      const branch = option.targetPhase === 'venting'
        ? 'venting'
        : option.targetPhase === 'isolation'
          ? 'isolation'
          : option.targetPhase === 'fireSuppression'
            ? 'fire'
            : option.targetPhase === 'evacuation'
              ? 'evacuate'
              : context.branch
      return {
        decisions: [...context.decisions, record],
        score: context.score + (option.scoreDelta ?? 0),
        branch,
        pendingDecision: null,
        environment: {
          ...context.environment,
          ventilationOn: option.id === 'opt-vent' || option.id === 'opt-confirm-off' ? true : context.environment.ventilationOn,
          isolationComplete: option.id === 'opt-isolate' || option.id === 'opt-confirm-off' ? true : context.environment.isolationComplete,
        },
        eventSeq: context.eventSeq + 1,
      }
    }),
    markSuccess: assign({ outcome: 'success' as const, phase: 'contained' as DeductionPhase }),
    markFailure: assign({ outcome: 'failure' as const, phase: 'escalation' as DeductionPhase }),
    completeSession: assign({ phase: 'completed' as DeductionPhase }),
    enterReplay: assign({ phase: 'replay' as DeductionPhase }),
  },
  guards: {
    tempReachEarlyWarning: ({ context }) => context.cells[0]?.temperature >= 60,
    tempReachRunaway: ({ context }) => context.cells[0]?.temperature >= 120,
    isOptimalDecision: ({ context, event }) => {
      if (event.type !== 'DECISION') return false
      const dp = context.pendingDecision
      const opt = dp?.options.find(o => o.id === event.optionId)
      return opt?.isOptimal === true
    },
    isBadDecision: ({ context, event }) => {
      if (event.type !== 'DECISION') return false
      const dp = context.pendingDecision
      const opt = dp?.options.find(o => o.id === event.optionId)
      return opt?.isOptimal === false
    },
    branchVenting: ({ context, event }) => {
      if (event.type !== 'DECISION') return false
      const opt = context.pendingDecision?.options.find(o => o.id === event.optionId)
      return opt?.targetPhase === 'venting'
    },
    branchIsolation: ({ context, event }) => {
      if (event.type !== 'DECISION') return false
      const opt = context.pendingDecision?.options.find(o => o.id === event.optionId)
      return opt?.targetPhase === 'isolation'
    },
    isFireChoice: ({ context, event }) => {
      if (event.type !== 'DECISION') return false
      const opt = context.pendingDecision?.options.find(o => o.id === event.optionId)
      return opt?.targetPhase === 'fireSuppression'
    },
    isEvacuateChoice: ({ context, event }) => {
      if (event.type !== 'DECISION') return false
      const opt = context.pendingDecision?.options.find(o => o.id === event.optionId)
      return opt?.targetPhase === 'evacuation'
    },
  },
}).createMachine({
  id: 'thermalRunaway',
  initial: 'idle',
  context: {
    sessionId: '',
    scenarioId: '',
    elapsedMs: 0,
    phase: 'idle',
    cells: [],
    environment: {
      temperature: 25,
      humidity: 45,
      gasLevel: 100,
      ventilationOn: false,
      isolationComplete: false,
    },
    decisions: [],
    score: 0,
    maxTemperature: 0,
    branch: 'none',
    outcome: 'pending',
    pendingDecision: null,
    eventSeq: 0,
  },
  states: {
    idle: {
      on: {
        START: {
          target: 'monitoring',
          actions: 'initSession',
        },
      },
    },
    monitoring: {
      on: {
        TICK: [
          {
            guard: 'tempReachEarlyWarning',
            target: 'earlyWarning',
            actions: ['advanceTime', 'setPendingDecision'],
          },
          { actions: 'advanceTime' },
        ],
        PAUSE: {},
        FINISH: 'completed',
      },
    },
    earlyWarning: {
      on: {
        TICK: { actions: 'advanceTime' },
        DECISION: [
          { guard: 'isBadDecision', target: 'escalation', actions: ['recordDecision', 'markFailure'] },
          { guard: 'branchVenting', target: 'venting', actions: 'recordDecision' },
          { guard: 'branchIsolation', target: 'isolation', actions: 'recordDecision' },
          { target: 'monitoring', actions: 'recordDecision' },
        ],
        TIMEOUT: { target: 'escalation', actions: 'markFailure' },
      },
    },
    venting: {
      on: {
        TICK: [
          {
            guard: 'tempReachRunaway',
            target: 'thermalRunaway',
            actions: ['advanceTime', 'setPendingDecision'],
          },
          { actions: 'advanceTime' },
        ],
        DECISION: { actions: 'recordDecision' },
      },
    },
    isolation: {
      entry: 'setPendingDecision',
      on: {
        TICK: { actions: 'advanceTime' },
        DECISION: [
          { guard: 'isOptimalDecision', target: 'contained', actions: ['recordDecision', 'markSuccess'] },
          { guard: 'isBadDecision', target: 'escalation', actions: ['recordDecision', 'markFailure'] },
        ],
        TIMEOUT: { target: 'escalation', actions: 'markFailure' },
      },
    },
    thermalRunaway: {
      entry: 'setPendingDecision',
      on: {
        TICK: { actions: 'advanceTime' },
        DECISION: [
          { guard: 'isOptimalDecision', target: 'fireSuppression', actions: 'recordDecision' },
          { guard: 'isFireChoice', target: 'fireSuppression', actions: 'recordDecision' },
          { guard: 'isBadDecision', target: 'escalation', actions: ['recordDecision', 'markFailure'] },
          { guard: 'isEvacuateChoice', target: 'evacuation', actions: 'recordDecision' },
        ],
        TIMEOUT: { target: 'escalation', actions: 'markFailure' },
      },
    },
    fireSuppression: {
      on: {
        TICK: { actions: 'advanceTime' },
        DECISION: { target: 'contained', actions: ['recordDecision', 'markSuccess'] },
      },
      after: { 5000: { target: 'contained', actions: 'markSuccess' } },
    },
    evacuation: {
      after: { 3000: { target: 'contained', actions: 'markSuccess' } },
    },
    contained: {
      always: { target: 'completed', actions: 'completeSession' },
    },
    escalation: {
      always: { target: 'completed', actions: 'completeSession' },
    },
    completed: {
      on: {
        REPLAY: { target: 'replay', actions: 'enterReplay' },
      },
    },
    replay: {
      on: {
        SEEK: { actions: 'advanceTime' },
        START: { target: 'monitoring', actions: 'initSession' },
      },
    },
  },
})
