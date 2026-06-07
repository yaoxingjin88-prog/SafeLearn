-- 事故推演引擎扩展表（在现有 schema.sql 之后执行）
-- 注意：COLLATE 须与 users/scenarios 表一致（utf8mb4_unicode_ci）

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
