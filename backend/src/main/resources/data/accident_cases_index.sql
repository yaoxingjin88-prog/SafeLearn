-- 储能电站事故案例多维标签索引表（2021-2025 公开报道案例，共 12 条）
-- 数据来源：应急管理部、地方应急管理局及行业媒体报道

INSERT IGNORE INTO accident_cases (id, title, location, date, type, severity, description, timeline, cause_analysis, loss_estimate, lessons_learned, created_at, updated_at) VALUES
('40000000-0000-0000-0000-000000000003', '北京丰台''4·16''储能电站火灾爆炸事故', '北京丰台区西马场甲14号', '2021-04-16', 'explosion', 'critical', '【电池类型】磷酸铁锂【电站类型】光储充一体。站内电池因内短路发生热失控，先后引发火灾与爆炸，造成重大人员伤亡；明火持续至23:40，冷却处置约40小时。', '[{"id":"c3t1","time":"12:00","title":"储能区异常","description":"储能区监测到电池温度、电压异常波动","type":"warning"},{"id":"c3t2","time":"12:25","title":"热失控发生","description":"磷酸铁锂电池内短路触发热失控，局部冒烟升温","type":"danger"},{"id":"c3t3","time":"12:40","title":"火灾扩大","description":"火势蔓延，消防力量赶赴现场","type":"danger"},{"id":"c3t4","time":"13:10","title":"爆炸发生","description":"储能设施发生爆炸，冲击波扩大事故后果","type":"danger"},{"id":"c3t5","time":"13:30","title":"全力救援","description":"多支消防力量投入救援与冷却处置","type":"action"},{"id":"c3t6","time":"23:40","title":"明火扑灭","description":"明火被扑灭，后续进入长时间冷却监护阶段","type":"info"}]', '直接技术原因为内短路引发热失控；管理方面存在巡检缺失、应急预案未演练、消防系统未联动等问题，导致事故链扩大。', '财产损失1660.81万元；爆炸当量约26kg TNT；2名消防员牺牲、1人遇难、1人受伤。', '1. 严格落实储能电站日常巡检与异常处置；2. 定期组织应急预案演练；3. 确保消防系统与储能BMS联动有效；4. 加强磷酸铁锂储能热失控早期预警。', NOW(), NOW()),
('40000000-0000-0000-0000-000000000004', '海南某市光伏+17.5MW/35MWh储能电站火灾事故', '海南某市', '2024-05-26', 'thermal_runaway', 'moderate', '【电池类型】磷酸铁锂【电站类型】集中式储能。1组电池预制舱因内短路发生热失控起火，未蔓延扩大，未发生次生灾害。', '[{"id":"c4t1","time":"—","title":"运行异常","description":"BMS监测到预制舱内电池温度异常升高","type":"warning"},{"id":"c4t2","time":"—","title":"热失控触发","description":"电池内短路引发热失控，舱内出现冒烟","type":"danger"},{"id":"c4t3","time":"—","title":"预制舱起火","description":"1组电池预制舱起火燃烧","type":"danger"},{"id":"c4t4","time":"—","title":"现场处置","description":"运维与消防力量到场隔离处置","type":"action"},{"id":"c4t5","time":"—","title":"火势控制","description":"火势被控制在单舱范围，未蔓延至全站","type":"info"}]', '内短路为直接技术原因；巡检缺失与应急预案未演练削弱了早期发现与快速响应能力。', '1组电池预制舱烧毁，无人员伤亡，未蔓延扩大。', '1. 加强预制舱级温度监测与告警；2. 落实定期巡检制度；3. 完善并演练预制舱火灾应急预案；4. 设置舱间防火隔离措施。', NOW(), NOW()),
('40000000-0000-0000-0000-000000000005', '广州智光储能科技''6·14''锂电池包闪爆事故', '广州黄埔区', '2024-06-14', 'explosion', 'critical', '【电池类型】锂电池【电站类型】分布式储能（测试）。测试过程中锂电池包发生闪爆，造成人员伤亡；黄埔区应急管理局已发布事故调查报告。', '[{"id":"c5t1","time":"—","title":"测试作业","description":"储能测试现场进行锂电池包相关作业","type":"warning"},{"id":"c5t2","time":"—","title":"异常征兆","description":"电池包出现异常温升或电压波动","type":"warning"},{"id":"c5t3","time":"—","title":"闪爆发生","description":"锂电池包发生闪爆","type":"danger"},{"id":"c5t4","time":"—","title":"应急响应","description":"现场人员报警并启动应急处置","type":"action"},{"id":"c5t5","time":"—","title":"事故调查","description":"应急管理部门开展事故调查并发布报告","type":"info"}]', '内短路为直接技术原因；测试环节应急预案未演练、操作人员无证上岗等管理缺陷加剧了风险。', '1人死亡。', '1. 储能测试作业必须持证上岗；2. 测试前完成风险辨识与应急演练；3. 严格测试过程监护与隔离措施；4. 电池包异常立即中止作业。', NOW(), NOW()),
('40000000-0000-0000-0000-000000000006', '珠海广通物流园储能柜火灾事故', '广东珠海香洲区', '2023-08-01', 'fire', 'moderate', '【电池类型】磷酸铁锂【电站类型】用户侧储能。储能柜内电池热失控起火，消防调派9车39人处置，无人员伤亡。', '[{"id":"c6t1","time":"—","title":"异常发现","description":"储能柜运行参数异常或现场发现冒烟","type":"warning"},{"id":"c6t2","time":"—","title":"电池起火","description":"储能柜内电池起火","type":"danger"},{"id":"c6t3","time":"—","title":"消防出动","description":"珠海消防调派9辆消防车39名指战员到场","type":"action"},{"id":"c6t4","time":"—","title":"处置完成","description":"火势得到控制，无人员伤亡","type":"info"}]', '内短路引发起火；消防系统未联动、巡检缺失影响早期处置效率。', '储能柜烧毁，无人员伤亡。', '1. 用户侧储能柜应接入消防联动；2. 落实物流园区储能巡检；3. 明确园区与消防的应急联络机制；4. 储能柜周边保持安全间距。', NOW(), NOW()),
('40000000-0000-0000-0000-000000000007', '温州某工商业储能项目火灾事故', '浙江温州', '2024-06-01', 'thermal_runaway', 'moderate', '【电池类型】磷酸铁锂【电站类型】用户侧储能（工商业）。现场因热失控引发火灾，设备烧毁，具体伤亡损失数据待进一步核实。', '[{"id":"c7t1","time":"—","title":"热失控前兆","description":"电池温度或电压出现异常","type":"warning"},{"id":"c7t2","time":"—","title":"热失控发生","description":"电池热失控，局部冒烟升温","type":"danger"},{"id":"c7t3","time":"—","title":"火灾发生","description":"工商业储能设备起火燃烧","type":"danger"},{"id":"c7t4","time":"—","title":"现场处置","description":"企业自救与消防力量到场处置","type":"action"},{"id":"c7t5","time":"—","title":"后续调查","description":"事故损失与原因进一步核实中","type":"info"}]', '热失控为直接技术原因；巡检缺失、应急预案未演练、消防系统未联动等问题并存。', '现场烧毁，具体伤亡损失数据待查。', '1. 工商业储能应纳入企业安全生产管理；2. 建立日常巡检与异常记录制度；3. 定期演练火灾应急预案；4. 确保消防系统有效联动。', NOW(), NOW()),
('40000000-0000-0000-0000-000000000008', '韩国国家级数据中心储能锂电池火灾事故', '韩国', '2024-09-01', 'thermal_runaway', 'critical', '【电池类型】三元锂（ESS用）【电站类型】分布式储能（数据中心）。储能系统火灾导致全国性政府网络中断，构成重大基础设施事故。', '[{"id":"c8t1","time":"—","title":"ESS异常","description":"数据中心储能系统监测参数异常","type":"warning"},{"id":"c8t2","time":"—","title":"热失控起火","description":"三元锂电池热失控并引发火灾","type":"danger"},{"id":"c8t3","time":"—","title":"基础设施受影响","description":"火灾导致关键供电中断，政府网络服务大面积中断","type":"danger"},{"id":"c8t4","time":"—","title":"应急恢复","description":"启动应急供电与网络恢复措施","type":"action"},{"id":"c8t5","time":"—","title":"事故评估","description":"评估基础设施韧性与储能安全冗余","type":"info"}]', '热失控为直接技术原因；巡检缺失与消防系统未联动导致关键设施防护不足。', '造成全国性政府网络中断，属重大基础设施事故。', '1. 关键基础设施储能须独立消防与供电冗余；2. 加强数据中心ESS专项巡检；3. 建立跨部门应急联动机制；4. 评估三元锂在关键场景的适用性。', NOW(), NOW()),
('40000000-0000-0000-0000-000000000009', '美国加州Moss Landing储能电站系列火灾（第4次）', '美国加州蒙特雷县', '2025-01-15', 'thermal_runaway', 'critical', '【电池类型】锂电池【电站类型】集中式储能。该电站累计发生至少4次火灾，第4次火灾持续超8小时，70%以上设施受损，曾导致1200余居民疏散。', '[{"id":"c9t1","time":"—","title":"多次事故历史","description":"2021年9月、2022年2月、2025年1月等多次发生火灾","type":"warning"},{"id":"c9t2","time":"—","title":"第4次火灾","description":"电池热失控引发火灾，BMS保护可能失效","type":"danger"},{"id":"c9t3","time":"—","title":"持续燃烧","description":"火灾持续超8小时，多次复燃","type":"danger"},{"id":"c9t4","time":"—","title":"人员疏散","description":"周边1200余名居民被疏散","type":"action"},{"id":"c9t5","time":"—","title":"设施损毁","description":"70%以上设施受损，引发社区诉讼","type":"info"}]', '热失控及BMS失效（推断）为技术原因；应急预案未演练、巡检缺失导致事故反复发生。', '70%以上设施受损；1200+居民疏散；多次复燃；社区诉讼。', '1. 对反复发生事故的电站开展全面安全评估；2. 强化BMS与消防系统可靠性验证；3. 建立复燃监测与长期冷却方案；4. 加强社区沟通与应急疏散预案。', NOW(), NOW()),
('40000000-0000-0000-0000-00000000000a', '美国加州Gateway（Otay Mesa）250MW储能电站火灾', '美国加州圣地亚哥Otay Mesa', '2024-05-15', 'fire', 'major', '【电池类型】三元锂（LG Chem）【电站类型】集中式储能。250MW/250MWh规模电站发生火灾，持续11-16天并多次复燃，产生有毒烟雾和氢气，无人员伤亡。', '[{"id":"cat1","time":"05-15","title":"火灾发生","description":"大型储能电站发生火灾","type":"danger"},{"id":"cat2","time":"—","title":"持续燃烧","description":"火灾持续11-16天，多次复燃","type":"danger"},{"id":"cat3","time":"—","title":"有害气体","description":"燃烧产生有毒烟雾和氢气","type":"danger"},{"id":"cat4","time":"—","title":"长期处置","description":"消防与专业机构开展长时间监护处置","type":"action"},{"id":"cat5","time":"—","title":"火势受控","description":"无人员伤亡，事故进入调查阶段","type":"info"}]', '热失控及BMS失效（推断）为技术原因；消防系统未联动、应急预案未演练影响处置效率。', '250MW/250MWh设施严重受损；火灾持续11-16天；无人员伤亡。', '1. 大型储能项目须验证消防系统与BMS联动；2. 制定长期复燃监测方案；3. 加强三元锂电池热失控气体防护；4. 定期演练大型电站火灾应急预案。', NOW(), NOW()),
('40000000-0000-0000-0000-00000000000b', '德国尼尔莫尔商业区锂电池储能集装箱火灾', '德国尼尔莫尔（里尔区）', '2024-04-27', 'fire', 'major', '【电池类型】锂电池【电站类型】分布式储能（工商业集装箱）。集装箱储能发生火灾，2名消防员轻伤，财产损失约50万欧元，扑救约10小时。', '[{"id":"cbt1","time":"—","title":"集装箱异常","description":"工商业储能集装箱出现异常温升","type":"warning"},{"id":"cbt2","time":"—","title":"火灾发生","description":"锂电池储能集装箱起火","type":"danger"},{"id":"cbt3","time":"—","title":"消防扑救","description":"消防力量展开约10小时扑救","type":"action"},{"id":"cbt4","time":"—","title":"人员受伤","description":"2名消防员在处置中轻伤","type":"danger"},{"id":"cbt5","time":"—","title":"事故结束","description":"火势受控，评估财产损失约50万欧元","type":"info"}]', '热失控为直接技术原因；应急预案未演练、消防系统未联动影响处置安全。', '2名消防员轻伤；财产损失约50万欧元。', '1. 集装箱储能应规范布置与防火间距；2. 消防处置前评估爆炸与有毒气体风险；3. 定期演练集装箱火灾应急预案；4. 加强消防员储能火灾专项培训。', NOW(), NOW()),
('40000000-0000-0000-0000-00000000000c', '韩国ESS储能系统系列火灾事故（2017-2022）', '韩国全罗北道等多地', '2019-06-01', 'fire', 'critical', '【电池类型】三元锂（LG化学）+ 磷酸铁锂【电站类型】分布式储能（ESS）。2017-2019年共27起火灾，LG电池占17起；2011-2022年累计32起以上。', '[{"id":"cct1","time":"2017","title":"系列事故开端","description":"韩国多地ESS陆续发生火灾","type":"warning"},{"id":"cct2","time":"2018","title":"事故频发","description":"LG化学等品牌电池涉事事故增多","type":"danger"},{"id":"cct3","time":"2019","title":"27起火灾统计","description":"2017-2019年累计27起ESS火灾","type":"danger"},{"id":"cct4","time":"—","title":"行业整顿","description":"韩国政府与行业开展安全整顿与召回","type":"action"},{"id":"cct5","time":"2022","title":"持续影响","description":"2011-2022年累计事故32起以上","type":"info"}]', '内短路、外部短路、过充等技术原因并存；巡检缺失与BMS失效为重要管理因素。', '2017-2019年27起火灾；LG电池占17起；累计32起以上。', '1. 建立ESS全生命周期安全监测体系；2. 强化BMS软件与硬件可靠性；3. 对问题批次电池及时召回更换；4. 国家层面建立储能事故统计与预警机制。', NOW(), NOW()),
('40000000-0000-0000-0000-00000000000d', '美国杰克逊维尔电池工厂火灾', '美国杰克逊维尔', '2023-06-01', 'fire', 'moderate', '【电池类型】锂电池【电站类型】集中式储能（电池工厂）。电池工厂起火，具体损失数据待进一步核实。', '[{"id":"cdt1","time":"—","title":"工厂异常","description":"电池生产或仓储区域出现异常","type":"warning"},{"id":"cdt2","time":"—","title":"火灾发生","description":"电池工厂起火","type":"danger"},{"id":"cdt3","time":"—","title":"消防处置","description":"消防力量赶赴现场扑救","type":"action"},{"id":"cdt4","time":"—","title":"损失评估","description":"具体损失数据待查","type":"info"}]', '热失控为直接技术原因；巡检缺失与应急预案未演练影响工厂火灾防控。', '电池工厂起火，具体损失数据待查。', '1. 电池工厂须严格分区防火管理；2. 加强生产环节热失控监测；3. 完善工厂火灾应急预案并演练；4. 危险品仓储符合消防规范。', NOW(), NOW()),
('40000000-0000-0000-0000-00000000000e', '2025年全国储能电站火灾事故（统计案例）', '全国多地', '2025-03-01', 'thermal_runaway', 'moderate', '【电池类型】磷酸铁锂（主流）【电站类型】集中式/分布式/用户侧。2025年1-5月全国已发生储能电站火灾事故17起，热失控引发占比82%，事故具有级联反应特性。', '[{"id":"cet1","time":"2025-01","title":"年初事故抬头","description":"全国多地陆续报告储能火灾","type":"warning"},{"id":"cet2","time":"—","title":"热失控为主因","description":"82%事故由热失控引发","type":"danger"},{"id":"cet3","time":"—","title":"级联反应特征","description":"多起事故呈现热扩散与级联反应特征","type":"danger"},{"id":"cet4","time":"2025-05","title":"阶段性统计","description":"1-5月累计17起储能电站火灾事故","type":"info"}]', '热失控与BMS失效为主要技术原因；巡检缺失、应急预案未演练、消防系统未联动为共性管理缺陷。', '2025年1-5月全国储能火灾17起；热失控占比82%。', '1. 从行业统计看须强化热失控早期预警；2. 推动消防与BMS联动标准落地；3. 建立全国储能事故案例共享机制；4. 针对级联反应加强舱间隔离设计。', NOW(), NOW());

