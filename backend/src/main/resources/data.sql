-- 默认管理员 (密码: admin123)
INSERT IGNORE INTO users (id, username, email, password_hash, role, company, department, created_at, updated_at)
VALUES ('550e8400-e29b-41d4-a716-446655440000', 'admin', 'admin@example.com', '$2a$10$H2wPB2Pxr9eac3CKbwcqJejquPzbbAjm6VCj8kC0nlV21X2gbk7hK', 'admin', '储能科技公司', '安全管理部', NOW(), NOW());

-- 测试用户 (密码均为 admin123)
INSERT IGNORE INTO users (id, username, email, password_hash, role, company, department, created_at, updated_at) VALUES
('660e8400-e29b-41d4-a716-446655440001', 'zhanggong', 'zhanggong@safelearn.com', '$2a$10$H2wPB2Pxr9eac3CKbwcqJejquPzbbAjm6VCj8kC0nlV21X2gbk7hK', 'trainee', '华能储能电站', '运维部', DATE_SUB(NOW(), INTERVAL 44 DAY), NOW()),
('660e8400-e29b-41d4-a716-446655440002', 'lisi', 'lisi@safelearn.com', '$2a$10$H2wPB2Pxr9eac3CKbwcqJejquPzbbAjm6VCj8kC0nlV21X2gbk7hK', 'trainee', '国电新能源', '安全部', DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),
('660e8400-e29b-41d4-a716-446655440003', 'wangwu', 'wangwu@safelearn.com', '$2a$10$H2wPB2Pxr9eac3CKbwcqJejquPzbbAjm6VCj8kC0nlV21X2gbk7hK', 'trainee', '南方电网储能', '培训部', DATE_SUB(NOW(), INTERVAL 60 DAY), NOW());

-- 课程数据
INSERT IGNORE INTO courses (id, title, description, category, total_duration, status, created_at, updated_at) VALUES
('10000000-0000-0000-0000-000000000001', '储能基础知识', '了解储能电站的基本原理和组成结构', 'basic', 120, 'published', NOW(), NOW()),
('10000000-0000-0000-0000-000000000002', '锂电池热失控机理', '深入学习锂电池热失控的发生机理和传播过程', 'thermal', 90, 'published', NOW(), NOW()),
('10000000-0000-0000-0000-000000000003', '储能消防系统', '掌握储能电站消防系统的设计和使用方法', 'fire', 60, 'published', NOW(), NOW()),
('10000000-0000-0000-0000-000000000004', 'BMS安全管理', '学习电池管理系统的安全监控和预警功能', 'bms', 75, 'published', NOW(), NOW()),
('10000000-0000-0000-0000-000000000005', '储能电站运维规范', '学习储能电站日常运维的标准流程和规范要求', 'basic', 60, 'published', NOW(), NOW());

