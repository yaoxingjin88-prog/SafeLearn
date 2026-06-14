import type { DeductionScenarioDef } from '../types/deduction.types'

/** 电池组热扩散应急 — L2 进阶场景 */
export const batteryGroupThermalDiffusion: DeductionScenarioDef = {
  id: '30000000-0000-0000-0000-000000000002',
  name: '电池组热扩散应急',
  description:
    '模拟电池组中一颗电芯热失控后向相邻电芯扩散，训练模组隔离、区域消防与全站响应决策。',
  durationSec: 120,
  initialConditions: {
    cellCount: 16,
    initialTemperature: 30,
    batteryType: 'ternary_lithium',
    capacityMWh: 1,
  },
  thermalModel: {
    baseRiseRate: 0.45,
    runawayThreshold: 110,
    runawayRate: 2.8,
    spreadThreshold: 65,
    spreadRate: 0.25,
    hotspotCellId: 7,
  },
  decisionPoints: [
    {
      id: 'dp-l2-early',
      phase: 'earlyWarning',
      question: 'BMS 告警：中心电芯温度异常升高，请选择首要处置措施。',
      timePressureSec: 25,
      triggerTemp: 58,
      triggerTimeSec: 15,
      order: 1,
      options: [
        {
          id: 'opt-l2-isolate-module',
          label: '切断该电池模组电源并启动冷却',
          description: '隔离故障模组，防止能量继续输入',
          isOptimal: true,
          targetPhase: 'isolation',
          scoreDelta: 30,
        },
        {
          id: 'opt-l2-isolate-all',
          label: '切断整个电池组电源',
          description: '范围过大，可能影响正常供电',
          isOptimal: false,
          scoreDelta: 10,
        },
        {
          id: 'opt-l2-monitor',
          label: '仅加强监控不断电',
          description: '错失早期干预窗口',
          isOptimal: false,
          scoreDelta: -15,
        },
        {
          id: 'opt-l2-reboot-bms',
          label: '重启 BMS 系统',
          description: '无法解决热失控根源',
          isOptimal: false,
          scoreDelta: -20,
        },
      ],
    },
    {
      id: 'dp-l2-spread',
      phase: 'thermalRunaway',
      question: '热失控开始向相邻电芯扩散，请立即决策。',
      timePressureSec: 20,
      triggerTimeSec: 40,
      order: 2,
      options: [
        {
          id: 'opt-l2-region-fire',
          label: '启动区域隔离和消防系统',
          description: '封锁扩散路径并启动固定灭火',
          isOptimal: true,
          targetPhase: 'fireSuppression',
          scoreDelta: 35,
        },
        {
          id: 'opt-l2-force-cool',
          label: '对所有电芯强制冷却',
          description: '资源分散，效果有限',
          isOptimal: false,
          scoreDelta: 8,
        },
        {
          id: 'opt-l2-evacuate-only',
          label: '仅疏散人员',
          description: '未采取遏制措施',
          isOptimal: false,
          scoreDelta: -5,
        },
        {
          id: 'opt-l2-shutdown-station',
          label: '关闭整个电站',
          description: '响应过度且耗时',
          isOptimal: false,
          scoreDelta: 5,
        },
      ],
    },
  ],
}
