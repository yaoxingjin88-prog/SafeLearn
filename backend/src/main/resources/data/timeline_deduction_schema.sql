-- 时间轴式事故推演系统（北京丰台 4·16 等案例）

CREATE TABLE IF NOT EXISTS td_scenarios (
    id CHAR(36) PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    case_ref VARCHAR(200),
    description TEXT,
    difficulty VARCHAR(20) DEFAULT 'advanced',
    duration_minutes INT DEFAULT 45,
    definition_json JSON NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS td_nodes (
    id CHAR(36) PRIMARY KEY,
    scenario_id CHAR(36) NOT NULL,
    node_key VARCHAR(64) NOT NULL,
    phase VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    narrative TEXT,
    offset_min INT NOT NULL DEFAULT 0,
    scene_state JSON,
    telemetry_snapshot JSON,
    sort_order INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_td_node (scenario_id, node_key),
    INDEX idx_td_nodes_scenario (scenario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS td_decisions (
    id CHAR(36) PRIMARY KEY,
    scenario_id CHAR(36) NOT NULL,
    node_key VARCHAR(64) NOT NULL,
    question TEXT NOT NULL,
    options_json JSON NOT NULL,
    correct_option_id VARCHAR(16) NOT NULL,
    regulation_ref VARCHAR(500),
    sort_order INT NOT NULL DEFAULT 0,
    INDEX idx_td_decisions_scenario (scenario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    INDEX idx_td_sessions_user (user_id),
    INDEX idx_td_sessions_scenario (scenario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS td_session_decisions (
    id CHAR(36) PRIMARY KEY,
    session_id CHAR(36) NOT NULL,
    decision_id CHAR(36),
    node_key VARCHAR(64) NOT NULL,
    option_id VARCHAR(16) NOT NULL,
    is_correct TINYINT(1) NOT NULL,
    response_ms INT,
    risk_delta INT DEFAULT 0,
    consequence TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_td_sd_session (session_id)
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

CREATE TABLE IF NOT EXISTS td_course_links (
    id CHAR(36) PRIMARY KEY,
    scenario_id CHAR(36) NOT NULL,
    trigger_node_key VARCHAR(64),
    wrong_option_id VARCHAR(16),
    course_id CHAR(36),
    course_title VARCHAR(200) NOT NULL,
    reason VARCHAR(500),
    INDEX idx_td_course_scenario (scenario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
