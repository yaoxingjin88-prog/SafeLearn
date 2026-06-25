# 管理端顶部栏功能开发方案

> 文档说明：梳理管理端 Header 右侧四个入口（通知、消息、帮助、用户）的现状、目标与分阶段开发路径，便于后续迭代优化。  
> 关联文件：`frontend/src/layouts/MainLayout.vue`、`backend/.../AdminDashboardService.java`

---

## 1. 功能范围

| 入口 | 图标 | 当前 UI | 业务定位 |
|------|------|---------|----------|
| 通知 | Bell | 角标写死 `12` | 需管理员**处理**的预警（考试异常、进度滞后、证书到期等） |
| 消息 | Message | 角标写死 `5` | 平台**告知**类信息（公告、培训通知、发布提醒等） |
| 帮助 | QuestionFilled | 无点击逻辑 | 操作指引、FAQ、模块说明 |
| 用户 | Avatar + 用户名 | 已接账号设置/退出 | 身份展示、账号管理、退出登录 |

---

## 2. 现状分析

### 2.1 前端（MainLayout）

路径：`frontend/src/layouts/MainLayout.vue`

- 通知、消息按钮为静态 DOM，数字 `12`、`5` 硬编码，无 Popover / Drawer
- 帮助按钮无 `@click` 事件
- 用户区已通过 `useUserStore` 接入，支持跳转 `/admin/account` 与退出登录
- 侧栏底部也有用户下拉，与 Header 用户区功能部分重复

### 2.2 后端（可复用数据）

路径：`backend/src/main/java/com/safelearn/service/AdminDashboardService.java`

| 方法 | 用途 | 是否可直接复用 |
|------|------|----------------|
| `buildAlerts()` | 考试未通过、培训进度低、训练低分、证书即将到期等 | ✅ 可作为「通知」数据源 |
| `buildAnnouncements()` | 系统公告 + 课程上线通知 | ✅ 可作为「消息」数据源 |
| `systemConfig.getString("dashboard.announcement")` | 首页公告文案 | ✅ 可并入消息 |

首页驾驶舱已在消费上述数据：

- `frontend/src/pages/dashboard/DashboardPage.vue` → `dashboard.alerts`、`dashboard.announcements`
- 快捷入口「预警管理」锚点 `#alerts`

**结论**：Phase 1 无需新建数据库表，可先聚合现有 Dashboard 逻辑供 Header 使用。

### 2.3 用户认证（已完成）

- Store：`frontend/src/stores/user.ts`
- 接口：`/api/auth/login`、`/api/auth/userinfo`
- 管理端路由守卫依赖 `userStore.role === 'admin'`

---

## 3. 业务定义（通知 vs 消息）

| 维度 | 通知（Notification） | 消息（Message） |
|------|----------------------|-----------------|
| 性质 | 异常 / 预警 / 待办 | 公告 / 通知 / 告知 |
| 用户动作 | 需查看并可能跳转处理 | 阅读即可 |
| 典型来源 | 考试未通过、进度 < 40%、证书 30 天内到期 | 平台公告、新课程上线、考试发布 |
| 首页对应 | 预警面板 `#alerts` | 公告列表 `announcements` |
| 角标策略 | 未读预警数 | 未读公告数 |

---

## 4. 推荐开发工作流（与项目一致）

本项目模块（课程 / 题库 / 组卷 / 监控）通用节奏：

```
业务定义 → Entity + Repository + Service + AdminController
         → frontend admin.ts 类型与 API
         → 页面 / 组件
         → MainLayout 或业务页联动
```

Header 功能建议同样遵循，分三阶段推进。

---

## 5. 分阶段实施方案

### Phase 1 — 快速可用（建议优先）

**目标**：去掉硬编码角标，Header 可点开查看列表，与首页数据一致。

#### 5.1 后端

新增轻量聚合服务（可不建表）：

| 接口 | 说明 |
|------|------|
| `GET /api/admin/notifications/summary` | 返回 `{ items[], unreadCount }`，内部调用 `buildAlerts()` |
| `GET /api/admin/messages/summary` | 返回 `{ items[], unreadCount }`，内部调用 `buildAnnouncements()` |

可选：为每条 item 生成稳定 `id`（如 `alert:exam-fail`、`notice:course-{courseId}`），便于前端已读标记。

#### 5.2 前端

| 文件 | 职责 |
|------|------|
| `frontend/src/api/admin.ts` | 新增 `getNotificationSummary`、`getMessageSummary` |
| `frontend/src/composables/useAdminInbox.ts` | 拉取 summary、维护已读、计算角标 |
| `frontend/src/components/admin/AdminNotificationPopover.vue` | 铃铛 + 列表 + 「全部已读」 |
| `frontend/src/components/admin/AdminMessagePopover.vue` | 信封 + 列表 |
| `frontend/src/components/admin/AdminHelpDrawer.vue` | 帮助抽屉（纯前端） |
| `frontend/src/layouts/MainLayout.vue` | 替换静态按钮，挂载上述组件 |

#### 5.3 已读策略（Phase 1）

- 使用 `localStorage` 存储已读 ID 列表（按用户维度 key）
- `unreadCount = items.length - readIds.length`
- 不依赖后端持久化，适合单管理员 / 演示环境

#### 5.4 跳转联动

| 通知类型 | 建议跳转 |
|----------|----------|
| 考试异常 | `/admin/learning/exams` 或 `/dashboard#alerts` |
| 培训进度 | `/admin/learning/monitoring` |
| 证书到期 | 学员证书管理（若后续有管理页）或 Dashboard |

#### 5.5 帮助（Phase 1 纯前端）

