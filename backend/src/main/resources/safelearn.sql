-- SafeLearn 储安云 - 完整数据库脚本（建表 + 初始数据）
-- 手动执行: mysql -u root -p < safelearn.sql
-- Spring Boot 启动时通过 spring.sql.init.schema-locations 自动执行

-- ========== 数据库与核心表 ==========

CREATE DATABASE IF NOT EXISTS safelearn DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE safelearn;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id CHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'trainee',
    company VARCHAR(100),
    department VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 课程表
CREATE TABLE IF NOT EXISTS courses (
    id CHAR(36) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    cover_image VARCHAR(500),
    category VARCHAR(50) NOT NULL,
    total_duration INT,
    status VARCHAR(20) DEFAULT 'published',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_courses_category (category),
    INDEX idx_courses_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 课程分类表
CREATE TABLE IF NOT EXISTS course_categories (
    id CHAR(36) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    tag_type VARCHAR(20) DEFAULT '',
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_course_categories_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 章节表
CREATE TABLE IF NOT EXISTS chapters (
    id CHAR(36) PRIMARY KEY,
    course_id CHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    video_url VARCHAR(500),
    duration INT,
    order_num INT NOT NULL,
    difficulty_level INT NOT NULL DEFAULT 1 COMMENT '1=基础理论, 2=案例分析, 3=高级实操',
    prerequisite_ids JSON DEFAULT NULL COMMENT '前置章节ID数组',
    scenario_id CHAR(36) DEFAULT NULL COMMENT '关联的训练场景ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    INDEX idx_chapters_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学习进度表
CREATE TABLE IF NOT EXISTS user_progress (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    course_id CHAR(36) NOT NULL,
    chapter_id CHAR(36) NOT NULL,
    progress INT DEFAULT 0,
    completed TINYINT(1) DEFAULT 0,
    mastery_level INT DEFAULT 0 COMMENT '掌握度评级(0-100)',
    study_seconds INT DEFAULT 0 COMMENT '累计有效学习时长(秒)',
    last_access_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_chapter (user_id, chapter_id),
    INDEX idx_progress_user (user_id),
    INDEX idx_progress_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 推演场景表
CREATE TABLE IF NOT EXISTS scenarios (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    initial_conditions JSON NOT NULL,
    events JSON NOT NULL,
    decision_points JSON,
    duration INT NOT NULL,
    difficulty INT NOT NULL DEFAULT 1 COMMENT '1=基础, 2=中级, 3=高级',
    prerequisite_ids JSON DEFAULT NULL COMMENT '前置章节ID数组',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 训练记录表
CREATE TABLE IF NOT EXISTS training_records (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    scenario_id CHAR(36),
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    decisions JSON,
    total_score INT,
    rating VARCHAR(20),
    feedback TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (scenario_id) REFERENCES scenarios(id) ON DELETE SET NULL,
    INDEX idx_training_user (user_id),
    INDEX idx_training_scenario (scenario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 事故案例表
CREATE TABLE IF NOT EXISTS accident_cases (
    id CHAR(36) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    location VARCHAR(200),
    date DATE,
    type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description TEXT,
    timeline JSON,
    cause_analysis TEXT,
    loss_estimate TEXT,
    lessons_learned TEXT,
    direct_cause TEXT,
    indirect_cause TEXT,
    root_cause TEXT,
    responsible_party VARCHAR(200),
    casualties VARCHAR(100),
    loss_amount INT,
    loss_breakdown JSON,
    lessons JSON,
    difficulty VARCHAR(20),
    study_minutes INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cases_type (type),
    INDEX idx_cases_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户案例复盘进度
CREATE TABLE IF NOT EXISTS user_case_progress (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    case_id CHAR(36) NOT NULL,
    current_step INT NOT NULL DEFAULT 0,
    total_steps INT NOT NULL DEFAULT 0,
    completed TINYINT(1) NOT NULL DEFAULT 0,
    reflections JSON,
    completed_at DATETIME,
    last_access_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_case (user_id, case_id),
    INDEX idx_ucp_user (user_id),
    INDEX idx_ucp_case (case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 知识库表
CREATE TABLE IF NOT EXISTS knowledge_base (
    id CHAR(36) PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    tags TEXT,
    source VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_kb_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI问答记录表
CREATE TABLE IF NOT EXISTS qa_records (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36),
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    sources TEXT,
    rating INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_qa_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 章节测验配置表
CREATE TABLE IF NOT EXISTS chapter_quizzes (
    id VARCHAR(36) PRIMARY KEY,
    chapter_id VARCHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    questions JSON NOT NULL COMMENT '题目数组',
    pass_score INT DEFAULT 60 COMMENT '及格分数',
    time_limit INT COMMENT '限时(分钟), null为不限时',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE,
    INDEX idx_quiz_chapter (chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 测验尝试记录表
CREATE TABLE IF NOT EXISTS quiz_attempts (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    quiz_id VARCHAR(36) NOT NULL,
    answers JSON COMMENT '用户答案和错题记录',
    score INT DEFAULT 0 COMMENT '得分(0-100)',
    passed BOOLEAN DEFAULT FALSE,
    mastery_level INT DEFAULT 0 COMMENT '掌握度',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (quiz_id) REFERENCES chapter_quizzes(id) ON DELETE CASCADE,
    INDEX idx_attempt_user (user_id),
    INDEX idx_attempt_quiz (quiz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 综合考试记录表（跨章节抽题快照）
CREATE TABLE IF NOT EXISTS comprehensive_exam_attempts (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    questions JSON NOT NULL COMMENT '抽题快照',
    answers JSON COMMENT '答题与错题',
    score INT DEFAULT 0,
    passed BOOLEAN DEFAULT FALSE,
    pass_score INT DEFAULT 70,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    INDEX idx_comp_exam_user (user_id),
    INDEX idx_comp_exam_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 学习体验增强表 ==========

CREATE TABLE IF NOT EXISTS user_notes (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    target_type VARCHAR(20) NOT NULL COMMENT 'chapter/case',
    target_id CHAR(36) NOT NULL,
    course_id CHAR(36) DEFAULT NULL COMMENT '章节笔记关联课程',
    content TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notes_user (user_id),
    INDEX idx_notes_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_favorites (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    target_type VARCHAR(20) NOT NULL COMMENT 'course/case',
    target_id CHAR(36) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_favorite (user_id, target_type, target_id),
    INDEX idx_fav_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS certificate_templates (
    code VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    badge_label VARCHAR(50) NOT NULL,
    title_suffix VARCHAR(100) NOT NULL,
    validity_months INT NOT NULL DEFAULT 24,
    early_renew_days INT NOT NULL DEFAULT 30,
    border_color VARCHAR(20),
    accent_color VARCHAR(20),
    header_title VARCHAR(50),
    body_template TEXT,
    renewal_requirement VARCHAR(30) DEFAULT 'comprehensive_exam',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_certificates (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    course_id CHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    certificate_no VARCHAR(50) NOT NULL,
    cert_level VARCHAR(20) NOT NULL DEFAULT 'advanced' COMMENT 'advanced/basic/professional',
    template_code VARCHAR(32) DEFAULT 'advanced',
    expires_at DATETIME DEFAULT NULL,
    status VARCHAR(20) DEFAULT 'active' COMMENT 'active|expired|revoked',
    renewal_count INT DEFAULT 0,
    last_renewed_at DATETIME DEFAULT NULL,
    issue_source VARCHAR(30) DEFAULT 'course_complete' COMMENT 'course_complete|comprehensive_exam|renewal',
    issued_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_course_cert (user_id, course_id),
    UNIQUE KEY uk_cert_no (certificate_no),
    INDEX idx_cert_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 事故推演引擎表 ==========
-- COLLATE 须与 users/scenarios 表一致（utf8mb4_unicode_ci）

-- 推演会话表
CREATE TABLE IF NOT EXISTS simulation_sessions (
    id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY,
    user_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    scenario_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'running' COMMENT 'running|paused|completed|aborted',
    outcome VARCHAR(20) DEFAULT NULL COMMENT 'success|failure',
    branch VARCHAR(30) DEFAULT 'none' COMMENT 'venting|isolation|fire|evacuate',
    elapsed_ms BIGINT DEFAULT 0,
    max_temperature DECIMAL(8,2) DEFAULT 0,
    rule_score INT DEFAULT 0,
    ai_score INT DEFAULT NULL,
    total_score INT DEFAULT NULL,
    rating VARCHAR(20) DEFAULT NULL,
    machine_state VARCHAR(50) DEFAULT 'idle',
    started_at DATETIME NOT NULL,
    finished_at DATETIME DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (scenario_id) REFERENCES scenarios(id) ON DELETE CASCADE,
    INDEX idx_sim_session_user (user_id),
    INDEX idx_sim_session_scenario (scenario_id),
    INDEX idx_sim_session_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 事件日志（回放数据源）
CREATE TABLE IF NOT EXISTS simulation_event_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    seq INT NOT NULL,
    elapsed_ms BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    machine_state VARCHAR(50) NOT NULL,
    payload JSON NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES simulation_sessions(id) ON DELETE CASCADE,
    UNIQUE KEY uk_session_seq (session_id, seq),
    INDEX idx_event_session_time (session_id, elapsed_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户决策记录
CREATE TABLE IF NOT EXISTS simulation_decisions (
    id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY,
    session_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    decision_point_id VARCHAR(64) NOT NULL,
    option_id VARCHAR(64) NOT NULL,
    elapsed_ms BIGINT NOT NULL,
    response_time_ms BIGINT NOT NULL,
    score_delta INT DEFAULT 0,
    is_optimal TINYINT(1) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES simulation_sessions(id) ON DELETE CASCADE,
    INDEX idx_decision_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 状态快照（加速 seek 回放）
CREATE TABLE IF NOT EXISTS simulation_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    seq INT NOT NULL,
    elapsed_ms BIGINT NOT NULL,
    machine_state VARCHAR(50) NOT NULL,
    cells JSON NOT NULL,
    environment JSON NOT NULL,
    score INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES simulation_sessions(id) ON DELETE CASCADE,
    INDEX idx_snapshot_session_time (session_id, elapsed_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AI 评分报告
CREATE TABLE IF NOT EXISTS simulation_score_reports (
    id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY,
    session_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE,
    rule_score INT NOT NULL DEFAULT 0,
    ai_score INT DEFAULT NULL,
    total_score INT NOT NULL DEFAULT 0,
    rating VARCHAR(20) NOT NULL,
    dimensions JSON NOT NULL COMMENT 'timeliness/procedure/decision/outcome',
    highlights JSON DEFAULT NULL,
    improvements JSON DEFAULT NULL,
    instructor_summary TEXT,
    raw_ai_response TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES simulation_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 初始数据 ==========
-- 默认管理员 (密码: admin123)
INSERT IGNORE INTO users (id, username, email, password_hash, role, company, department, created_at, updated_at)
VALUES ('550e8400-e29b-41d4-a716-446655440000', 'admin', 'admin@example.com', '$2a$10$H2wPB2Pxr9eac3CKbwcqJejquPzbbAjm6VCj8kC0nlV21X2gbk7hK', 'admin', '储能科技公司', '安全管理部', NOW(), NOW());

-- 测试用户 (密码均为 admin123)
INSERT IGNORE INTO users (id, username, email, password_hash, role, company, department, created_at, updated_at) VALUES
('660e8400-e29b-41d4-a716-446655440001', 'zhanggong', 'zhanggong@safelearn.com', '$2a$10$H2wPB2Pxr9eac3CKbwcqJejquPzbbAjm6VCj8kC0nlV21X2gbk7hK', 'trainee', '华能储能电站', '运维部', DATE_SUB(NOW(), INTERVAL 44 DAY), NOW()),
('660e8400-e29b-41d4-a716-446655440002', 'lisi', 'lisi@safelearn.com', '$2a$10$H2wPB2Pxr9eac3CKbwcqJejquPzbbAjm6VCj8kC0nlV21X2gbk7hK', 'trainee', '国电新能源', '安全部', DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),
('660e8400-e29b-41d4-a716-446655440003', 'wangwu', 'wangwu@safelearn.com', '$2a$10$H2wPB2Pxr9eac3CKbwcqJejquPzbbAjm6VCj8kC0nlV21X2gbk7hK', 'trainee', '南方电网储能', '培训部', DATE_SUB(NOW(), INTERVAL 60 DAY), NOW());

-- 课程分类
INSERT IGNORE INTO course_categories (id, code, name, tag_type, sort_order, enabled) VALUES
('cc000000-0000-0000-0000-000000000001', 'basic', '基础知识', '', 1, 1),
('cc000000-0000-0000-0000-000000000002', 'battery', '电池安全', 'success', 2, 1),
('cc000000-0000-0000-0000-000000000003', 'thermal', '热失控', 'danger', 3, 1),
('cc000000-0000-0000-0000-000000000004', 'fire', '消防安全', 'warning', 4, 1),
('cc000000-0000-0000-0000-000000000005', 'bms', 'BMS系统', 'info', 5, 1),
('cc000000-0000-0000-0000-000000000006', 'case', '事故案例', '', 6, 1);

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

-- 案例结构化字段回填（INSERT IGNORE 不会覆盖已存在行，故用 UPDATE 幂等补充）
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

UPDATE accident_cases SET direct_cause = '内短路引发磷酸铁锂电池热失控', indirect_cause = '巡检缺失；应急预案未演练；消防系统未联动', root_cause = '电池内短路叠加运维管理与消防联动缺陷，导致事故链扩大为火灾爆炸', casualties = '2名消防员牺牲、1人遇难、1人受伤', loss_amount = 1661, loss_breakdown = '[{"label":"财产损失","amount":1661}]', lessons = '["严格落实储能电站日常巡检与异常处置","定期组织应急预案演练","确保消防系统与储能BMS联动有效","加强磷酸铁锂储能热失控早期预警"]', difficulty = 'advanced', study_minutes = 35 WHERE id = '40000000-0000-0000-0000-000000000003';
UPDATE accident_cases SET direct_cause = '电池内短路引发热失控', indirect_cause = '巡检缺失；应急预案未演练', root_cause = '内短路触发单舱热失控，早期巡检与应急准备不足延误处置', casualties = '无人员伤亡', loss_breakdown = '[{"label":"预制舱损毁","amount":0}]', lessons = '["加强预制舱级温度监测与告警","落实定期巡检制度","完善并演练预制舱火灾应急预案","设置舱间防火隔离措施"]', difficulty = 'intermediate', study_minutes = 20 WHERE id = '40000000-0000-0000-0000-000000000004';
UPDATE accident_cases SET direct_cause = '内短路引发锂电池包闪爆', indirect_cause = '应急预案未演练；操作人员无证上岗', root_cause = '测试环节管理失控与人员资质缺失导致致命事故', casualties = '1人死亡', lessons = '["储能测试作业必须持证上岗","测试前完成风险辨识与应急演练","严格测试过程监护与隔离措施","电池包异常立即中止作业"]', difficulty = 'advanced', study_minutes = 25 WHERE id = '40000000-0000-0000-0000-000000000005';
UPDATE accident_cases SET direct_cause = '内短路引发储能柜起火', indirect_cause = '消防系统未联动；巡检缺失', root_cause = '电池故障未能被早期发现，消防联动不足延缓处置', casualties = '无人员伤亡', lessons = '["用户侧储能柜应接入消防联动","落实物流园区储能巡检","明确园区与消防的应急联络机制","储能柜周边保持安全间距"]', difficulty = 'intermediate', study_minutes = 18 WHERE id = '40000000-0000-0000-0000-000000000006';
UPDATE accident_cases SET direct_cause = '电池热失控', indirect_cause = '巡检缺失；应急预案未演练；消防系统未联动', root_cause = '热失控未被早期识别，多重管理缺陷并存', casualties = '待查', lessons = '["工商业储能应纳入企业安全生产管理","建立日常巡检与异常记录制度","定期演练火灾应急预案","确保消防系统有效联动"]', difficulty = 'intermediate', study_minutes = 20 WHERE id = '40000000-0000-0000-0000-000000000007';
UPDATE accident_cases SET direct_cause = '三元锂电池热失控', indirect_cause = '巡检缺失；消防系统未联动', root_cause = '关键基础设施储能安全防护不足，火灾演变为系统性风险', casualties = '无人员伤亡（网络服务中断）', lessons = '["关键基础设施储能须独立消防与供电冗余","加强数据中心ESS专项巡检","建立跨部门应急联动机制","评估三元锂在关键场景的适用性"]', difficulty = 'advanced', study_minutes = 30 WHERE id = '40000000-0000-0000-0000-000000000008';
UPDATE accident_cases SET direct_cause = '热失控；BMS失效（推断）', indirect_cause = '应急预案未演练；巡检缺失', root_cause = '大型电站反复发生事故，系统性安全治理不足', casualties = '无人员伤亡；1200+居民疏散', lessons = '["对反复发生事故的电站开展全面安全评估","强化BMS与消防系统可靠性验证","建立复燃监测与长期冷却方案","加强社区沟通与应急疏散预案"]', difficulty = 'advanced', study_minutes = 35 WHERE id = '40000000-0000-0000-0000-000000000009';
UPDATE accident_cases SET direct_cause = '热失控；BMS失效（推断）', indirect_cause = '消防系统未联动；应急预案未演练', root_cause = '超大规模三元锂储能火灾长期复燃，暴露设计与应急短板', casualties = '无人员伤亡', lessons = '["大型储能项目须验证消防系统与BMS联动","制定长期复燃监测方案","加强三元锂电池热失控气体防护","定期演练大型电站火灾应急预案"]', difficulty = 'advanced', study_minutes = 35 WHERE id = '40000000-0000-0000-0000-00000000000a';
UPDATE accident_cases SET direct_cause = '热失控引发集装箱储能火灾', indirect_cause = '应急预案未演练；消防系统未联动', root_cause = '集装箱布置与消防处置预案不足导致扑救周期长', casualties = '2名消防员轻伤', loss_amount = 40, loss_breakdown = '[{"label":"财产损失（约50万欧元）","amount":40}]', lessons = '["集装箱储能应规范布置与防火间距","消防处置前评估爆炸与有毒气体风险","定期演练集装箱火灾应急预案","加强消防员储能火灾专项培训"]', difficulty = 'intermediate', study_minutes = 22 WHERE id = '40000000-0000-0000-0000-00000000000b';
UPDATE accident_cases SET direct_cause = '内短路；外部短路；过充', indirect_cause = '巡检缺失；BMS失效', root_cause = '韩国ESS行业快速扩张期安全标准与监管未能同步', casualties = '系列事故累计（无单一伤亡汇总）', lessons = '["建立ESS全生命周期安全监测体系","强化BMS软件与硬件可靠性","对问题批次电池及时召回更换","国家层面建立储能事故统计与预警机制"]', difficulty = 'advanced', study_minutes = 40 WHERE id = '40000000-0000-0000-0000-00000000000c';
UPDATE accident_cases SET direct_cause = '热失控引发工厂火灾', indirect_cause = '巡检缺失；应急预案未演练', root_cause = '电池工厂生产与仓储环节火灾防控不足', casualties = '待查', lessons = '["电池工厂须严格分区防火管理","加强生产环节热失控监测","完善工厂火灾应急预案并演练","危险品仓储符合消防规范"]', difficulty = 'intermediate', study_minutes = 18 WHERE id = '40000000-0000-0000-0000-00000000000d';
UPDATE accident_cases SET direct_cause = '热失控；BMS失效', indirect_cause = '巡检缺失；应急预案未演练；消防系统未联动', root_cause = '行业共性管理缺陷导致热失控事故高发并呈级联特征', casualties = '统计案例（17起火灾）', lessons = '["从行业统计看须强化热失控早期预警","推动消防与BMS联动标准落地","建立全国储能事故案例共享机制","针对级联反应加强舱间隔离设计"]', difficulty = 'intermediate', study_minutes = 25 WHERE id = '40000000-0000-0000-0000-00000000000e';

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

-- ========== 时间轴事故推演 ==========
CREATE TABLE IF NOT EXISTS td_sessions (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    scenario_id CHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'running',
    current_phase VARCHAR(32),
    current_node_key VARCHAR(64),
    branch_path VARCHAR(64) DEFAULT 'standard',
    outcome VARCHAR(32),
    risk_index INT DEFAULT 0,
    engine_state JSON,
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    ended_at DATETIME,
    INDEX idx_td_sessions_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS td_scores (
    id CHAR(36) PRIMARY KEY,
    session_id CHAR(36) NOT NULL UNIQUE,
    total_score INT NOT NULL,
    risk_identification INT DEFAULT 0,
    decision_making INT DEFAULT 0,
    emergency_response INT DEFAULT 0,
    accident_analysis INT DEFAULT 0,
    ai_comment TEXT,
    strengths_json JSON,
    weaknesses_json JSON,
    recommendations_json JSON,
    recommended_courses_json JSON,
    fishbone_json JSON,
    five_why_json JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 时间轴事故推演 ==========
CREATE TABLE IF NOT EXISTS td_sessions (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    scenario_id CHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'running',
    current_phase VARCHAR(32),
    current_node_key VARCHAR(64),
    branch_path VARCHAR(64) DEFAULT 'standard',
    outcome VARCHAR(32),
    risk_index INT DEFAULT 0,
    engine_state JSON,
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    ended_at DATETIME,
    INDEX idx_td_sessions_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS td_scores (
    id CHAR(36) PRIMARY KEY,
    session_id CHAR(36) NOT NULL UNIQUE,
    total_score INT NOT NULL,
    risk_identification INT DEFAULT 0,
    decision_making INT DEFAULT 0,
    emergency_response INT DEFAULT 0,
    accident_analysis INT DEFAULT 0,
    ai_comment TEXT,
    strengths_json JSON,
    weaknesses_json JSON,
    recommendations_json JSON,
    recommended_courses_json JSON,
    fishbone_json JSON,
    five_why_json JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