-- 结构化字段回填（幂等 UPDATE）
UPDATE accident_cases SET
    direct_cause = '内短路引发磷酸铁锂电池热失控',
    indirect_cause = '巡检缺失；应急预案未演练；消防系统未联动',
    root_cause = '电池内短路叠加运维管理与消防联动缺陷，导致事故链扩大为火灾爆炸',
    casualties = '2名消防员牺牲、1人遇难、1人受伤',
    loss_amount = 1661,
    loss_breakdown = '[{"label":"财产损失","amount":1661}]',
    lessons = '["严格落实储能电站日常巡检与异常处置","定期组织应急预案演练","确保消防系统与储能BMS联动有效","加强磷酸铁锂储能热失控早期预警"]',
    difficulty = 'advanced',
    study_minutes = 35
WHERE id = '40000000-0000-0000-0000-000000000003';

UPDATE accident_cases SET
    direct_cause = '电池内短路引发热失控',
    indirect_cause = '巡检缺失；应急预案未演练',
    root_cause = '内短路触发单舱热失控，早期巡检与应急准备不足延误处置',
    casualties = '无人员伤亡',
    loss_amount = NULL,
    loss_breakdown = '[{"label":"预制舱损毁","amount":0}]',
    lessons = '["加强预制舱级温度监测与告警","落实定期巡检制度","完善并演练预制舱火灾应急预案","设置舱间防火隔离措施"]',
    difficulty = 'intermediate',
    study_minutes = 20
