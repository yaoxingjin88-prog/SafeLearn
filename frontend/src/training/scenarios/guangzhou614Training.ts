/** 广州智光「6·14」锂电池包闪爆事故 — 测试作业场景应急决策训练 */
export const guangzhou614Training = {
  id: '50000000-0000-0000-0000-000000000002',
  caseId: '40000000-0000-0000-0000-000000000005',
  name: '广州智光「6·14」应急决策训练',
  sceneType: 'test_operation' as const,
  sceneLabel: '测试作业',
  riskLevel: 'critical' as const,
  difficulty: 3,
  duration: 240,
  description:
    '基于2024年广州黄埔区锂电池包闪爆特别重大事故，还原储能测试作业场景下从开工确认、异常征兆、热失控发展到闪爆后响应的全流程决策。',
  decisionPoints: [
    {
      id: 'gz_dp1',
      timelinePhase: 'T0 · 14:00 测试前',
      triggerCondition:
        '室内测试区气温 32℃，即将对 1# 磷酸铁锂电池包开展充放电循环与容量标定测试。现场有测试工程师 2 人、监护 1 人，BMS 与气体监测在线。',
      question: '测试开始前，必须完成哪项安全确认？',
      timePressure: 90,
      regulationRef: '《储能系统测试安全规范》',
      explanation:
        '6·14 事故调查报告指出：测试环节管理失控、操作人员无证上岗、应急预案未演练是重要间接原因。测试作业的风险远高于日常运维，开工前须核查人员资质、电池包状态、通风与消防设施、急停与隔离措施四项缺一不可。仅凭经验口头确认或只查设备接线，会在异常发生时丧失中止条件与监护能力。',
      options: [
        {
          id: 'gz_dp1_a',
          text: '核查人员持证上岗、电池包外观与绝缘、测试区通风消防、急停隔离措施，不合格禁止开工',
          score: 30,
          correct: true,
        },
        {
          id: 'gz_dp1_b',
          text: '仅确认测试设备接线正确、量程设置无误即可开始',
          score: 0,
          correct: false,
        },
        {
          id: 'gz_dp1_c',
          text: '由经验最丰富的工程师口头确认安全后直接启动测试',
          score: 5,
          correct: false,
        },
        {
          id: 'gz_dp1_d',
          text: '参考上次测试记录，若数据正常则简化检查项目以加快测试进度',
          score: 0,
          correct: false,
        },
      ],
    },
    {
      id: 'gz_dp2',
      timelinePhase: 'T+24min · 14:24 异常征兆',
      triggerCondition:
        '测试进行 24 分钟后，BMS 一级告警：1# 电池包温度升至 65℃（15 分钟内温升 37℃），电压离散度增大，电流波动，VOC 气体监测缓慢上升，模组孪生显示由绿转黄。',
      question: '发现上述异常征兆后，应立即采取哪项操作？',
      timePressure: 60,
      regulationRef: 'GB/T 42288-2022 电化学储能电站安全规程',
      explanation:
        '从本次事故时间线看，异常征兆（14:24）到闪爆（14:30）仅约 6 分钟。继续测试、请示等待都会错失切断能量输入的"黄金窗口"。正确处置是立即急停、切断充放电回路、物理隔离电池包并强化通风——这是遏制热失控链的第一道关卡。事故教训：电池包异常必须立即中止作业，不得以"完成本轮测试"为由拖延。',
      options: [
        {
          id: 'gz_dp2_a',
          text: '立即按下急停，切断充放电回路，隔离 1# 电池包并启动强制通风',
          score: 35,
          correct: true,
        },
        {
          id: 'gz_dp2_b',
          text: '继续测试并加强监控，待本轮循环结束后再处理',
          score: 0,
          correct: false,
        },
        {
          id: 'gz_dp2_c',
          text: '电话请示项目负责人，等待 15 分钟后再决定是否停机',
          score: 5,
          correct: false,
        },
        {
          id: 'gz_dp2_d',
          text: '将充放电倍率降至 50% 继续运行，同时加密温度采集频率',
          score: 0,
          correct: false,
        },
      ],
    },
    {
      id: 'gz_dp3',
      timelinePhase: 'T+28min · 14:28 热失控发展',
      triggerCondition:
        '温度在数分钟内由 65℃ 飙升至 180℃，绝缘阻值失效，H₂/VOC 浓度急剧上升，测试区可见烟雾，尚未明火，但闪爆风险已进入临界区。',
      question: '热失控产气阶段，现场人员应如何处置？',
      timePressure: 45,
      regulationRef: 'DL/T 2528-2022 应急预案编制导则',
      explanation:
        '热失控产气阶段最大的风险是可燃气体达到爆炸下限后发生闪爆——6·14 事故正是此路径导致 1 人死亡。此时开门查看会引入氧气，水基灭火可能加剧电解液反应；任何近距离作业均可能触发二次爆炸。必须坚持"生命第一"：全员撤离至上风向安全区，远程切断一切电源，启动报警与警戒，禁止无关人员进入。专业消防到场前，现场人员不得返回核心区。',
      options: [
        {
          id: 'gz_dp3_a',
          text: '全体人员立即撤离至安全区，远程切断电源，启动气体报警与现场警戒',
          score: 35,
          correct: true,
        },
        {
          id: 'gz_dp3_b',
          text: '派人打开电池包舱门近距离查看冒烟部位',
          score: 0,
          correct: false,
        },
        {
          id: 'gz_dp3_c',
          text: '使用水基灭火器直接对准电池包喷射降温',
          score: 5,
          correct: false,
        },
        {
          id: 'gz_dp3_d',
          text: '关闭测试区普通照明与插座电源，保持被测电池包回路以便持续监测',
          score: 0,
          correct: false,
        },
      ],
    },
    {
      id: 'gz_dp4',
      timelinePhase: 'T+30min · 14:30 闪爆后',
      triggerCondition:
        '可燃气体达到爆炸下限，1# 锂电池包发生闪爆，冲击波波及测试区，设备损毁，现场存在人员受伤风险，需启动紧急救援。',
      question: '闪爆发生后，最优先采取的措施是？',
      timePressure: 45,
      regulationRef: '《生产安全事故应急条例》',
      explanation:
        '闪爆后处置优先级依次为：救人、报警、警戒、保护现场。先拍摄视频或读取 BMS 数据会延误救援黄金时间；事故已发生，数据留存应通过后台日志与监控回放实现。6·14 教训强调测试现场必须预先明确应急联络人与疏散路线，事故发生后立即拨打 119/120、组织搜救、封锁现场并向上级报告，为调查复盘保留证据链。',
      options: [
        {
          id: 'gz_dp4_a',
          text: '立即搜救伤员、拨打 119/120、封锁现场并向上级报告',
          score: 30,
          correct: true,
        },
        {
          id: 'gz_dp4_b',
          text: '先拍摄现场视频与照片留存证据，再组织施救',
          score: 0,
          correct: false,
        },
        {
          id: 'gz_dp4_c',
          text: '尝试重启 BMS 读取最后测试数据',
          score: 5,
          correct: false,
        },
        {
          id: 'gz_dp4_d',
          text: '先由现场人员自行包扎轻伤、恢复通风，确认无二次风险后再报警',
          score: 0,
          correct: false,
        },
      ],
    },
  ],
}
