// Mock 数据中心 - 所有模块的模拟数据

export const mockUsers = [
  { id: '1', username: 'admin', email: 'admin@example.com', role: 'admin', company: '储能科技公司', department: '安全管理部', createdAt: '2026-01-01 10:00:00' },
  { id: '2', username: 'zhangsan', email: 'zhangsan@example.com', role: 'trainee', company: '新能源集团', department: '运维部', createdAt: '2026-02-15 14:30:00' },
  { id: '3', username: 'lisi', email: 'lisi@example.com', role: 'trainee', company: '电力公司', department: '安监部', createdAt: '2026-03-20 09:15:00' },
]

export const mockCourses = [
  { id: '1', title: '储能基础知识', description: '了解储能电站的基本原理和组成结构', coverImage: '', category: 'basic', chapters: [
    { id: '1', courseId: '1', title: '储能概述', content: '<h2>什么是储能？</h2><p>储能是指将电能转化为其他形式的能量存储起来，在需要时再转化为电能释放的技术。</p>', duration: 30, order: 1 },
    { id: '2', courseId: '1', title: '电池类型与特点', content: '<h2>电池类型</h2><p>常见储能电池包括锂离子电池、铅酸电池、液流电池等。</p>', duration: 45, order: 2 },
    { id: '3', courseId: '1', title: '储能系统组成', content: '<h2>系统组成</h2><p>储能电站由电池系统、BMS、PCS、EMS等组成。</p>', duration: 45, order: 3 },
  ], totalDuration: 120, progress: 80, createdAt: '2026-01-15', updatedAt: '2026-05-20' },
  { id: '2', title: '锂电池热失控机理', description: '深入学习锂电池热失控的发生机理和传播过程', coverImage: '', category: 'thermal', chapters: [
    { id: '4', courseId: '2', title: '热失控概述', content: '<h2>热失控</h2><p>热失控是电池温度不可控上升的现象。</p>', duration: 30, order: 1 },
    { id: '5', courseId: '2', title: '热失控诱因', content: '<h2>诱因分析</h2><p>内部短路、过充、外部加热等都可能引发热失控。</p>', duration: 30, order: 2 },
    { id: '6', courseId: '2', title: '热扩散过程', content: '<h2>热扩散</h2><p>单个电池热失控后会向周围电池扩散。</p>', duration: 30, order: 3 },
  ], totalDuration: 90, progress: 45, createdAt: '2026-02-01', updatedAt: '2026-05-18' },
  { id: '3', title: '储能消防系统', description: '掌握储能电站消防系统的设计和使用方法', coverImage: '', category: 'fire', chapters: [
    { id: '7', courseId: '3', title: '消防系统概述', content: '<h2>消防系统</h2><p>储能电站消防系统包括气体灭火、水喷雾等。</p>', duration: 30, order: 1 },
    { id: '8', courseId: '3', title: '灭火剂选择', content: '<h2>灭火剂</h2><p>七氟丙烷、全氟己酮等是常用灭火剂。</p>', duration: 30, order: 2 },
  ], totalDuration: 60, progress: 100, createdAt: '2026-02-15', updatedAt: '2026-05-22' },
  { id: '4', title: 'BMS安全管理', description: '学习电池管理系统的安全监控和预警功能', coverImage: '', category: 'bms', chapters: [
    { id: '9', courseId: '4', title: 'BMS功能', content: '<h2>BMS</h2><p>电池管理系统负责监控电池状态。</p>', duration: 25, order: 1 },
    { id: '10', courseId: '4', title: '安全预警', content: '<h2>预警机制</h2><p>BMS通过温度、电压等参数实现预警。</p>', duration: 25, order: 2 },
    { id: '11', courseId: '4', title: '故障诊断', content: '<h2>故障诊断</h2><p>通过数据分析识别电池故障。</p>', duration: 25, order: 3 },
  ], totalDuration: 75, progress: 0, createdAt: '2026-03-01', updatedAt: '2026-05-15' },
  { id: '5', title: '储能电站运维规范', description: '学习储能电站日常运维的标准流程和规范要求', coverImage: '', category: 'basic', chapters: [
    { id: '12', courseId: '5', title: '巡检规范', content: '<h2>巡检</h2><p>定期巡检是保障储能电站安全运行的基础。</p>', duration: 20, order: 1 },
  ], totalDuration: 60, progress: 30, createdAt: '2026-03-10', updatedAt: '2026-05-25' },
  { id: '6', title: '锂电池安全测试标准', description: '了解锂电池安全测试的标准和方法', coverImage: '', category: 'battery', chapters: [], totalDuration: 45, progress: 0, createdAt: '2026-04-01', updatedAt: '2026-05-10' },
]