WHERE id = '40000000-0000-0000-0000-000000000004';

UPDATE accident_cases SET
    direct_cause = '内短路引发锂电池包闪爆',
    indirect_cause = '应急预案未演练；操作人员无证上岗',
    root_cause = '测试环节管理失控与人员资质缺失导致致命事故',
    casualties = '1人死亡',
    loss_amount = NULL,
    loss_breakdown = NULL,
    lessons = '["储能测试作业必须持证上岗","测试前完成风险辨识与应急演练","严格测试过程监护与隔离措施","电池包异常立即中止作业"]',
    difficulty = 'advanced',
    study_minutes = 25
WHERE id = '40000000-0000-0000-0000-000000000005';

UPDATE accident_cases SET
    direct_cause = '内短路引发储能柜起火',
    indirect_cause = '消防系统未联动；巡检缺失',
    root_cause = '电池故障未能被早期发现，消防联动不足延缓处置',
    casualties = '无人员伤亡',
    loss_amount = NULL,
    loss_breakdown = NULL,
    lessons = '["用户侧储能柜应接入消防联动","落实物流园区储能巡检","明确园区与消防的应急联络机制","储能柜周边保持安全间距"]',
    difficulty = 'intermediate',
    study_minutes = 18
WHERE id = '40000000-0000-0000-0000-000000000006';

