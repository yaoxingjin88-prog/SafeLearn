-- 应急决策训练：北京丰台 4·16 / 广州智光 6·14 案例题目
-- 执行：mysql -u root -p safelearn < training_scenarios_cases.sql

INSERT INTO scenarios (id, name, description, initial_conditions, events, decision_points, duration, difficulty, prerequisite_ids, created_at) VALUES
(
  '50000000-0000-0000-0000-000000000001',
  '北京丰台「4·16」应急决策训练',
  '基于2021年北京丰台储能电站火灾爆炸特别重大事故，还原并网运行值班场景下的预警响应、热失控处置、火势蔓延指挥与爆炸后应急决策。',
  '{"batteryCount":4,"initialTemperature":28,"batteryType":"lithium_iron_phosphate","capacity":10,"sceneType":"station_operation","sceneLabel":"电站运营","caseId":"40000000-0000-0000-0000-000000000003","riskLevel":"critical","trainingKind":"emergency_case","accidentDate":"2021-04-16"}',
  '[]',
  '[{"id":"bj_dp1","timelinePhase":"T+5min · 12:05","triggerCondition":"储能系统并网运行中，2#储能舱 BMS 发出一级预警：舱内温度由 28℃ 缓慢升至 42℃，气体探测器 H₂ 读数轻微波动至 15ppm，现场无明显异味，值班员正在例行巡检。","question":"作为当班负责人，收到一级预警后首先应采取哪项处置？","timePressure":90,"regulationRef":"GB/T 42288-2022 电化学储能电站安全规程","explanation":"4·16 事故直接技术原因为内短路引发热失控，管理缺陷包括巡检缺失与预警响应迟缓。一级预警即表明能量异常输入或散热失衡，必须立即远程切断故障舱直流回路并派员外围确认，而非继续观察。延误 10–15 分钟往往意味着热失控进入不可控阶段，是事故链扩大的首要诱因。","options":[{"id":"bj_dp1_a","text":"立即远程切断 2# 舱直流回路，派员着防护装备在外围巡查确认，同步通知站长","score":30,"correct":true},{"id":"bj_dp1_b","text":"继续并网运行，将 BMS 采样频率调高，观察 15 分钟待温度自行回落","score":0,"correct":false},{"id":"bj_dp1_c","text":"仅记录告警日志并发送邮件报备，等待站长到场后再决定","score":5,"correct":false}]},{"id":"bj_dp2","timelinePhase":"T+15min · 12:25","triggerCondition":"现场确认 2# 舱温度骤升至 78℃，单体电压离散度明显增大，舱门接缝处出现白色烟雾，H₂ 浓度 120ppm，疑似电芯内短路触发自发热，热失控已萌芽。","question":"舱内已出现白雾且温度持续攀升，下一步最恰当的处置是？","timePressure":60,"regulationRef":"DL/T 2528-2022 电化学储能电站应急预案编制导则","explanation":"白雾是可燃电解液蒸气与热失控气体的伴随征象。打开舱门通风会引入氧气，加剧热失控反应。4·16 教训要求消防系统与 BMS 联动、应急通风与人员撤离同步执行。此时应启动气体消防与通风联锁，划定禁区，人员撤至上风向，严禁无防护近距离作业。","options":[{"id":"bj_dp2_a","text":"启动应急通风与气体消防联锁，划定安全禁区，组织人员撤离至上风向并持续监测气体","score":30,"correct":true},{"id":"bj_dp2_b","text":"打开舱门加强自然通风，手提干粉灭火器进入舱内查找冒烟点","score":0,"correct":false},{"id":"bj_dp2_c","text":"仅远程调整 BMS 降温参数，不撤离现场人员以便随时处置","score":5,"correct":false}]},{"id":"bj_dp3","timelinePhase":"T+25min · 12:40","triggerCondition":"热失控向相邻电池簇蔓延，2# 舱烟雾浓度显著升高，舱内温度突破 145℃，部分直流回路尚未完全隔离，若不及时阻断能量与蔓延路径，爆燃风险急剧上升。","question":"火势呈蔓延态势，作为现场指挥应如何决策？","timePressure":45,"regulationRef":"GB 50016 建筑设计防火规范（储能部分）","explanation":"事故进入扩散阶段后，必须执行全面断电隔离、边界冷却与分级上报的组合处置。用水枪近距离作业存在触电与有毒气体暴露风险；消极等待则错过遏制窗口，4·16 事故即因处置链条断裂导致爆炸后果扩大。","options":[{"id":"bj_dp3_a","text":"全面断电隔离故障区域，对相邻舱实施边界冷却，上报并启动公司级应急预案","score":35,"correct":true},{"id":"bj_dp3_b","text":"集中水枪冷却 2# 舱外壳，运维人员近距离持续作业","score":0,"correct":false},{"id":"bj_dp3_c","text":"等待消防队到场，现场不作任何隔离与冷却处置","score":5,"correct":false}]},{"id":"bj_dp4","timelinePhase":"T+35min · 13:10","triggerCondition":"可燃气体积聚达到爆炸下限，2# 舱发生爆燃，冲击波损毁相邻设备，明火燃烧，消防力量正在赶赴现场，需防止二次伤亡与复燃。","question":"爆炸发生后，现场指挥的首要任务是什么？","timePressure":45,"regulationRef":"《生产安全事故应急条例》","explanation":"4·16 事故造成重大人员伤亡，明火持续至当日 23:40，冷却监护约 40 小时。爆炸后须清点人员、划定警戒、移交专业消防、制定长期冷却与复燃监测方案，严禁为减少损失让未防护人员进入核心区。","options":[{"id":"bj_dp4_a","text":"清点人员、划定警戒区，移交专业消防处置，启动长时间冷却与复燃监测方案","score":35,"correct":true},{"id":"bj_dp4_b","text":"组织非专业人员进入核心区搬运设备，减少财产损失","score":0,"correct":false},{"id":"bj_dp4_c","text":"仅对受损舱断电，其余舱继续并网运行以保障供电","score":5,"correct":false}]}]',
  300,
  3,
  NULL,
  NOW()
),
(
  '50000000-0000-0000-0000-000000000002',
  '广州智光「6·14」应急决策训练',
  '基于2024年广州黄埔区锂电池包闪爆特别重大事故，还原储能测试作业场景下从开工确认、异常征兆、热失控发展到闪爆后响应的全流程决策。',
  '{"batteryCount":1,"initialTemperature":32,"batteryType":"lithium_battery","capacity":0.5,"sceneType":"test_operation","sceneLabel":"测试作业","caseId":"40000000-0000-0000-0000-000000000005","riskLevel":"critical","trainingKind":"emergency_case","accidentDate":"2024-06-14"}',
  '[]',
  '[{"id":"gz_dp1","timelinePhase":"T0 · 14:00 测试前","triggerCondition":"室内测试区气温 32℃，即将对 1# 磷酸铁锂电池包开展充放电循环与容量标定测试。现场有测试工程师 2 人、监护 1 人，BMS 与气体监测在线。","question":"测试开始前，必须完成哪项安全确认？","timePressure":90,"regulationRef":"《储能系统测试安全规范》","explanation":"6·14 事故调查报告指出：测试环节管理失控、操作人员无证上岗、应急预案未演练是重要间接原因。开工前须核查人员资质、电池包状态、通风与消防设施、急停与隔离措施四项缺一不可。","options":[{"id":"gz_dp1_a","text":"核查人员持证上岗、电池包外观与绝缘、测试区通风消防、急停隔离措施，不合格禁止开工","score":30,"correct":true},{"id":"gz_dp1_b","text":"仅确认测试设备接线正确、量程设置无误即可开始","score":0,"correct":false},{"id":"gz_dp1_c","text":"由经验最丰富的工程师口头确认安全后直接启动测试","score":5,"correct":false}]},{"id":"gz_dp2","timelinePhase":"T+24min · 14:24 异常征兆","triggerCondition":"测试进行 24 分钟后，BMS 一级告警：1# 电池包温度升至 65℃（15 分钟内温升 37℃），电压离散度增大，电流波动，VOC 气体监测缓慢上升。","question":"发现上述异常征兆后，应立即采取哪项操作？","timePressure":60,"regulationRef":"GB/T 42288-2022 电化学储能电站安全规程","explanation":"从本次事故时间线看，异常征兆到闪爆仅约 6 分钟。继续测试或请示等待都会错失切断能量输入的黄金窗口。正确处置是立即急停、切断回路、物理隔离并强化通风。","options":[{"id":"gz_dp2_a","text":"立即按下急停，切断充放电回路，隔离 1# 电池包并启动强制通风","score":35,"correct":true},{"id":"gz_dp2_b","text":"继续测试并加强监控，待本轮循环结束后再处理","score":0,"correct":false},{"id":"gz_dp2_c","text":"电话请示项目负责人，等待 15 分钟后再决定是否停机","score":5,"correct":false}]},{"id":"gz_dp3","timelinePhase":"T+28min · 14:28 热失控发展","triggerCondition":"温度在数分钟内由 65℃ 飙升至 180℃，绝缘阻值失效，H₂/VOC 浓度急剧上升，测试区可见烟雾，闪爆风险已进入临界区。","question":"热失控产气阶段，现场人员应如何处置？","timePressure":45,"regulationRef":"DL/T 2528-2022 应急预案编制导则","explanation":"热失控产气阶段最大风险是可燃气体达到爆炸下限后闪爆——6·14 事故正是此路径导致人员伤亡。开门查看或水基灭火均可能触发二次爆炸，必须坚持生命第一、全员撤离并远程断电警戒。","options":[{"id":"gz_dp3_a","text":"全体人员立即撤离至安全区，远程切断电源，启动气体报警与现场警戒","score":35,"correct":true},{"id":"gz_dp3_b","text":"派人打开电池包舱门近距离查看冒烟部位","score":0,"correct":false},{"id":"gz_dp3_c","text":"使用水基灭火器直接对准电池包喷射降温","score":5,"correct":false}]},{"id":"gz_dp4","timelinePhase":"T+30min · 14:30 闪爆后","triggerCondition":"可燃气体达到爆炸下限，1# 锂电池包发生闪爆，冲击波波及测试区，设备损毁，现场存在人员受伤风险。","question":"闪爆发生后，最优先采取的措施是？","timePressure":45,"regulationRef":"《生产安全事故应急条例》","explanation":"闪爆后处置优先级为：救人、报警、警戒、保护现场。先拍摄视频或读取数据会延误救援黄金时间。须立即拨打 119/120、组织搜救、封锁现场并向上级报告。","options":[{"id":"gz_dp4_a","text":"立即搜救伤员、拨打 119/120、封锁现场并向上级报告","score":30,"correct":true},{"id":"gz_dp4_b","text":"先拍摄现场视频与照片留存证据，再组织施救","score":0,"correct":false},{"id":"gz_dp4_c","text":"尝试重启 BMS 读取最后测试数据","score":5,"correct":false}]}]',
  240,
  3,
  NULL,
  NOW()
)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  initial_conditions = VALUES(initial_conditions),
  decision_points = VALUES(decision_points),
  duration = VALUES(duration),
  difficulty = VALUES(difficulty);