- 点击 `?` 打开 `el-drawer`
- 按当前 `route.path` 展示模块说明（题库 / 组卷 / 监控 / 考试等）
- 附加通用 FAQ：登录、权限、发布流程
- 无需后端接口

**Phase 1 预估工作量**：1～2 天（含联调）

---

### Phase 2 — 业务写入与闭环

**目标**：关键操作自动产生通知/消息，而不只依赖 Dashboard 聚合。

#### 2.1 建议在哪些节点写入

| 业务动作 | 类型 | 示例文案 |
|----------|------|----------|
| 组卷发布 | 消息 + 通知 | 「XXX 考试已发布，共 88 题」 |
| 课程发布 | 消息 | 「课程《XXX》已上线」 |
| 学习监控超阈值 | 通知 | 「3 名学员进度低于 40%」 |
| 考试批量未通过 | 通知 | 「本月 5 人次考试未通过」 |

#### 2.2 后端

- 新增 `AdminNotificationService.create(...)`
- 在 `AdminPaperAssemblyService.publishPaper`、`AdminCourseService` 发布等位置调用
- 仍可使用内存聚合 + DB 表混合；或直接引入表（见 Phase 3）

---

### Phase 3 — 独立 Inbox 体系

**目标**：多管理员、跨设备已读一致，支持筛选与历史。

#### 3.1 数据表（建议）

**admin_notifications**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | CHAR(36) | 主键 |
| user_id | CHAR(36) | 接收人，空表示全员 |
| type | VARCHAR(32) | exam / progress / certificate / system |
| level | VARCHAR(16) | danger / warning / info |
| title | VARCHAR(200) | 标题 |
| content | TEXT | 内容 |
| link | VARCHAR(500) | 跳转路径 |
| read_at | DATETIME | 已读时间 |
| created_at | DATETIME | 创建时间 |

**admin_messages**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | CHAR(36) | 主键 |
| sender_id | CHAR(36) | 发送方 |
| receiver_id | CHAR(36) | 接收方 |
| type | VARCHAR(32) | announcement / training / exam |
| title | VARCHAR(200) | 标题 |
| body | TEXT | 正文 |
| read_at | DATETIME | 已读时间 |
| created_at | DATETIME | 创建时间 |

#### 3.2 接口扩展

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/notifications` | 分页列表 |
| PUT | `/api/admin/notifications/{id}/read` | 单条已读 |
| PUT | `/api/admin/notifications/read-all` | 全部已读 |
| GET | `/api/admin/messages` | 分页列表 |
| PUT | `/api/admin/messages/{id}/read` | 单条已读 |

#### 3.3 实时性（可选）

- 默认：Header 每 60s 轮询 `summary`
- 进阶：SSE / WebSocket（项目 AI 模块已有 SSE 可参考）

---

## 6. 用户区优化项（低优先级）

| 项 | 现状 | 建议 |
|----|------|------|
| 展示名 | `displayName` from userStore | 保持 |
| 角色标签 | 无 | 可加「管理员」小标签 |
| 部门 | 侧栏有 `displayDepartment` | Header 用户名旁可展示 |
| 菜单重复 | 侧栏 + Header 均有用户下拉 | 可统一交互，减少重复入口 |

无需新增后端接口。

---

## 7. 目录结构建议

```text
frontend/src/
├── api/admin.ts
├── composables/useAdminInbox.ts
├── components/admin/
│   ├── AdminNotificationPopover.vue
│   ├── AdminMessagePopover.vue
│   └── AdminHelpDrawer.vue
└── layouts/MainLayout.vue

backend/src/main/java/com/safelearn/
├── service/AdminInboxService.java          # Phase 1 聚合
├── service/AdminNotificationService.java   # Phase 2/3
├── entity/AdminNotification.java           # Phase 3
├── entity/AdminMessage.java                # Phase 3
└── controller/AdminController.java
```

---

## 8. 实施优先级

| 优先级 | 任务 | 说明 |
|--------|------|------|
| P0 | 通知 / 消息 Popover + summary API | 去除假数字，与 Dashboard 同源 |
| P0 | 帮助 Drawer | 纯前端，快速提升可用性 |
| P1 | 组卷 / 课程 / 监控发布写通知 | 业务闭环 |
| P2 | 独立表 + 服务端已读 | 多管理员场景 |
| P3 | 实时推送 | 非必须 |

---

## 9. 与现有模块关系图

```text
                    ┌─────────────────────┐
                    │   MainLayout Header  │
                    │  通知 | 消息 | 帮助   │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
     AdminInboxService   AdminHelpDrawer    useUserStore
              │           (纯前端)
    ┌─────────┴─────────┐
    ▼                   ▼
buildAlerts()    buildAnnouncements()
    │                   │
    ▼                   ▼
考试/进度/证书      公告/课程上线
    │                   │
    └─────────┬─────────┘
              ▼
      DashboardPage（首页同源展示）
```

---

## 10. 后续优化备忘（待补充）

- [ ] 通知 / 消息 UI 稿细化（Popover 宽度、空状态、加载态）
- [ ] 已读策略最终选型：localStorage vs 服务端
- [ ] 是否单独做「通知中心 / 消息中心」全页
- [ ] 学员端是否也需要对称 Inbox
- [ ] 帮助内容维护方式：静态 MD vs 后台配置
- [ ] 权限：不同角色可见通知范围
- [ ] 国际化（若需要）

---

## 11. 变更记录

| 日期 | 说明 |
|------|------|
| 2026-06-23 | 初稿：基于 MainLayout 现状与 AdminDashboardService 可复用能力整理 |