UPDATE accident_cases SET
    direct_cause = '电池热失控',
    indirect_cause = '巡检缺失；应急预案未演练；消防系统未联动',
    root_cause = '热失控未被早期识别，多重管理缺陷并存',
    casualties = '待查',
    loss_amount = NULL,
    loss_breakdown = NULL,
    lessons = '["工商业储能应纳入企业安全生产管理","建立日常巡检与异常记录制度","定期演练火灾应急预案","确保消防系统有效联动"]',
    difficulty = 'intermediate',
    study_minutes = 20
WHERE id = '40000000-0000-0000-0000-000000000007';

UPDATE accident_cases SET
    direct_cause = '三元锂电池热失控',
    indirect_cause = '巡检缺失；消防系统未联动',
    root_cause = '关键基础设施储能安全防护不足，火灾演变为系统性风险',
    casualties = '无人员伤亡（网络服务中断）',
    loss_amount = NULL,
    loss_breakdown = NULL,
    lessons = '["关键基础设施储能须独立消防与供电冗余","加强数据中心ESS专项巡检","建立跨部门应急联动机制","评估三元锂在关键场景的适用性"]',
    difficulty = 'advanced',
    study_minutes = 30
WHERE id = '40000000-0000-0000-0000-000000000008';

UPDATE accident_cases SET
    direct_cause = '热失控；BMS失效（推断）',
    indirect_cause = '应急预案未演练；巡检缺失',
    root_cause = '大型电站反复发生事故，系统性安全治理不足',
    casualties = '无人员伤亡；1200+居民疏散',
    loss_amount = NULL,
    loss_breakdown = NULL,
    lessons = '["对反复发生事故的电站开展全面安全评估","强化BMS与消防系统可靠性验证","建立复燃监测与长期冷却方案","加强社区沟通与应急疏散预案"]',
    difficulty = 'advanced',
    study_minutes = 35
