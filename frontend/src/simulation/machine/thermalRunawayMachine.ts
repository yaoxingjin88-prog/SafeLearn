import { setup, assign } from 'xstate'
import type {
  DeductionContext,
  DeductionEvent,
  DeductionPhase,
  CellState,
  DecisionPointDef,
  UserDecisionRecord,
  DeductionScenarioDef,
} from '../types/deduction.types'

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

function resolveHotspotId(scenario: DeductionScenarioDef): number {
  const configured = scenario.thermalModel.hotspotCellId
  if (configured != null) return Math.min(configured, scenario.initialConditions.cellCount - 1)
  return Math.floor(scenario.initialConditions.cellCount / 2)
}

function spreadRadius(cellCount: number): number {
  if (cellCount <= 16) return 3
  if (cellCount <= 64) return 5
  return 8
}

export function createThermalRunawayMachine(scenario: DeductionScenarioDef) {
  const hotspotId = resolveHotspotId(scenario)
  const isMultiCell = scenario.initialConditions.cellCount > 1

  function findDecisionPoint(phase: DeductionPhase): DecisionPointDef | null {
    return scenario.decisionPoints.find(dp => dp.phase === phase) ?? null
  }

  function earlyWarningDp(): DecisionPointDef | undefined {
    return scenario.decisionPoints.find(dp => dp.phase === 'earlyWarning')
  }

  function runawayDp(): DecisionPointDef | undefined {
    return scenario.decisionPoints.find(dp => dp.phase === 'thermalRunaway')
  }

  function earlyWarningThreshold(): number {
    return earlyWarningDp()?.triggerTemp ?? 60
  }

  function elapsedSec(context: DeductionContext): number {
    return context.elapsedMs / 1000
  }

  function advanceCells(context: DeductionContext, deltaMs: number): CellState[] {
    const model = scenario.thermalModel
    const hotspot = context.cells[hotspotId]
    const radius = spreadRadius(scenario.initialConditions.cellCount)

    return context.cells.map((cell, i) => {
      let rate = 0
      if (i === hotspotId) {
        rate = model.baseRiseRate
        if (cell.temperature >= model.runawayThreshold) rate = model.runawayRate
      } else if (hotspot && hotspot.temperature >= (model.spreadThreshold ?? 999)) {
        const dist = Math.abs(i - hotspotId)
        if (dist <= radius) rate = (model.spreadRate ?? 0.15) / (dist + 0.5)
      }

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
  }

  return setup({
    types: {
      context: {} as DeductionContext,
      events: {} as DeductionEvent,
    },
    actions: {
      initSession: assign(({ event }) => {
        if (event.type !== 'START') return {}
        const { cellCount, initialTemperature } = scenario.initialConditions
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
          score: 50,
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
        const cells = advanceCells(context, deltaMs)
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
      setPendingDecision: assign(({ self }) => {
        const statePhase = self.getSnapshot().value as DeductionPhase
        return {
          phase: statePhase,
          pendingDecision: findDecisionPoint(statePhase),
        }
      }),
      syncPhase: assign(({ self }) => ({
        phase: self.getSnapshot().value as DeductionPhase,
      })),
      clearPendingDecision: assign({ pendingDecision: null }),
      recordDecision: assign(({ context, event }) => {
        if (event.type !== 'DECISION') return {}
        const dp = context.pendingDecision
          ?? scenario.decisionPoints.find(d => d.id === event.decisionPointId)
          ?? null
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
        const nextPhase = option.targetPhase ?? context.phase
        const outcome = option.isOptimal === false && (option.scoreDelta ?? 0) < -10
          ? 'failure' as const
          : context.outcome
        return {
          decisions: [...context.decisions, record],
          score: Math.max(0, Math.min(100, context.score + (option.scoreDelta ?? 0))),
          branch,
          phase: nextPhase,
          outcome,
          pendingDecision: null,
          environment: {
            ...context.environment,
            ventilationOn: context.environment.ventilationOn || option.targetPhase === 'venting',
            isolationComplete: context.environment.isolationComplete || option.targetPhase === 'isolation',
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
      shouldTriggerEarlyDecision: ({ context }) => {
        const dp = earlyWarningDp()
        if (!dp) return false
        const hotspot = context.cells[hotspotId]
        const byTemp = (hotspot?.temperature ?? 0) >= earlyWarningThreshold()
        const byTime = dp.triggerTimeSec != null && elapsedSec(context) >= dp.triggerTimeSec
        return byTemp || byTime
      },
      shouldEnterRunaway: ({ context }) => {
        const dp = runawayDp()
        const byTime = dp?.triggerTimeSec != null && elapsedSec(context) >= dp.triggerTimeSec
        if (!isMultiCell) {
          return byTime || (context.cells[0]?.temperature ?? 0) >= scenario.thermalModel.runawayThreshold
        }
        const affected = context.cells.filter(
          c => c.status === 'warning' || c.status === 'danger' || c.status === 'critical',
        ).length
        const maxTemp = Math.max(...context.cells.map(c => c.temperature), 0)
        return byTime || affected >= 2 || maxTemp >= scenario.thermalModel.runawayThreshold
      },
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
        return opt?.isOptimal === false && (opt.scoreDelta ?? 0) < 0
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
          TICK: { actions: 'advanceTime' },
          DECISION: { actions: 'recordDecision' },
          PAUSE: {},
          FINISH: 'completed',
        },
      },
      earlyWarning: {
        entry: 'setPendingDecision',
        on: {
          TICK: { actions: 'advanceTime' },
          DECISION: [
            { guard: 'isBadDecision', target: 'escalation', actions: ['recordDecision', 'markFailure'] },
            { guard: 'branchVenting', target: 'venting', actions: 'recordDecision' },
            { guard: 'branchIsolation', target: 'isolation', actions: 'recordDecision' },
            { guard: 'isFireChoice', target: 'fireSuppression', actions: 'recordDecision' },
            { guard: 'isEvacuateChoice', target: 'evacuation', actions: 'recordDecision' },
            { target: 'monitoring', actions: 'recordDecision' },
          ],
          TIMEOUT: { target: 'escalation', actions: 'markFailure' },
        },
      },
      venting: {
        entry: 'syncPhase',
        on: {
          TICK: { actions: 'advanceTime' },
          DECISION: { actions: 'recordDecision' },
        },
      },
      isolation: {
        entry: assign({
          phase: 'isolation' as DeductionPhase,
        }),
        on: {
          TICK: { actions: 'advanceTime' },
          DECISION: { actions: 'recordDecision' },
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
        entry: 'syncPhase',
        on: {
          TICK: { actions: 'advanceTime' },
          DECISION: { target: 'contained', actions: ['recordDecision', 'markSuccess'] },
        },
        after: { 5000: { target: 'contained', actions: 'markSuccess' } },
      },
      evacuation: {
        entry: 'syncPhase',
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
}

/** 默认单电池场景状态机（兼容旧引用） */
import { singleBatteryThermalRunaway } from '../scenarios/singleBatteryThermalRunaway'
export const thermalRunawayMachine = createThermalRunawayMachine(singleBatteryThermalRunaway)