export const mockScenarios = [
  {
    id: '1', name: '单电池热失控', description: '模拟单个电池单元发生热失控的过程，观察温度变化和热扩散',
    difficulty: 'easy', duration: 60,
    initialConditions: { batteryCount: 1, initialTemperature: 25, batteryType: 'lithium_iron_phosphate', capacity: 0.1 },
    events: [
      { id: 'e1', triggerTime: 10, type: 'temperature_rise', location: { x: 0, y: 0, z: 0 }, parameters: { temperature: 45 }, description: '电池温度开始异常升高' },
      { id: 'e2', triggerTime: 25, type: 'smoke', location: { x: 0, y: 0, z: 0 }, parameters: { temperature: 80, gasConcentration: 200 }, description: '电池开始冒烟' },
      { id: 'e3', triggerTime: 40, type: 'fire', location: { x: 0, y: 0, z: 0 }, parameters: { temperature: 150 }, description: '电池起火' },
    ],
  },
  {
    id: '2', name: '电池组热扩散', description: '模拟电池组中一个电池热失控后向周围电池扩散的过程',
    difficulty: 'medium', duration: 120,
    initialConditions: { batteryCount: 16, initialTemperature: 30, batteryType: 'ternary_lithium', capacity: 1 },
    events: [
      { id: 'e4', triggerTime: 15, type: 'temperature_rise', location: { x: 0, y: 0, z: 0 }, parameters: { temperature: 55 }, description: '中心电池温度异常' },
      { id: 'e5', triggerTime: 40, type: 'smoke', location: { x: 0, y: 0, z: 0 }, parameters: { temperature: 90 }, description: '热失控开始扩散' },
      { id: 'e6', triggerTime: 70, type: 'fire', location: { x: 1, y: 0, z: 0 }, parameters: { temperature: 200, spreadRate: 0.5 }, description: '相邻电池起火' },
      { id: 'e7', triggerTime: 100, type: 'explosion', location: { x: 0, y: 0, z: 0 }, parameters: { temperature: 300 }, description: '发生爆炸' },
    ],
  },
  {
    id: '3', name: '储能舱火灾事故', description: '模拟整个储能舱发生火灾的完整过程，包括预警、扩散和爆炸',
    difficulty: 'hard', duration: 180,
    initialConditions: { batteryCount: 256, initialTemperature: 35, batteryType: 'ternary_lithium', capacity: 10 },
    events: [
      { id: 'e8', triggerTime: 20, type: 'temperature_rise', location: { x: 2, y: 1, z: 0 }, parameters: { temperature: 50 }, description: '局部温度异常' },
      { id: 'e9', triggerTime: 50, type: 'smoke', location: { x: 2, y: 1, z: 0 }, parameters: { temperature: 85, gasConcentration: 500 }, description: '大量烟雾产生' },
      { id: 'e10', triggerTime: 80, type: 'fire', location: { x: 2, y: 1, z: 0 }, parameters: { temperature: 180 }, description: '明火出现' },
      { id: 'e11', triggerTime: 120, type: 'gas_leak', location: { x: 3, y: 2, z: 1 }, parameters: { gasConcentration: 1000 }, description: '有毒气体泄漏' },
      { id: 'e12', triggerTime: 150, type: 'explosion', location: { x: 2, y: 1, z: 0 }, parameters: { temperature: 400 }, description: '储能舱爆炸' },
    ],
  },
]

