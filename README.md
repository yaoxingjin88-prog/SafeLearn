# SafeLearn（储安云）

储能电站安全培训与事故推演系统，为储能行业从业人员提供在线安全培训、事故案例分析、应急推演训练和 AI 智能问答的一体化平台。

系统采用 **「学员消费端 + 管理治理端」** 双端分离设计：学员在 `/user/*` 完成学、练、评、查；管理员在 `/dashboard` 与 `/admin/*` 完成内容配置、组织治理与数据监控。

## 技术栈

**前端**
- Vue 3 + TypeScript + Vite
- Element Plus + UnoCSS
- Three.js（3D 可视化）+ ECharts（数据图表）
- Pinia（状态管理）+ Vue Router
- XState（推演/训练状态机）

**后端**
- Spring Boot 3.3 + Java 17
- Spring Data JPA + MySQL
- Spring Security + JWT 认证

**AI**
- 通义千问（DashScope OpenAI 兼容接口）

## 项目结构

```
SafeLearn/
├── frontend/                      # 前端 Vue 项目
│   ├── src/
│   │   ├── api/                   # 接口请求
│   │   ├── components/            # 公共组件
│   │   ├── composables/           # 组合式函数
│   │   ├── layouts/               # 布局（MainLayout / UserLayout）
│   │   ├── pages/
│   │   │   ├── admin/             # 管理端（课程、题库、组卷、用户、权限等）
│   │   │   ├── user/              # 学员端工作台、考试中心、学习报告等
│   │   │   ├── courses/           # 课程学习、技能树、错题本
│   │   │   ├── simulation/        # 事故推演与回放
│   │   │   ├── timeline-deduction/# 时间轴推演
│   │   │   ├── test-simulation/   # 测试作业推演
│   │   │   ├── training/          # 应急训练
│   │   │   ├── cases/             # 事故案例
│   │   │   ├── ai/                # AI 安全问答
│   │   │   ├── dashboard/         # 管理端驾驶舱、预警、报表
│   │   │   └── auth/              # 登录注册
│   │   ├── simulation/            # 推演引擎
│   │   ├── router/                # 路由配置
│   │   ├── stores/                # Pinia 状态管理
│   │   └── types/                 # TypeScript 类型定义
│   └── package.json
│
├── backend/                       # 后端 Spring Boot 项目
│   └── src/main/java/com/safelearn/
│       ├── controller/            # REST 控制器
│       ├── service/               # 业务逻辑层
│       ├── entity/                # 实体类
│       ├── repository/            # 数据访问层
│       ├── deduction/             # 经典推演模块
│       ├── timeline/              # 时间轴推演模块
│       ├── testsim/               # 测试作业推演模块
│       ├── security/              # 权限与 JWT
│       └── config/                # 配置与数据初始化
│
├── docs/                          # 参赛文档、运行说明、双端工作流分析
└── wechat-article-writer/         # 公众号内容创作工具（可选）
```

## 功能模块

### 学员端（`/user/*`）

| 模块 | 说明 |
|------|------|
| 工作台 | 学习数据概览、待办提醒、快捷入口 |
| 统一搜索 | 跨课程、案例、场景的全局搜索 |
| 安全培训 | 课程列表、章节学习、技能树、我的学习、章节测验、综合考试 |
| 考试中心 | 独立试卷考试、成绩查询 |
| 学习报告 | 个人学习数据分析 |
| 错题本 | 错题回顾与掌握标记 |
| 事故推演 | 经典 3D 推演、时间轴推演、测试作业推演、推演记录与回放 |
| 应急训练 | 场景决策训练、训练报告生成 |
| 事故案例 | 案例库、案例详情（时间线、原因分析、教训总结） |
| AI 安全问答 | 基于知识库的智能问答、问答历史、草稿同步 |
| 学习中心 | 学习日历、我的收藏、我的证书 |

### 管理端（`/dashboard`、`/admin/*`）

| 模块 | 说明 |
|------|------|
| 驾驶舱 | 数据概览、预警管理、学习报表 |
| 培训课程 | 课程管理、分类配置、题库管理、组卷管理、考试管理、学习监控 |
| 实训内容 | 案例管理、推演场景管理 |
| 组织与账号 | 用户管理、组织与部门、角色与权限、系统设置 |

## 快速开始

### 环境要求

- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Node.js 18+
- 浏览器支持 WebGL（推演/训练页面需要）

### 数据库初始化

```sql
CREATE DATABASE safelearn DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

启动后端后会自动建表并初始化种子数据（`backend/src/main/resources/safelearn.sql` 及 `data/training_scenarios_cases.sql`）。

### 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`。

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:3000`，`/api` 请求自动代理到后端 8080 端口。

### 访问入口

| 端 | 地址 | 说明 |
|----|------|------|
| 学员端 | http://localhost:3000/user/login | 学员学习、训练、考试 |
| 管理端 | http://localhost:3000/login | 管理员运营治理 |

## 默认账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 管理端后台 |
| 培训学员 | zhanggong | admin123 | 演示用户「张工」，含学习进度 |
| 培训学员 | lisi | admin123 | 普通学员 |
| 管理人员 | wangwu | admin123 | 管理人员角色 |

其他账号可通过注册页面自行注册。

## 配置说明

### 数据库

修改 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/safelearn?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: "123456"
```

### AI 问答（可选）

配置环境变量 `DASHSCOPE_API_KEY` 以启用通义千问 AI 问答；未配置时 AI 相关功能可能不可用。

```bash
# Windows PowerShell
$env:DASHSCOPE_API_KEY="your-api-key"

# Linux / macOS
export DASHSCOPE_API_KEY="your-api-key"
```

也可在 `application.yml` 的 `app.ai` 节点中调整模型与接口地址。

## 相关文档

| 文档 | 说明 |
|------|------|
| [docs/运行条件与佐证材料.md](docs/运行条件与佐证材料.md) | 部署参数、演示流程、大赛佐证材料 |
| [docs/双端工作流分析与优化方案.md](docs/双端工作流分析与优化方案.md) | 学员端与管理端工作流、数据衔接 |
| [docs/user-portal-optimization.md](docs/user-portal-optimization.md) | 学员端优化记录 |
| [wechat-article-writer/README.md](wechat-article-writer/README.md) | 储安云公众号内容创作工具 |

## 构建部署

```bash
# 前端生产构建
cd frontend
npm run build
# 产物输出至 frontend/dist/

# 后端打包
cd backend
mvn package -DskipTests
# 产物输出至 backend/target/safelearn-backend-1.0.0.jar
```
