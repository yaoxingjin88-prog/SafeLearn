# SafeLearn - 储能电站安全培训与事故推演系统

储能电站安全培训与事故推演系统，为储能行业从业人员提供在线安全培训、事故案例分析、应急推演训练和 AI 智能问答的一体化平台。

## 技术栈

**前端**
- Vue 3 + TypeScript + Vite
- Element Plus + UnoCSS
- Three.js（3D 可视化）+ ECharts（数据图表）
- Pinia（状态管理）+ Vue Router

**后端**
- Spring Boot 3.3 + Java 17
- Spring Data JPA + MySQL
- Spring Security + JWT 认证

## 项目结构

```
SafeLearn/
├── frontend/                  # 前端 Vue 项目
│   ├── src/
│   │   ├── api/               # 接口请求
│   │   ├── components/        # 公共组件
│   │   ├── composables/       # 组合式函数
│   │   ├── layouts/           # 布局组件
│   │   ├── mock/              # Mock 数据
│   │   ├── pages/             # 页面
│   │   │   ├── admin/         # 管理后台（用户管理、数据大屏）
│   │   │   ├── ai/            # AI 安全问答
│   │   │   ├── auth/          # 登录注册
│   │   │   ├── cases/         # 事故案例
│   │   │   ├── courses/       # 安全培训课程
│   │   │   ├── dashboard/     # 工作台
│   │   │   ├── simulation/    # 事故推演
│   │   │   └── training/      # 应急训练
│   │   ├── router/            # 路由配置
│   │   ├── stores/            # Pinia 状态管理
│   │   └── types/             # TypeScript 类型定义
│   └── package.json
│
└── backend/                   # 后端 Spring Boot 项目
    └── src/main/java/com/safelearn/
        ├── common/            # 通用工具（响应封装、JWT、异常处理）
        ├── config/            # 配置（CORS、Security）
        ├── controller/        # 控制器
        ├── dto/               # 数据传输对象
        ├── entity/            # 实体类
        ├── filter/            # 过滤器（JWT 认证）
        ├── repository/        # 数据访问层
        └── service/           # 业务逻辑层
```

## 功能模块

| 模块 | 说明 |
|------|------|
| 工作台 | 学习数据概览、快捷入口 |
| 安全培训 | 课程列表、章节学习、学习进度跟踪 |
| 事故推演 | 推演场景选择、3D 可视化推演演示 |
| 应急训练 | 场景决策训练、训练报告生成 |
| 事故案例 | 案例列表、案例详情（时间线、原因分析、教训总结） |
| AI 安全问答 | 基于知识库的智能问答 |
| 系统管理 | 用户管理、数据大屏（仅管理员） |

## 快速开始

### 环境要求

- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Node.js 18+

### 数据库初始化

```sql
CREATE DATABASE safelearn DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

启动后端后会自动建表并初始化数据（`schema.sql` + `data.sql`）。

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

前端默认运行在 `http://localhost:3000`，API 请求自动代理到后端 8080 端口。

## 默认账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 系统管理后台 |
| 培训学员 | zhanggong | admin123 | 演示用户「张工」，含学习进度 |
| 培训学员 | lisi | admin123 | 普通学员 |
| 管理人员 | wangwu | admin123 | 管理人员角色 |

其他账号可通过注册页面自行注册。

## 数据库配置

修改 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/safelearn?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: "123456"
```