export const mockTrainingScenarios = [
  {
    id: '1', name: '储能舱温度异常处置', description: '某储能舱温度达到85℃，请在规定时间内做出正确的应急决策',
    difficulty: 'easy', timeLimit: 60,
    decisionPoints: [
      {
        id: 'dp1', scenarioId: '1', triggerCondition: '温度达到85℃',
        question: '系统检测到储能舱温度异常升高至85℃，您应该首先采取什么措施？',
        options: [
          { id: 'o1', text: '立即断电', score: 30, consequence: '断电成功，热源被切断，温度开始下降', isCorrect: true },
          { id: 'o2', text: '继续观察', score: 0, consequence: '温度继续升高，风险增加', isCorrect: false },
          { id: 'o3', text: '启动消防', score: 20, consequence: '消防系统启动，但热源未切断', isCorrect: false },
        ],
        timePressure: 30,
      },
      {
        id: 'dp2', scenarioId: '1', triggerCondition: '断电完成',
        question: '断电后，您下一步应该采取什么措施？',
        options: [
          { id: 'o4', text: '疏散人员', score: 30, consequence: '人员已安全撤离', isCorrect: true },
          { id: 'o5', text: '检查设备', score: 10, consequence: '人员仍在危险区域', isCorrect: false },
          { id: 'o6', text: '通知上级', score: 20, consequence: '已通知，但未疏散人员', isCorrect: false },
        ],
        timePressure: 20,
      },
      {
        id: 'dp3', scenarioId: '1', triggerCondition: '人员已疏散',
        question: '人员疏散后，您应该采取什么措施？',
        options: [
          { id: 'o7', text: '启动消防系统', score: 25, consequence: '消防系统已启动，火势得到控制', isCorrect: true },
          { id: 'o8', text: '等待消防队', score: 15, consequence: '火势可能蔓延', isCorrect: false },
          { id: 'o9', text: '自行灭火', score: 5, consequence: '存在安全风险', isCorrect: false },
        ],
        timePressure: 25,
      },
    ],
  },
  {
    id: '2', name: '电池热失控应急响应', description: '检测到电池热失控前兆，请按照标准流程进行处置',
    difficulty: 'medium', timeLimit: 90,
    decisionPoints: [
      {
        id: 'dp4', scenarioId: '2', triggerCondition: '检测到热失控前兆',
        question: 'BMS检测到电池电压异常波动，温度快速上升，您应该？',
        options: [
          { id: 'o10', text: '立即切断故障电池回路', score: 30, consequence: '故障电池被隔离', isCorrect: true },
          { id: 'o11', text: '降低充电功率', score: 10, consequence: '仍在继续充放电', isCorrect: false },
          { id: 'o12', text: '重启BMS系统', score: 5, consequence: '重启期间失去监控', isCorrect: false },
        ],
        timePressure: 25,
      },
      {
        id: 'dp5', scenarioId: '2', triggerCondition: '电池已隔离',
        question: '故障电池已隔离，但温度仍在上升，您应该？',
        options: [
          { id: 'o13', text: '启动热管理系统强制冷却', score: 25, consequence: '温度开始下降', isCorrect: true },
          { id: 'o14', text: '打开舱门通风', score: 15, consequence: '可能引入更多氧气', isCorrect: false },
          { id: 'o15', text: '等待自然降温', score: 5, consequence: '温度持续上升', isCorrect: false },
        ],
        timePressure: 20,
      },
    ],
  },
  {
    id: '3', name: '储能电站火灾扑救', description: '储能电站发生火灾，请组织人员进行应急处置',
    difficulty: 'hard', timeLimit: 120,
    decisionPoints: [
      {
        id: 'dp6', scenarioId: '3', triggerCondition: '火灾确认',
        question: '储能电站发生火灾，您的第一反应是？',
        options: [
          { id: 'o16', text: '启动应急预案，全员疏散', score: 30, consequence: '人员安全撤离', isCorrect: true },
          { id: 'o17', text: '组织现场人员灭火', score: 10, consequence: '人员面临危险', isCorrect: false },
          { id: 'o18', text: '先确认火源位置', score: 15, consequence: '延误疏散时间', isCorrect: false },
        ],
        timePressure: 20,
      },
    ],
  },
]

export const mockTrainingRecords = [
  { id: '1', scenarioName: '储能舱温度异常处置', score: 85, rating: 'good', completedAt: '2026-06-02 14:30' },
  { id: '2', scenarioName: '电池热失控应急响应', score: 60, rating: 'average', completedAt: '2026-06-01 10:15' },
]

