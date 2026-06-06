-- SafeLearn 储安云 - MySQL 建表脚本

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
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cases_type (type),
    INDEX idx_cases_severity (severity)
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
