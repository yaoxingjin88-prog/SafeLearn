import type { DeductionScenarioDef } from '../types/deduction.types'

/** 储能舱火灾处置 — L3 高级场景 */
export const energyStorageCabinFire: DeductionScenarioDef = {
  id: '30000000-0000-0000-0000-000000000003',
  name: '储能舱火灾处置',
  description:
    '模拟大型储能舱局部异常升级至全舱火灾，训练应急预案启动、全舱消防与人员撤离指挥决策。',
  durationSec: 180,
  initialConditions: {
    cellCount: 256,
    initialTemperature: 35,
    batteryType: 'ternary_lithium',
    capacityMWh: 10,
  },
  thermalModel: {
    baseRiseRate: 0.3,
    runawayThreshold: 100,
    runawayRate: 2.0,
    spreadThreshold: 58,
    spreadRate: 0.12,
    hotspotCellId: 128,
  },
  decisionPoints: [
    {
      id: 'dp-l3-local',
      phase: 'earlyWarning',
      question: '巡检发现储能舱局部温度异常，如何处置？',
      timePressureSec: 30,
      triggerTemp: 52,
      triggerTimeSec: 20,
      order: 1,
      options: [
        {
          id: 'opt-l3-emergency-cut',
          label: '立即启动应急预案，切断故障区域电源',
          description: '按 SOP 隔离故障分区',
          isOptimal: true,
          targetPhase: 'isolation',
          scoreDelta: 30,
        },
        {
          id: 'opt-l3-patrol-more',
          label: '加强巡检频次，暂不处理',
          description: '延误处置时机',
          isOptimal: false,
          scoreDelta: -15,
        },
        {
          id: 'opt-l3-bms-tune',
          label: '远程调整 BMS 参数降温',
          description: '无法应对真实热失控',
          isOptimal: false,
          scoreDelta: 5,
        },
        {
          id: 'opt-l3-shutdown-cabin',
          label: '关闭整个储能舱',
          description: '未精确定位故障区域',
          isOptimal: false,
          scoreDelta: 8,
        },
      ],
    },
    {
      id: 'dp-l3-smoke',
      phase: 'thermalRunaway',
      question: '储能舱内大量烟雾产生，请指挥现场处置。',
      timePressureSec: 15,
      triggerTimeSec: 60,
      order: 2,
      options: [
        {
          id: 'opt-l3-full-fire-evac',
          label: '启动全舱消防系统，全员撤离',
          description: '生命第一，同步启动灭火',
          isOptimal: true,
          targetPhase: 'evacuation',
          scoreDelta: 35,
        },
        {
          id: 'opt-l3-enter-check',
          label: '派人进入查看情况',
          description: '严重违规，危及人员安全',
          isOptimal: false,
          scoreDelta: -35,
        },
        {
          id: 'opt-l3-vent-only',
          label: '仅启动通风排烟',
          description: '可能助燃，未启动灭火',
          isOptimal: false,
          scoreDelta: -5,
        },
        {
          id: 'opt-l3-close-vent',
          label: '关闭通风系统防止扩散',
          description: '气体积聚风险更高',
          isOptimal: false,
          scoreDelta: -10,
        },
      ],
    },
  ],
}