export const mockTrainingReport = {
  id: '1', scenarioName: '储能舱温度异常处置', completedAt: '2026-06-03 10:30:00',
  totalScore: 85, rating: 'good',
  feedback: '整体表现良好，能够正确识别热失控风险并采取断电措施。建议在后续训练中注意响应速度，提高应急处置效率。',
  decisions: [
    { question: '系统检测到储能舱温度异常升高至85℃，您应该首先采取什么措施？', selectedAnswer: '立即断电', correctAnswer: '立即断电', isCorrect: true, responseTime: 8, score: 30 },
    { question: '断电后，您下一步应该采取什么措施？', selectedAnswer: '检查设备', correctAnswer: '疏散人员', isCorrect: false, responseTime: 12, score: 0 },
    { question: '人员疏散后，您应该采取什么措施？', selectedAnswer: '启动消防系统', correctAnswer: '启动消防系统', isCorrect: true, responseTime: 10, score: 25 },
  ],
}

export const mockCases = [
  {
    id: '1', title: '韩国某储能电站火灾事故', location: '韩国忠清南道', date: '2022-06-15',
    type: 'fire', severity: 'major',
    description: '某储能电站发生火灾，造成大量电池损毁，直接经济损失约200万美元。事故发生后，消防部门出动多辆消防车进行扑救，经过数小时才将火势控制。',
    timeline: [
      { id: 't1', time: '2022-06-15 02:30', title: '温度异常', description: 'BMS检测到电池温度异常升高', type: 'detection', importance: 'medium' },
      { id: 't2', time: '2022-06-15 02:35', title: '预警触发', description: '温度达到预警阈值，系统发出警报', type: 'alarm', importance: 'high' },
      { id: 't3', time: '2022-06-15 02:40', title: '热失控发生', description: '电池发生热失控，开始冒烟', type: 'escalation', importance: 'high' },
      { id: 't4', time: '2022-06-15 02:45', title: '火灾发生', description: '明火出现，火势迅速蔓延', type: 'escalation', importance: 'high' },
      { id: 't5', time: '2022-06-15 03:00', title: '消防响应', description: '消防部门到达现场开始扑救', type: 'response', importance: 'medium' },
      { id: 't6', time: '2022-06-15 06:00', title: '火势控制', description: '经过数小时扑救，火势得到控制', type: 'resolution', importance: 'low' },
    ],
    causeAnalysis: '事故原因分析：1. 电池内部短路导致热失控；2. BMS系统未能及时切断故障电池；3. 消防系统响应延迟；4. 电池舱通风设计不合理。',
    lossEstimate: '约200万美元',
    lessonsLearned: '1. 加强BMS监控能力，提高故障检测灵敏度；2. 优化消防系统设计，缩短响应时间；3. 改进电池舱通风设计；4. 建立完善的应急预案。',
    references: ['https://example.com/report1', 'https://example.com/report2'], images: [],
  },
  {
    id: '2', title: '美国亚利桑那储能爆炸事故', location: '美国亚利桑那州', date: '2019-04-19',
    type: 'explosion', severity: 'critical',
    description: '储能设施发生爆炸，造成多名消防人员受伤。事故发生在地下储能设施中，爆炸产生的冲击波导致建筑结构受损。',
    timeline: [
      { id: 't7', time: '2019-04-19 17:00', title: '系统告警', description: '储能系统检测到异常', type: 'detection', importance: 'medium' },
      { id: 't8', time: '2019-04-19 17:05', title: '烟雾检测', description: '烟雾传感器触发', type: 'alarm', importance: 'high' },
      { id: 't9', time: '2019-04-19 17:15', title: '爆炸发生', description: '储能设施发生爆炸', type: 'escalation', importance: 'high' },
      { id: 't10', time: '2019-04-19 17:30', title: '救援开始', description: '消防和救援人员到达', type: 'response', importance: 'medium' },
    ],
    causeAnalysis: '电池内部短路导致热失控，产生的可燃气体在密闭空间内积聚，最终引发爆炸。',
    lossEstimate: '约500万美元',
    lessonsLearned: '1. 改进电池安全设计；2. 加强通风系统；3. 安装可燃气体检测装置；4. 制定专门的密闭空间储能安全标准。',
    references: ['https://example.com/report3'], images: [],
  },
  {
    id: '3', title: '国内某锂电池储能火灾', location: '中国广东', date: '2023-03-10',
    type: 'thermal_runaway', severity: 'moderate',
    description: '某工商业储能项目发生热失控事件，及时发现并处置，未造成人员伤亡。',
    timeline: [
      { id: 't11', time: '2023-03-10 14:20', title: '温度异常', description: 'BMS告警', type: 'detection', importance: 'medium' },
      { id: 't12', time: '2023-03-10 14:25', title: '自动断电', description: '系统自动切断故障回路', type: 'response', importance: 'medium' },
      { id: 't13', time: '2023-03-10 14:30', title: '消防启动', description: '自动灭火系统启动', type: 'response', importance: 'medium' },
      { id: 't14', time: '2023-03-10 15:00', title: '事件处置', description: '热失控得到控制', type: 'resolution', importance: 'low' },
    ],
    causeAnalysis: '电池老化导致内阻增大，充放电过程中产生过多热量，引发热失控。',
    lossEstimate: '约50万元',
    lessonsLearned: '1. 定期检测电池状态；2. 及时更换老化电池；3. 完善BMS预警算法；4. 确保消防系统可靠运行。',
    references: [], images: [],
  },
  {
    id: '4', title: '澳大利亚特斯拉储能站起火', location: '澳大利亚维多利亚', date: '2021-07-30',
    type: 'fire', severity: 'major',
    description: '特斯拉Megapack储能站测试期间发生火灾，火势持续数天。',
    timeline: [
      { id: 't15', time: '2021-07-30 10:00', title: '测试开始', description: '储能站进行上线前测试', type: 'detection', importance: 'low' },
      { id: 't16', time: '2021-07-30 10:15', title: '火灾发生', description: 'Megapack单元起火', type: 'escalation', importance: 'high' },
      { id: 't17', time: '2021-07-30 11:00', title: '消防响应', description: '消防部门到场', type: 'response', importance: 'medium' },
      { id: 't18', time: '2021-08-02', title: '火势扑灭', description: '经过数天火势完全扑灭', type: 'resolution', importance: 'low' },
    ],
    causeAnalysis: '冷却液泄漏导致短路，引发热失控和火灾。',
    lossEstimate: '数百万美元',
    lessonsLearned: '1. 加强冷却系统检测；2. 改进电池模组隔离设计；3. 完善消防预案。',
    references: [], images: [],
  },
]