-- 章节数据 (difficulty_level: 1=基础理论, 2=案例分析, 3=高级实操)
-- prerequisite_ids: JSON数组，支持多条件解锁
-- scenario_id: 关联的训练场景，学完理论后可无缝进入演练
INSERT IGNORE INTO chapters (id, course_id, title, content, duration, order_num, difficulty_level, prerequisite_ids, scenario_id, created_at) VALUES
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '储能概述', '<h2>什么是储能？</h2><p>储能是指将电能转化为其他形式的能量存储起来，在需要时再转化为电能释放的技术。</p><p>储能技术是解决新能源发电间歇性和波动性的关键手段。</p>', 30, 1, 1, NULL, NULL, NOW()),
('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '电池类型与特点', '<h2>电池类型</h2><p>常见储能电池包括锂离子电池、铅酸电池、液流电池等。</p><p>锂离子电池因其高能量密度和长寿命成为主流选择。</p>', 45, 2, 1, '["20000000-0000-0000-0000-000000000001"]', NULL, NOW()),
('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', '储能系统组成', '<h2>系统组成</h2><p>储能电站由电池系统、BMS、PCS、EMS等组成。</p>', 45, 3, 1, '["20000000-0000-0000-0000-000000000002"]', NULL, NOW()),
('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000002', '热失控概述', '<h2>热失控</h2><p>热失控是电池温度不可控上升的现象，可能导致起火或爆炸。</p>', 30, 1, 2, NULL, '30000000-0000-0000-0000-000000000001', NOW()),
('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000002', '热失控诱因', '<h2>诱因分析</h2><p>内部短路、过充、外部加热等都可能引发热失控。</p>', 30, 2, 2, '["20000000-0000-0000-0000-000000000004"]', '30000000-0000-0000-0000-000000000002', NOW()),
('20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000003', '消防系统概述', '<h2>消防系统</h2><p>储能电站消防系统包括气体灭火、水喷雾等。</p>', 30, 1, 2, NULL, NULL, NOW()),
('20000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000003', '灭火剂选择', '<h2>灭火剂</h2><p>七氟丙烷、全氟己酮等是常用灭火剂。</p>', 30, 2, 3, '["20000000-0000-0000-0000-000000000006"]', '30000000-0000-0000-0000-000000000003', NOW());

-- 训练场景（含决策点）
-- difficulty: 1=基础, 2=中级, 3=高级 (与章节共用同一套难度体系)
-- prerequisite_ids: JSON数组，支持多条件解锁
INSERT IGNORE INTO scenarios (id, name, description, initial_conditions, events, decision_points, duration, difficulty, prerequisite_ids, created_at) VALUES
('30000000-0000-0000-0000-000000000001', '单电池热失控处置', '模拟单个电池单元发生热失控时的应急处置决策', '{"batteryCount":1,"initialTemperature":25,"batteryType":"lithium_iron_phosphate","capacity":0.1}', '[{"id":"e1","triggerTime":10,"type":"temperature_rise","description":"电池温度开始异常升高"},{"id":"e2","triggerTime":25,"type":"smoke","description":"电池开始冒烟"},{"id":"e3","triggerTime":40,"type":"fire","description":"电池起火"}]', '[{"id":"dp1","triggerCondition":"发现电池温度异常升高","question":"发现电池温度异常升高至60°C，应采取什么措施？","options":[{"id":"opt1","text":"立即切断电源并启动冷却系统","score":30,"correct":true},{"id":"opt2","text":"继续观察等待温度自行下降","score":0,"correct":false},{"id":"opt3","text":"仅启动冷却系统不断电","score":15,"correct":false},{"id":"opt4","text":"通知上级等待指示","score":10,"correct":false}],"timePressure":30},{"id":"dp2","triggerCondition":"电池开始冒烟","question":"电池开始冒烟，应采取什么措施？","options":[{"id":"opt5","text":"启动消防系统并疏散人员","score":35,"correct":true},{"id":"opt6","text":"用水直接灭火","score":0,"correct":false},{"id":"opt7","text":"用干粉灭火器灭火","score":20,"correct":false},{"id":"opt8","text":"打开通风系统排烟","score":10,"correct":false}],"timePressure":20},{"id":"dp3","triggerCondition":"电池起火","question":"电池已经起火，如何处理？","options":[{"id":"opt9","text":"启动全氟己酮灭火系统","score":35,"correct":true},{"id":"opt10","text":"使用水喷雾系统","score":25,"correct":false},{"id":"opt11","text":"使用干粉灭火器","score":15,"correct":false},{"id":"opt12","text":"等待消防部门","score":5,"correct":false}],"timePressure":15}]', 60, 1, NULL, NOW()),
('30000000-0000-0000-0000-000000000002', '电池组热扩散应急', '模拟电池组中一个电池热失控后向周围扩散的应急决策', '{"batteryCount":16,"initialTemperature":30,"batteryType":"ternary_lithium","capacity":1}', '[{"id":"e4","triggerTime":15,"type":"temperature_rise","description":"中心电池温度异常"},{"id":"e5","triggerTime":40,"type":"smoke","description":"热失控开始扩散"},{"id":"e6","triggerTime":70,"type":"fire","description":"相邻电池起火"}]', '[{"id":"dp4","triggerCondition":"中心电池温度异常","question":"BMS告警中心电池温度异常，如何处理？","options":[{"id":"opt13","text":"切断该电池模组电源并启动冷却","score":30,"correct":true},{"id":"opt14","text":"切断整个电池组电源","score":20,"correct":false},{"id":"opt15","text":"仅加强监控不断电","score":5,"correct":false},{"id":"opt16","text":"重启BMS系统","score":0,"correct":false}],"timePressure":25},{"id":"dp5","triggerCondition":"热失控开始扩散","question":"热失控开始向相邻电池扩散，应采取什么措施？","options":[{"id":"opt17","text":"启动区域隔离和消防系统","score":35,"correct":true},{"id":"opt18","text":"对所有电池强制冷却","score":20,"correct":false},{"id":"opt19","text":"仅疏散人员","score":10,"correct":false},{"id":"opt20","text":"关闭整个电站","score":15,"correct":false}],"timePressure":20}]', 120, 2, '["20000000-0000-0000-0000-000000000004"]', NOW()),
('30000000-0000-0000-0000-000000000003', '储能舱火灾处置', '模拟整个储能舱发生火灾时的指挥决策', '{"batteryCount":256,"initialTemperature":35,"batteryType":"ternary_lithium","capacity":10}', '[{"id":"e7","triggerTime":20,"type":"temperature_rise","description":"局部温度异常"},{"id":"e8","triggerTime":60,"type":"smoke","description":"大量烟雾产生"},{"id":"e9","triggerTime":120,"type":"fire","description":"储能舱起火"}]', '[{"id":"dp6","triggerCondition":"局部温度异常","question":"巡检发现储能舱局部温度异常，如何处置？","options":[{"id":"opt21","text":"立即启动应急预案，切断故障区域电源","score":30,"correct":true},{"id":"opt22","text":"加强巡检频次，暂不处理","score":5,"correct":false},{"id":"opt23","text":"远程调整BMS参数降温","score":15,"correct":false},{"id":"opt24","text":"关闭整个储能舱","score":20,"correct":false}],"timePressure":30},{"id":"dp7","triggerCondition":"大量烟雾产生","question":"储能舱内大量烟雾产生，如何处置？","options":[{"id":"opt25","text":"启动全舱消防系统，全员撤离","score":35,"correct":true},{"id":"opt26","text":"派人进入查看情况","score":0,"correct":false},{"id":"opt27","text":"仅启动通风排烟","score":10,"correct":false},{"id":"opt28","text":"关闭通风系统防止扩散","score":20,"correct":false}],"timePressure":15}]', 180, 3, '["20000000-0000-0000-0000-000000000006","20000000-0000-0000-0000-000000000004"]', NOW());

-- 事故案例（含时间线）
INSERT IGNORE INTO accident_cases (id, title, location, date, type, severity, description, timeline, cause_analysis, loss_estimate, lessons_learned, created_at, updated_at) VALUES
('40000000-0000-0000-0000-000000000001', '某储能电站锂电池起火事故', '广东省深圳市', '2025-03-15', 'fire', 'major', '某储能电站在运行过程中，一个电池模组发生热失控，导致起火。事故造成直接经济损失约200万元。', '[{"id":"t1","time":"03:15","title":"BMS告警","description":"BMS系统检测到3号电池模组温度异常，温度达到55°C","type":"warning"},{"id":"t2","time":"03:18","title":"温度持续升高","description":"3分钟内温度升至75°C，BMS发出紧急告警","type":"danger"},{"id":"t3","time":"03:22","title":"电池冒烟","description":"3号模组开始冒出白色烟雾，运维人员发现异常","type":"danger"},{"id":"t4","time":"03:25","title":"切断电源","description":"运维人员手动切断该模组电源","type":"action"},{"id":"t5","time":"03:30","title":"电池起火","description":"3号模组电池起火，火势向相邻模组蔓延","type":"danger"},{"id":"t6","time":"03:35","title":"启动消防","description":"启动气体灭火系统，消防部门接到报警","type":"action"},{"id":"t7","time":"04:15","title":"火势控制","description":"消防人员控制住火势，事故得到处置","type":"info"}]', '电池内部短路导致热失控，BMS未能及时切断电路。根本原因是电池制造缺陷导致隔膜损坏，引起内部短路。', '直接经济损失约200万元，包括设备损毁150万元、停产损失30万元、救援费用20万元。', '1. 加强BMS告警阈值设置，温度超过45°C即触发预警；2. 安装气体探测预警系统，实现早期发现；3. 定期进行电池内阻检测，及时发现异常电池；4. 完善应急预案，缩短响应时间。', NOW(), NOW()),
('40000000-0000-0000-0000-000000000002', '储能电站热失控扩散事故', '江苏省南京市', '2025-06-20', 'thermal_runaway', 'severe', '某储能电站因单体电池热失控引发连锁反应，导致整个电池舱起火。', '[{"id":"t8","time":"14:00","title":"单体异常","description":"巡检发现A区5号电池单体电压异常偏低","type":"warning"},{"id":"t9","time":"14:30","title":"温度升高","description":"5号电池温度快速升至80°C","type":"danger"},{"id":"t10","time":"14:35","title":"热扩散","description":"热量向相邻电池扩散，多个电池温度异常","type":"danger"},{"id":"t11","time":"14:40","title":"模组起火","description":"A区电池模组起火","type":"danger"},{"id":"t12","time":"14:50","title":"舱内蔓延","description":"火势蔓延至整个A区，产生大量有毒烟雾","type":"danger"},{"id":"t13","time":"15:00","title":"全面响应","description":"启动全站应急预案，消防力量到场","type":"action"},{"id":"t14","time":"16:30","title":"火灾扑灭","description":"火灾被扑灭，事故处置完成","type":"info"}]', '电池模组间缺乏有效的热隔离措施，热失控快速扩散。电池老化导致内阻增大，是热失控的诱因。', '直接经济损失约500万元，包括设备损毁380万元、停产损失80万元、环境修复40万元。', '1. 电池模组间必须设置防火隔离层；2. 安装自动灭火系统，实现早期灭火；3. 制定完善的应急预案，定期演练；4. 建立电池健康状态监测体系。', NOW(), NOW());

-- 案例结构化字段回填（data.sql 用 INSERT IGNORE 不会覆盖已存在行，故用 UPDATE 幂等补充）
UPDATE accident_cases SET
    timeline = '[{"id":"t1","time":"03:15","title":"BMS告警","description":"BMS系统检测到3号电池模组温度异常，温度达到55°C","type":"warning"},{"id":"t2","time":"03:18","title":"温度持续升高","description":"3分钟内温度升至75°C，BMS发出紧急告警","type":"danger"},{"id":"t3","time":"03:22","title":"电池冒烟","description":"3号模组开始冒出白色烟雾，运维人员发现异常","type":"danger"},{"id":"t4","time":"03:25","title":"切断电源","description":"运维人员手动切断该模组电源","type":"action"},{"id":"t5","time":"03:30","title":"电池起火","description":"3号模组电池起火，火势向相邻模组蔓延","type":"danger"},{"id":"t6","time":"03:35","title":"启动消防","description":"启动气体灭火系统，消防部门接到报警","type":"action"},{"id":"t7","time":"04:15","title":"火势控制","description":"消防人员控制住火势，事故得到处置","type":"info"}]',
    direct_cause = '电池内部短路引发热失控，BMS 未能及时切断电路',
    indirect_cause = 'BMS 告警阈值偏高，温度异常未在早期触发联动保护',
    root_cause = '电池制造缺陷导致隔膜损坏，引起内部短路',
    responsible_party = '电池供应商（制造缺陷）',
    casualties = '无人员伤亡',
    loss_amount = 200,
    loss_breakdown = '[{"label":"设备损毁","amount":150},{"label":"停产损失","amount":30},{"label":"救援费用","amount":20}]',
    lessons = '["加强 BMS 告警阈值设置，温度超过 45°C 即触发预警","安装气体探测预警系统，实现早期发现","定期进行电池内阻检测，及时发现异常电池","完善应急预案，缩短响应时间"]',
    difficulty = 'intermediate',
    study_minutes = 20
WHERE id = '40000000-0000-0000-0000-000000000001';

UPDATE accident_cases SET
    timeline = '[{"id":"t8","time":"14:00","title":"单体异常","description":"巡检发现A区5号电池单体电压异常偏低","type":"warning"},{"id":"t9","time":"14:30","title":"温度升高","description":"5号电池温度快速升至80°C","type":"danger"},{"id":"t10","time":"14:35","title":"热扩散","description":"热量向相邻电池扩散，多个电池温度异常","type":"danger"},{"id":"t11","time":"14:40","title":"模组起火","description":"A区电池模组起火","type":"danger"},{"id":"t12","time":"14:50","title":"舱内蔓延","description":"火势蔓延至整个A区，产生大量有毒烟雾","type":"danger"},{"id":"t13","time":"15:00","title":"全面响应","description":"启动全站应急预案，消防力量到场","type":"action"},{"id":"t14","time":"16:30","title":"火灾扑灭","description":"火灾被扑灭，事故处置完成","type":"info"}]',
    direct_cause = '单体电池热失控后向相邻电池快速扩散，引发连锁反应',
    indirect_cause = '电池老化导致内阻增大，发热加剧',
    root_cause = '电池模组间缺乏有效的防火热隔离措施',
    responsible_party = '电站设计方（热隔离设计缺陷）',
    casualties = '无人员伤亡',
    loss_amount = 500,
    loss_breakdown = '[{"label":"设备损毁","amount":380},{"label":"停产损失","amount":80},{"label":"环境修复","amount":40}]',
    lessons = '["电池模组间必须设置防火隔离层","安装自动灭火系统，实现早期灭火","制定完善的应急预案，定期演练","建立电池健康状态监测体系"]',
    difficulty = 'advanced',
    study_minutes = 30
WHERE id = '40000000-0000-0000-0000-000000000002';

-- 知识库数据
INSERT IGNORE INTO knowledge_base (id, category, title, content, tags, source, created_at, updated_at) VALUES
('50000000-0000-0000-0000-000000000001', 'procedure', '储能柜冒烟应急处置', '发现储能柜冒烟后：1. 立即切断电源；2. 启动消防系统；3. 疏散周围人员；4. 通知消防部门；5. 在安全距离外观察。切勿直接用水扑灭锂电池火灾。', '冒烟,应急,处置,消防', '储能电站安全管理规定', NOW(), NOW()),
('50000000-0000-0000-0000-000000000002', 'faq', '热失控前兆识别', '热失控前兆包括：1. 电池温度异常升高；2. 电压异常波动；3. 电池膨胀变形；4. 产生异味气体；5. BMS频繁告警。一旦发现应立即启动应急响应。', '热失控,前兆,BMS,温度', '锂离子电池储能系统技术规范', NOW(), NOW()),
('50000000-0000-0000-0000-000000000003', 'standard', '锂电池储能消防系统选择', '锂电池储能消防系统类型：1. 气体灭火系统（七氟丙烷、全氟己酮）；2. 水喷雾系统；3. 细水雾系统；4. 干粉灭火系统。大型储能舱推荐全氟己酮或组合式气体灭火方案。', '消防,灭火,全氟己酮,七氟丙烷', '电化学储能电站安全规程', NOW(), NOW()),
('50000000-0000-0000-0000-000000000004', 'regulation', 'BMS安全监控要点', 'BMS系统主要功能：1. 电池状态监控（电压、电流、温度）；2. SOC/SOH估算；3. 均衡管理；4. 故障诊断与预警；5. 热管理控制。应定期核查告警阈值与联动策略。', 'BMS,监控,告警,SOC', '储能电站安全管理规定', NOW(), NOW()),
('50000000-0000-0000-0000-000000000005', 'procedure', '储能电站安全检查清单', '储能电站安全检查重点：1. 电池外观检查；2. BMS告警记录；3. 消防系统状态；4. 通风系统运行；5. 电气连接检查；6. 接地系统检测。', '安全检查,巡检,运维', '储能电站运维规范', NOW(), NOW()),
('50000000-0000-0000-0000-000000000006', 'case', '热扩散隔离措施', '当单体电池热失控并向相邻电池扩散时，应优先切断故障模组电源，启动区域隔离和消防系统，避免火势蔓延至整个电池舱。', '热扩散,隔离,模组,应急', '典型事故案例分析', NOW(), NOW());

-- 张工学习进度（工作台演示数据）
INSERT IGNORE INTO user_progress (id, user_id, course_id, chapter_id, progress, completed, mastery_level, last_access_at) VALUES
('77000000-0000-0000-0000-000000000001', '660e8400-e29b-41d4-a716-446655440001', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 100, 1, 85, DATE_SUB(NOW(), INTERVAL 12 DAY)),
('77000000-0000-0000-0000-000000000002', '660e8400-e29b-41d4-a716-446655440001', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 100, 1, 90, DATE_SUB(NOW(), INTERVAL 10 DAY)),
('77000000-0000-0000-0000-000000000003', '660e8400-e29b-41d4-a716-446655440001', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', 100, 1, 88, DATE_SUB(NOW(), INTERVAL 8 DAY)),
('77000000-0000-0000-0000-000000000004', '660e8400-e29b-41d4-a716-446655440001', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000004', 68, 0, 55, DATE_SUB(NOW(), INTERVAL 1 DAY)),
('77000000-0000-0000-0000-000000000005', '660e8400-e29b-41d4-a716-446655440001', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000006', 100, 1, 92, DATE_SUB(NOW(), INTERVAL 5 DAY)),
('77000000-0000-0000-0000-000000000006', '660e8400-e29b-41d4-a716-446655440001', '10000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000008', 45, 0, 40, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- 张工训练记录
INSERT IGNORE INTO training_records (id, user_id, scenario_id, start_time, end_time, decisions, total_score, rating, feedback, created_at) VALUES
('88000000-0000-0000-0000-000000000001', '660e8400-e29b-41d4-a716-446655440001', '30000000-0000-0000-0000-000000000001', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), '[{"decisionPointId":"dp1","optionId":"opt1","score":30,"correct":true}]', 95, 'excellent', '表现优秀。全部决策正确，应急处置流程掌握扎实。', DATE_SUB(NOW(), INTERVAL 3 DAY));

-- 张工 AI 问答记录
INSERT IGNORE INTO qa_records (id, user_id, question, answer, sources, rating, created_at) VALUES
('99000000-0000-0000-0000-000000000001', '660e8400-e29b-41d4-a716-446655440001', '储能柜冒烟如何处理？', '发现储能柜冒烟后：1. 立即切断电源；2. 启动消防系统；3. 疏散周围人员；4. 通知消防部门。', '["储能柜冒烟应急处置"]', 5, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- 补充近期学习活动（学习趋势图）
INSERT IGNORE INTO user_progress (id, user_id, course_id, chapter_id, progress, completed, mastery_level, last_access_at) VALUES
('77000000-0000-0000-0000-000000000007', '660e8400-e29b-41d4-a716-446655440001', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000005', 25, 0, 28, DATE_SUB(NOW(), INTERVAL 0 DAY)),
('77000000-0000-0000-0000-000000000008', '660e8400-e29b-41d4-a716-446655440001', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 80, 0, 72, DATE_SUB(NOW(), INTERVAL 4 DAY));

-- 章节测验数据
INSERT IGNORE INTO chapter_quizzes (id, chapter_id, title, questions, pass_score, time_limit, created_at) VALUES
-- 储能概述测验
('60000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', '测验：储能概述',
'[
  {"id":"q1","type":"single","question":"储能的主要作用是什么？","options":[{"id":"a","text":"将电能转化为其他形式能量存储","correct":true},{"id":"b","text":"直接发电","correct":false},{"id":"c","text":"传输电力","correct":false},{"id":"d","text":"消耗电能","correct":false}],"explanation":"储能是指将电能转化为其他形式的能量存储起来，在需要时再转化为电能释放的技术。"},
  {"id":"q2","type":"single","question":"以下哪项不是储能技术解决的问题？","options":[{"id":"a","text":"新能源发电的间歇性","correct":false},{"id":"b","text":"电网负荷波动","correct":false},{"id":"c","text":"电力传输损耗","correct":true},{"id":"d","text":"新能源发电的波动性","correct":false}],"explanation":"储能主要解决新能源发电的间歇性和波动性问题，电力传输损耗不是储能直接解决的问题。"},
  {"id":"q3","type":"truefalse","question":"储能技术是解决新能源发电间歇性和波动性的关键手段。","options":[{"id":"a","text":"正确","correct":true},{"id":"b","text":"错误","correct":false}],"explanation":"储能技术确实是解决新能源发电间歇性和波动性的关键手段。"},
  {"id":"q4","type":"multiple","question":"储能系统可以应用在以下哪些场景？（多选）","options":[{"id":"a","text":"电网调峰","correct":true},{"id":"b","text":"新能源消纳","correct":true},{"id":"c","text":"直接发电","correct":false},{"id":"d","text":"备用电源","correct":true}],"explanation":"储能系统可应用于电网调峰、新能源消纳、备用电源等场景，但不能直接发电。"},
  {"id":"q5","type":"single","question":"储能技术的核心价值是什么？","options":[{"id":"a","text":"降低发电成本","correct":false},{"id":"b","text":"实现电能的时空转移","correct":true},{"id":"c","text":"提高发电效率","correct":false},{"id":"d","text":"减少电力需求","correct":false}],"explanation":"储能技术的核心价值是实现电能的时空转移，将低谷时段的电能转移到高峰时段使用。"}
]', 60, 15, NOW()),

-- 电池类型与特点测验
('60000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', '测验：电池类型与特点',
'[
  {"id":"q6","type":"single","question":"目前储能领域的主流电池类型是？","options":[{"id":"a","text":"铅酸电池","correct":false},{"id":"b","text":"锂离子电池","correct":true},{"id":"c","text":"液流电池","correct":false},{"id":"d","text":"钠硫电池","correct":false}],"explanation":"锂离子电池因其高能量密度和长寿命成为储能领域的主流选择。"},
  {"id":"q7","type":"single","question":"锂离子电池的主要优势是什么？","options":[{"id":"a","text":"成本最低","correct":false},{"id":"b","text":"安全性最高","correct":false},{"id":"c","text":"高能量密度和长寿命","correct":true},{"id":"d","text":"技术最简单","correct":false}],"explanation":"锂离子电池的主要优势是高能量密度和长循环寿命。"},
  {"id":"q8","type":"truefalse","question":"铅酸电池的能量密度高于锂离子电池。","options":[{"id":"a","text":"正确","correct":false},{"id":"b","text":"错误","correct":true}],"explanation":"铅酸电池的能量密度远低于锂离子电池，这是锂离子电池成为主流的重要原因之一。"},
  {"id":"q9","type":"single","question":"液流电池的主要特点是什么？","options":[{"id":"a","text":"能量密度最高","correct":false},{"id":"b","text":"功率密度最高","correct":false},{"id":"c","text":"容量可独立扩展","correct":true},{"id":"d","text":"成本最低","correct":false}],"explanation":"液流电池的主要特点是容量和功率可以独立扩展，适合大规模储能。"},
  {"id":"q10","type":"multiple","question":"以下哪些是常见的储能电池类型？（多选）","options":[{"id":"a","text":"锂离子电池","correct":true},{"id":"b","text":"铅酸电池","correct":true},{"id":"c","text":"液流电池","correct":true},{"id":"d","text":"汽油电池","correct":false}],"explanation":"常见的储能电池类型包括锂离子电池、铅酸电池、液流电池等，汽油电池不是储能电池类型。"}
]', 60, 15, NOW()),

-- 热失控概述测验
('60000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000004', '测验：热失控概述',
'[
  {"id":"q11","type":"single","question":"什么是电池热失控？","options":[{"id":"a","text":"电池温度缓慢下降","correct":false},{"id":"b","text":"电池温度不可控上升的现象","correct":true},{"id":"c","text":"电池电压突然升高","correct":false},{"id":"d","text":"电池容量逐渐衰减","correct":false}],"explanation":"热失控是电池温度不可控上升的现象，可能导致起火或爆炸。"},
  {"id":"q12","type":"multiple","question":"热失控可能导致哪些后果？（多选）","options":[{"id":"a","text":"起火","correct":true},{"id":"b","text":"爆炸","correct":true},{"id":"c","text":"电池容量增加","correct":false},{"id":"d","text":"有毒气体释放","correct":true}],"explanation":"热失控可能导致起火、爆炸、有毒气体释放等严重后果。"},
  {"id":"q13","type":"single","question":"热失控的根本原因是什么？","options":[{"id":"a","text":"电池过充","correct":false},{"id":"b","text":"电池内部化学反应失控","correct":true},{"id":"c","text":"外部高温","correct":false},{"id":"d","text":"电池老化","correct":false}],"explanation":"热失控的根本原因是电池内部化学反应失控，导致温度持续升高。"},
  {"id":"q14","type":"truefalse","question":"热失控是储能电站最常见的安全事故类型之一。","options":[{"id":"a","text":"正确","correct":true},{"id":"b","text":"错误","correct":false}],"explanation":"热失控确实是储能电站最常见的安全事故类型之一，是安全管理的重点。"},
  {"id":"q15","type":"single","question":"预防热失控的关键措施是什么？","options":[{"id":"a","text":"增加电池数量","correct":false},{"id":"b","text":"完善BMS监控和预警","correct":true},{"id":"c","text":"降低充电电压","correct":false},{"id":"d","text":"减少放电深度","correct":false}],"explanation":"预防热失控的关键措施是完善BMS监控和预警系统，实现早期发现和处置。"}
]', 60, 15, NOW()),

-- 消防系统概述测验
('60000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000006', '测验：消防系统概述',
'[
  {"id":"q16","type":"multiple","question":"储能电站常用的消防系统类型有哪些？（多选）","options":[{"id":"a","text":"气体灭火系统","correct":true},{"id":"b","text":"水喷雾系统","correct":true},{"id":"c","text":"干粉灭火系统","correct":false},{"id":"d","text":"泡沫灭火系统","correct":false}],"explanation":"储能电站常用气体灭火系统和水喷雾系统，干粉和泡沫系统不常用于储能电站。"},
  {"id":"q17","type":"single","question":"气体灭火系统的主要优势是什么？","options":[{"id":"a","text":"成本最低","correct":false},{"id":"b","text":"不导电、不留残留","correct":true},{"id":"c","text":"灭火速度最快","correct":false},{"id":"d","text":"维护最简单","correct":false}],"explanation":"气体灭火系统的主要优势是不导电、灭火后不留残留，适合电气设备。"},
  {"id":"q18","type":"truefalse","question":"水喷雾系统可以直接用于扑灭锂电池火灾。","options":[{"id":"a","text":"正确","correct":false},{"id":"b","text":"错误","correct":true}],"explanation":"锂电池火灾不能直接用水扑灭，可能导致触电或火势扩大。需要使用专用灭火剂。"},
  {"id":"q19","type":"single","question":"大型储能舱推荐使用什么灭火方案？","options":[{"id":"a","text":"干粉灭火器","correct":false},{"id":"b","text":"全氟己酮或组合式气体灭火","correct":true},{"id":"c","text":"泡沫灭火系统","correct":false},{"id":"d","text":"二氧化碳灭火器","correct":false}],"explanation":"大型储能舱推荐使用全氟己酮或组合式气体灭火方案。"},
  {"id":"q20","type":"single","question":"消防系统设计的首要原则是什么？","options":[{"id":"a","text":"成本最低","correct":false},{"id":"b","text":"早期发现、快速响应","correct":true},{"id":"c","text":"设备最少","correct":false},{"id":"d","text":"维护最简单","correct":false}],"explanation":"消防系统设计的首要原则是早期发现、快速响应，最大限度减少损失。"}
]', 60, 15, NOW());
