-- 用户案例复盘进度表（已有库可单独执行）
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
