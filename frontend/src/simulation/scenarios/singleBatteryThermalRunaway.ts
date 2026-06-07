import type { DeductionScenarioDef } from '../types/deduction.types'

/** 单电池热失控处置 — 标准场景配置 */
export const singleBatteryThermalRunaway: DeductionScenarioDef = {
  id: '30000000-0000-0000-0000-000000000001',
  name: '单电池热失控处置',
  description:
    '模拟储能舱内单颗电芯温度异常升高，学员需在黄金窗口内完成隔离、通风或灭火决策，避免热扩散。',
  durationSec: 300,
  initialConditions: {
    cellCount: 1,
    initialTemperature: 35,
    batteryType: 'lithium_iron_phosphate',
    capacityMWh: 0.28,
  },
  thermalModel: {
    baseRiseRate: 0.35,
    runawayThreshold: 120,
    runawayRate: 2.5,
  },
  decisionPoints: [
    {
      id: 'dp-early-warning',
      phase: 'earlyWarning',
      question: 'BMS 告警：#1 电芯温度 62°C 且持续上升，请选择首要处置措施。',
      timePressureSec: 30,
      triggerTemp: 60,
      options: [
        {
          id: 'opt-observe',
          label: '继续观察',
          description: '等待 BMS 二次确认',
          isOptimal: false,
          scoreDelta: -15,
        },
        {
          id: 'opt-vent',
          label: '启动通风降温和气体监测',
          description: '开启排风，降低可燃气体浓度',
          isOptimal: true,
          targetPhase: 'venting',
          scoreDelta: 20,
        },
        {
          id: 'opt-isolate',
          label: '立即电气隔离',
          description: '断开该簇直流回路',
          isOptimal: true,
          targetPhase: 'isolation',
          scoreDelta: 25,
        },
      ],
    },
    {
      id: 'dp-thermal-runaway',
      phase: 'thermalRunaway',
      question: '电芯温度突破 120°C，冒烟明显，请选择应急措施。',
      timePressureSec: 20,
      triggerTemp: 120,
      options: [
        {
          id: 'opt-extinguish',
          label: '启动气体灭火/气溶胶',
          description: '按 SOP 启动固定灭火系统',
          isOptimal: true,
          targetPhase: 'fireSuppression',
          scoreDelta: 25,
        },
        {
          id: 'opt-evacuate',
          label: '人员撤离并远程监控',
          description: '确保人员安全，等待专业队伍',
          isOptimal: true,
          targetPhase: 'evacuation',
          scoreDelta: 15,
        },
        {
          id: 'opt-open-door',
          label: '打开舱门直接查看',
          description: '现场目视确认',
          isOptimal: false,
          scoreDelta: -30,
        },
      ],
    },
    {
      id: 'dp-isolation-confirm',
      phase: 'isolation',
      question: '隔离指令已下发，请确认后续操作。',
      timePressureSec: 15,
      options: [
        {
          id: 'opt-confirm-off',
          label: '确认断电完成并启动通风',
          isOptimal: true,
          scoreDelta: 20,
        },
        {
          id: 'opt-skip',
          label: '跳过确认继续运行',
          isOptimal: false,
          scoreDelta: -20,
        },
      ],
    },
  ],
}
