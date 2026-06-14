/** 北京丰台「4·16」储能电站事故 — 电站运营场景应急决策训练 */
export const beijing416Training = {
  id: "50000000-0000-0000-0000-000000000001",
  caseId: "40000000-0000-0000-0000-000000000003",
  name: "北京丰台「4·16」应急决策训练",
  sceneType: "station_operation" as const,
  sceneLabel: "电站运营",
  riskLevel: "critical" as const,
  difficulty: 3,
  duration: 300,
  description:
    "基于2021年北京丰台储能电站火灾爆炸特别重大事故，还原并网运行值班场景下的预警响应、热失控处置、火势蔓延指挥与爆炸后应急决策。",
  decisionPoints: [
    {
      id: "bj_dp1",
      timelinePhase: "T+5min · 12:05",
      triggerCondition:
        "储能系统并网运行中，2#储能舱 BMS 发出一级预警：舱内温度由 28℃ 缓慢升至 42℃，气体探测器 H₂ 读数轻微波动至 15ppm，现场无明显异味，值班员正在例行巡检。",
      question: "作为当班负责人，收到一级预警后首先应采取哪项处置？",
      timePressure: 90,
      regulationRef: "GB/T 42288-2022 电化学储能电站安全规程",
      explanation:
        '4·16 事故直接技术原因为内短路引发热失控，管理缺陷包括巡检缺失与预警响应迟缓。一级预警即表明能量异常输入或散热失衡，必须立即远程切断故障舱直流回路并派员外围确认，而非"继续观察"。延误 10–15 分钟往往意味着热失控进入不可控阶段，是事故链扩大的首要诱因。',
      options: [
        {
          id: "bj_dp1_a",
          text: "立即远程切断 2# 舱直流回路，派员着防护装备在外围巡查确认，同步通知站长",
          score: 30,
          correct: true,
        },
        {
          id: "bj_dp1_b",
          text: "继续并网运行，将 BMS 采样频率调高，观察 15 分钟待温度自行回落",
          score: 0,
          correct: false,
        },
        {
          id: "bj_dp1_c",
          text: "仅记录告警日志并发送邮件报备，等待站长到场后再决定",
          score: 5,
          correct: false,
        },
        {
          id: "bj_dp1_d",
          text: "远程调高 BMS 温度告警阈值以消除误报，维持并网并加密巡检频次",
          score: 0,
          correct: false,
        },
      ],
    },
    {
      id: "bj_dp2",
      timelinePhase: "T+15min · 12:25",
      triggerCondition:
        "现场确认 2# 舱温度骤升至 78℃，单体电压离散度明显增大，舱门接缝处出现白色烟雾，H₂ 浓度 120ppm，疑似电芯内短路触发自发热，热失控已萌芽。",
      question: "舱内已出现白雾且温度持续攀升，下一步最恰当的处置是？",
      timePressure: 60,
      regulationRef: "DL/T 2528-2022 电化学储能电站应急预案编制导则",
      explanation:
        "白雾是可燃电解液蒸气与热失控气体的伴随征象。打开舱门通风会引入氧气，加剧热失控反应——这是储能火灾处置中最常见的致命错误之一。4·16 教训要求消防系统与 BMS 联动、应急通风与人员撤离同步执行。此时应启动气体消防与通风联锁，划定禁区，人员撤至上风向，严禁无防护近距离作业。",
      options: [
        {
          id: "bj_dp2_a",
          text: "启动应急通风与气体消防联锁，划定安全禁区，组织人员撤离至上风向并持续监测气体",
          score: 30,
          correct: true,
        },
        {
          id: "bj_dp2_b",
          text: "打开舱门加强自然通风，手提干粉灭火器进入舱内查找冒烟点",
          score: 0,
          correct: false,
        },
        {
          id: "bj_dp2_c",
          text: '仅远程调整 BMS 降温参数，不撤离现场人员以便"随时处置"',
          score: 5,
          correct: false,
        },
        {
          id: "bj_dp2_d",
          text: "在舱外架设临时风机向舱内送风降温，人员在外围持续监测",
          score: 0,
          correct: false,
        },
      ],
    },
    {
      id: "bj_dp3",
      timelinePhase: "T+25min · 12:40",
      triggerCondition:
        "热失控向相邻电池簇蔓延，2# 舱烟雾浓度显著升高，舱内温度突破 145℃，部分直流回路尚未完全隔离，若不及时阻断能量与蔓延路径，爆燃风险急剧上升。",
      question: "火势呈蔓延态势，作为现场指挥应如何决策？",
      timePressure: 45,
      regulationRef: "GB 50016 建筑设计防火规范（储能部分）",
      explanation:
        '事故进入扩散阶段后，单一手段已不足以控制局面。必须执行"全面断电隔离 + 边界冷却 + 分级上报"的组合拳：切断故障区域及可能受牵连回路的能量输入，对相邻舱体实施边界冷却，同时启动公司级应急预案并通知消防。用水枪近距离作业存在触电与有毒气体暴露风险；消极等待则错过遏制窗口，4·16 事故即因处置链条断裂导致爆炸后果扩大。',
      options: [
        {
          id: "bj_dp3_a",
          text: "全面断电隔离故障区域，对相邻舱实施边界冷却，上报并启动公司级应急预案",
          score: 35,
          correct: true,
        },
        {
          id: "bj_dp3_b",
          text: "集中水枪冷却 2# 舱外壳，运维人员近距离持续作业",
          score: 0,
          correct: false,
        },
        {
          id: "bj_dp3_c",
          text: "等待消防队到场，现场不作任何隔离与冷却处置",
          score: 5,
          correct: false,
        },
        {
          id: "bj_dp3_d",
          text: "仅暂停 2# 舱充放电，相邻舱维持运行并远程加大通风避免全站停运",
          score: 0,
          correct: false,
        },
      ],
    },
    {
      id: "bj_dp4",
      timelinePhase: "T+35min · 13:10",
      triggerCondition:
        "可燃气体积聚达到爆炸下限，2# 舱发生爆燃，冲击波损毁相邻设备，明火燃烧，消防力量正在赶赴现场，需防止二次伤亡与复燃。",
      question: "爆炸发生后，现场指挥的首要任务是什么？",
      timePressure: 45,
      regulationRef: "《生产安全事故应急条例》",
      explanation:
        '4·16 事故造成重大人员伤亡，明火持续至当日 23:40，冷却监护约 40 小时。爆炸后核心原则是：清点人员、划定警戒、移交专业消防、制定长期冷却与复燃监测方案。严禁为减少财产损失让未防护人员进入核心区；亦不可在未评估结构安全前恢复相邻舱并网运行。此次事故教训：爆炸后处置周期远超初期灭火，须按"长期监护"而非"一战而定"组织应急。',
      options: [
        {
          id: "bj_dp4_a",
          text: "清点人员、划定警戒区，移交专业消防处置，启动长时间冷却与复燃监测方案",
          score: 35,
          correct: true,
        },
        {
          id: "bj_dp4_b",
          text: "组织非专业人员进入核心区搬运设备，减少财产损失",
          score: 0,
          correct: false,
        },
        {
          id: "bj_dp4_c",
          text: "仅对受损舱断电，其余舱继续并网运行以保障供电",
          score: 5,
          correct: false,
        },
        {
          id: "bj_dp4_d",
          text: "组织佩戴简易防护的运维人员进入核心区关闭阀门、切断剩余回路",
          score: 0,
          correct: false,
        },
      ],
    },
  ],
};