WHERE id = '40000000-0000-0000-0000-000000000009';

UPDATE accident_cases SET
    direct_cause = '热失控；BMS失效（推断）',
    indirect_cause = '消防系统未联动；应急预案未演练',
    root_cause = '超大规模三元锂储能火灾长期复燃，暴露设计与应急短板',
    casualties = '无人员伤亡',
    loss_amount = NULL,
    loss_breakdown = NULL,
    lessons = '["大型储能项目须验证消防系统与BMS联动","制定长期复燃监测方案","加强三元锂电池热失控气体防护","定期演练大型电站火灾应急预案"]',
    difficulty = 'advanced',
    study_minutes = 35
WHERE id = '40000000-0000-0000-0000-00000000000a';

UPDATE accident_cases SET
    direct_cause = '热失控引发集装箱储能火灾',
    indirect_cause = '应急预案未演练；消防系统未联动',
    root_cause = '集装箱布置与消防处置预案不足导致扑救周期长',
    casualties = '2名消防员轻伤',
    loss_amount = 40,
    loss_breakdown = '[{"label":"财产损失（约50万欧元）","amount":40}]',
    lessons = '["集装箱储能应规范布置与防火间距","消防处置前评估爆炸与有毒气体风险","定期演练集装箱火灾应急预案","加强消防员储能火灾专项培训"]',
    difficulty = 'intermediate',
    study_minutes = 22