export const mockDashboardStats = { courseCount: 12, completedCount: 5, simulationCount: 8, avgScore: 85 }
export const mockRecentCourses = [
  { id: '1', title: '储能基础知识', category: '基础', progress: 80 },
  { id: '2', title: '锂电池热失控机理', category: '热失控', progress: 45 },
  { id: '3', title: '储能消防系统', category: '消防', progress: 100 },
]

export const mockAdminStats = { totalUsers: 1256, totalCourses: 24, totalSimulations: 3842, avgScore: 82 }

export const mockAIResponse = (question: string) => {
  const responses: Record<string, string> = {
    '冒烟': '储能柜冒烟处理步骤：1. 立即切断电源；2. 启动消防系统；3. 疏散周围人员；4. 通知消防部门；5. 在安全距离外观察。',
    '热失控': '热失控前兆包括：1. 电池温度异常升高；2. 电压异常波动；3. 电池膨胀变形；4. 产生异味气体；5. BMS频繁告警。',
    '消防': '锂电池储能消防系统类型：1. 气体灭火系统（七氟丙烷、全氟己酮）；2. 水喷雾系统；3. 细水雾系统；4. 干粉灭火系统。',
    'BMS': 'BMS系统主要功能：1. 电池状态监控（电压、电流、温度）；2. SOC/SOH估算；3. 均衡管理；4. 故障诊断与预警；5. 热管理控制。',
    '安全检查': '储能电站安全检查重点：1. 电池外观检查；2. BMS告警记录；3. 消防系统状态；4. 通风系统运行；5. 电气连接检查；6. 接地系统检测。',
  }
  for (const [key, value] of Object.entries(responses)) {
    if (question.includes(key)) return value
  }
  return `关于"${question}"的解答：储能电站安全是新能源行业的重要课题。建议参考《储能电站安全管理规定》和《锂离子电池储能系统技术规范》相关标准。如需更详细的解答，请提供更具体的问题描述。`
}