WHERE id = '40000000-0000-0000-0000-00000000000b';

UPDATE accident_cases SET
    direct_cause = '内短路；外部短路；过充',
    indirect_cause = '巡检缺失；BMS失效',
    root_cause = '韩国ESS行业快速扩张期安全标准与监管未能同步',
    casualties = '系列事故累计（无单一伤亡汇总）',
    loss_amount = NULL,
    loss_breakdown = NULL,
    lessons = '["建立ESS全生命周期安全监测体系","强化BMS软件与硬件可靠性","对问题批次电池及时召回更换","国家层面建立储能事故统计与预警机制"]',
    difficulty = 'advanced',
    study_minutes = 40
WHERE id = '40000000-0000-0000-0000-00000000000c';

UPDATE accident_cases SET
    direct_cause = '热失控引发工厂火灾',
    indirect_cause = '巡检缺失；应急预案未演练',
    root_cause = '电池工厂生产与仓储环节火灾防控不足',
    casualties = '待查',
    loss_amount = NULL,
    loss_breakdown = NULL,
    lessons = '["电池工厂须严格分区防火管理","加强生产环节热失控监测","完善工厂火灾应急预案并演练","危险品仓储符合消防规范"]',
    difficulty = 'intermediate',
    study_minutes = 18
WHERE id = '40000000-0000-0000-0000-00000000000d';

UPDATE accident_cases SET
    direct_cause = '热失控；BMS失效',
    indirect_cause = '巡检缺失；应急预案未演练；消防系统未联动',
    root_cause = '行业共性管理缺陷导致热失控事故高发并呈级联特征',
    casualties = '统计案例（17起火灾）',
    loss_amount = NULL,
    loss_breakdown = NULL,
    lessons = '["从行业统计看须强化热失控早期预警","推动消防与BMS联动标准落地","建立全国储能事故案例共享机制","针对级联反应加强舱间隔离设计"]',
    difficulty = 'intermediate',
    study_minutes = 25
WHERE id = '40000000-0000-0000-0000-00000000000e';
