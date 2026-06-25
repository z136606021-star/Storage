# AGENTS.md — 仓库管理系统

> **维护要求**：每次项目结构、技术栈、约定或功能范围发生变化时，代理必须同步更新本文件。

## 项目概述

- **名称**：仓库管理系统（Storage Management System）
- **上层平台**：项目生命周期管理系统（项目管理平台）
- **所属模块**：项目管理平台 → 资源管理 → 仓库管理
- **远端仓库**：https://github.com/z136606021-star/Storage.git
- **当前阶段**：物料台账三版 + 鉴权第二期 + 系统管理第四期 + 登录页第五期 + 第六期平台壳层 UI + 第七期壳层 UI 补全 + Worktree 数据库隔离 + **第八期 DevX**（`dev-up`、`health-check`、遗留 Docker 清理）

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3、TypeScript、Vite、Ant Design Vue 4、Vue Router、Axios |
| 后端 | Java 17+、Spring Boot 3.3、MyBatis Plus 3.5、Apache POI、Apache Shiro 2 |
| 数据库 | MySQL 8 |
| 对象存储 | MinIO |

## 平台总体架构

本仓库当前实现的是项目生命周期管理系统中的「资源管理 → 仓库管理」能力。新增功能时必须先判断其所属一级域与业务边界，避免把项目管理、采购、财务或系统管理逻辑混入仓库管理模块。

| 一级域 | 子模块/能力 | 当前仓库边界 |
|--------|-------------|--------------|
| 个人中心 | 快捷导航、项目进度统计、待办事项、特别注意事项、工作安排 | 暂不实现；仅在平台壳层导航中保留扩展入口 |
| 项目管理 | 新建项目、项目评估阶段、项目启动阶段、项目规划阶段、项目执行阶段、过程监控阶段、项目收尾阶段 | 暂不实现；仓库模块只消费项目/BOM/采购等上游结果，不承载项目流程编排 |
| 资源管理 | 采购管理、仓库管理、设计指引、技能中心、经验库、财务结算中心 | 当前只实现仓库管理；采购、设计、技能、经验、财务结算均作为后续独立子模块 |
| 系统管理 | 角色管理、用户管理、客户管理 | 用户/角色管理已实现；**客户管理占位页**已实现，业务 CRUD 待实现 |

### 项目生命周期主流程

项目管理域围绕项目从立项到收尾的生命周期展开：新建项目 → Study → DFA → Buyoff → Award → Design → Make → Install → Debug → Trial → Delivery。流程节点会产生设计方案、BOM 用量表、采购申请、物料入库、安装调试、验收与项目总结等业务动作。

仓库管理只承接与物料相关的资源流转：根据 BOM 或采购需求判断库存，库存不足时进入采购申请，物料到货后入库，并在后续出入库模块中记录项目领用、退库或调整。不得在仓库模块内硬编码项目生命周期审批、客户验收、项目进度更新等项目管理职责。

### 资源管理模块边界

- **采购管理**：采购需求申请、采购审批、采购清单、供应商/合同/采购周期等，后续独立实现。
- **仓库管理**：物料台账、物料出入库、安全库存管理、库存预警与库存统计；这是当前仓库的核心实现范围。
- **设计指引**：图纸/工艺/标准类资料沉淀，后续独立实现。
- **技能中心**：岗位技能、认证、培训或人员能力库，后续独立实现。
- **经验库**：项目复盘、问题案例、知识沉淀，后续独立实现。
- **财务结算中心**：成本分析、财务结算或付款管理，后续独立实现。

## 子系统范围

| 子系统 | 状态 | 路由 | 组件 | API |
|--------|------|------|------|-----|
| 登录页 | Shiro Session 鉴权 + 开放注册 + **第五期 UI/UX 优化** | `/login`、`/login?tab=register` | `LoginView.vue`、`utils/loginRemember.ts` | `POST /api/auth/login`、`POST /api/auth/register`、`POST /api/auth/logout`、`GET /api/auth/me` |
| 系统管理 | 用户管理（含角色子 Tab）+ 客户占位 + Excel 导入导出已实现；菜单管理 UI 已移除 | `/system/users`、`/system/users/roles`、`/system/customers` | `SystemManageLayout.vue`、`UserManageView.vue`、`RoleManagePanel.vue`、`CustomerManageView.vue` | 用户：`GET/POST/PUT/DELETE /api/system/users`、`GET .../permissions`、`GET .../export`、`POST .../import`；角色：`GET/POST/PUT/DELETE /api/system/roles`、`GET .../export`、`POST .../import`；菜单树 API 保留供角色授权：`GET /api/system/menus/tree`；导航：`GET /api/menus/nav-tree` |
| 文件上传（MinIO） | 基础设施 + 通用上传 API 已实现 | — | — | `POST /api/files/upload` |
| 物料台账 | CRUD + 导入 + 批量操作 + 筛选联动已实现 | `/warehouse/material-ledger` | `MaterialLedgerView.vue`、`MaterialLedgerFormModal.vue` | `GET/POST/PUT/DELETE /api/materials`、`DELETE /api/materials/batch`、`GET /api/materials/filter-options`、`GET /api/materials/{id}`、`GET /api/materials/export`、`GET /api/materials/import-template`、`POST /api/materials/import` |
| 物料出入库 | **占位页**（业务待实现） | `/warehouse/material-io` | `MaterialIoPlaceholderView.vue` | — |
| 安全库存管理 | **占位页**（业务待实现） | `/warehouse/safety-stock` | `SafetyStockPlaceholderView.vue` | — |
| 配置管理 | **占位页**（Bin位 / 物料清单业务待实现） | `/warehouse/config/bin`、`/warehouse/config/bom` | `BinManagePlaceholderView.vue`、`BomManagePlaceholderView.vue` | — |
| 平台壳层 | **占位页**（个人中心/项目/采购/设计/技能/经验/财务） | `/platform/*` | `ShellPlaceholderView.vue` | — |

### 平台基础能力

- **鉴权**：Apache Shiro Session + Cookie；`sys_user/role/menu` 表；开放注册默认 `USER` 角色（只读物料台账）；`composables/useAuth.ts`（含 `hasPermission`）+ `router/guards.ts`（含 `meta.permission`）+ `http` 拦截器。
- **动态菜单**：`GET /api/menus/nav-tree` 按权限过滤；[SideMenu.vue](frontend/src/components/layout/SideMenu.vue) 从 API 加载；ADMIN 可见完整平台壳层，仓库管理下 4 项（台账 / 出入库 / 安全库存 / 配置管理 → Bin位、物料清单）；默认展开资源管理 → 仓库管理。
- **动态 TabBar**：[useWorkbenchTabs.ts](frontend/src/composables/useWorkbenchTabs.ts) + [TabBar.vue](frontend/src/components/layout/TabBar.vue)；ADMIN 登录预置「个人中心」「项目中心」，访问业务页自动追加 Tab，可切换/关闭；USER 仅预置物料台账 Tab。
- **壳层路由注册表**：[shellRouteRegistry.ts](frontend/src/constants/shellRouteRegistry.ts) 与 `migration-phase7-ui-shell-paths.sql` 对齐 DB `sys_menu.path`。
- **对象存储**：MinIO（`docker-compose`）；`MinioStorageService` + `POST /api/files/upload`（本期未接入业务页面）。
- **路由守卫**：`requiresAuth` 业务路由未登录时重定向 `/login`；登录页静态资源见 `frontend/src/assets/auth/`；**记住密码**用 `utils/loginRemember.ts` 仅存账号至 localStorage（不传 Shiro RememberMe）；Tab 可通过 `?tab=register` 切换。

### 物料台账字段

筛选：品类、统称、品牌、名称（关键字）、型号、Bin位；支持品类→统称→品牌联动收窄选项。表格：序号、品类、统称、品牌、名称、型号、Bin位、库存数量、单价、备注、操作（查看/编辑/删除）。导出按当前筛选条件导出全部匹配记录；勾选行支持批量导出与批量删除；支持 Excel 导入与模板下载。查看详情为右侧只读抽屉。新增/编辑通过弹窗维护主数据，**库存数量只读**（新建默认 0，变更走后续出入库模块）。

## 仓库约定

- **默认分支**：`main`
- **提交规范**：Conventional Commits（`feat:`、`fix:`、`chore:`）
- **忽略文件**：见 [.gitignore](.gitignore)
- **环境变量**：参考 [.env.example](.env.example)，勿提交 `.env`

## 代理工作指引

1. **更新本文件**：新增模块、路由、API、技术栈或目录结构时同步更新
2. **子系统实现**：新页面实现后更新「子系统范围」表
3. **不提交敏感信息**：数据库密码、API Key 等不入库
4. **保持 README 同步**：启动方式变化时更新 [README.md](README.md)

## 模块复用与可维护性门禁

> **强制要求**：代理在新增页面、API、表结构或业务逻辑前，必须先完成复用检查并优先使用现有公共模块，禁止为赶进度重复造轮子。

### 开发前检查清单

实现任何新功能前，必须先检索以下目录是否已有可复用能力：

| 层级 | 检索路径 | 典型复用物 |
|------|----------|------------|
| 前端组件 | `frontend/src/components/`、`frontend/src/layouts/` | 布局壳层、筛选区、表格包装、弹窗/抽屉 |
| 前端 API | `frontend/src/api/`、`frontend/src/types/` | Axios 封装、分页类型、筛选参数、实体类型 |
| 后端通用 | `backend/.../config/`、`backend/.../exception/`、`backend/.../dto/` | CORS、全局异常、分页响应、校验 DTO |
| 后端业务 | `backend/.../service/`、`backend/.../mapper/` | 分页查询、导入导出、批量操作 |
| 数据库 | `backend/src/main/resources/db/` | 表命名、字段命名、种子/迁移脚本 |
| 文档 | `AGENTS.md`、`README.md` | 子系统范围、API 列表、目录结构 |

### 分层职责

- **页面（views）**：只做路由入口、状态编排与组件组合，不写大段重复 UI 或请求逻辑。
- **组件（components）**：可复用 UI 必须下沉到 `components/common/` 或 `components/warehouse/`，禁止在多个 view 内复制粘贴相同模板。
- **API 层（api + types）**：每个资源一个模块文件；分页、筛选、导出等模式保持统一签名，不各写一套。
- **后端 Service**：分页、异常处理、导入导出、DTO 转换优先抽共享 helper 或基类，禁止每个 Controller 复制相同样板代码。
- **数据库**：相同业务含义只用一套字段名（如 `generic_name`、`bin_location`）；状态/类型优先字典表或统一枚举，避免子系统各搞一套。

### 禁止事项

- 禁止在未检查现有代码的情况下新建功能相近的组件、Service、DTO 或 API 文件。
- 禁止把通用逻辑长期留在某个子系统 view 或 Controller 内。
- 禁止为单个页面硬编码本应从 `filter-options` 或字典接口获取的下拉数据。
- 禁止复制粘贴超过 30 行且结构相同的代码而不抽象；若确不抽象，须在 PR/说明中写明理由。

### 代理执行要求

1. **先说明复用结论**：开始编码前，用一两句话说明「复用了哪些现有模块」或「为何需要新建模块」。
2. **新建公共模块时同步文档**：在「目录结构」与本文件记录其用途、边界、调用方。
3. **Worktree 并行开发**：公共能力优先合入 `main`，各功能分支通过 `git merge origin/main` 同步，不在多个分支各自维护一份相同公共代码。当前 worktree：`main`（`E:/Storage`）、`feat/material-ledger`（`E:/Storage-worktrees/material-ledger`）、`feat/material-io`、`feat/safety-stock`、`feat/config-mgmt`（`E:/Storage-worktrees/config-mgmt`）。**切换 worktree 或分支后必须先执行 `scripts/sync-worktree-env.ps1`**，确认 MySQL 端口与当前分支一致后再启 Docker / 后端。
4. **重构优先于堆叠**：发现重复实现时，优先抽取再扩展，而非再写第三份副本。

### Worktree 数据库隔离

各 worktree 拥有**独立的 MySQL / MinIO 端口、容器名与 Docker 数据卷**；逻辑库名均为 `storage`，隔离靠端口 + 卷。注册表 SSOT：[`scripts/worktree-db.ps1`](scripts/worktree-db.ps1)。

| 分支 | Worktree | MySQL | MinIO API | Compose 项目 |
|------|----------|-------|-----------|--------------|
| `main` | `E:/Storage` | 3307 | 9000 | `storage-main` |
| `feat/material-ledger` | `E:/Storage-worktrees/material-ledger` | 3308 | 9010 | `storage-material-ledger` |
| `feat/material-io` | `E:/Storage-worktrees/material-io` | 3309 | 9020 | `storage-material-io` |
| `feat/safety-stock` | `E:/Storage-worktrees/safety-stock` | 3310 | 9030 | `storage-safety-stock` |
| `feat/config-mgmt` | `E:/Storage-worktrees/config-mgmt` | 3311 | 9040 | `storage-config-mgmt` |

#### 日常流程

```powershell
cd E:\Storage-worktrees\material-io
git checkout feat/material-io
.\scripts\dev-up.ps1                      # 推荐：一键 sync + docker + 前后端
# 或分步：
.\scripts\sync-worktree-env.ps1
docker compose --env-file .env up -d
.\scripts\start-dev.ps1
```

`start-dev.ps1` / `reset-db.ps1` 启动时会自动 sync；手动改分支后建议显式执行一次 `sync-worktree-env.ps1`。

#### 隔离原则

- **代码在 Git 里合并，数据库永不合并**；每个 worktree 对应独立 Docker 卷，互不可见。
- **在哪个分支写代码，就用哪个分支的 `.env` 与端口**；禁止用 main 的 3307 卷测试 feature 分支代码。
- **禁止**在未确认目录/分支时对 `docker compose down -v`（会删错卷）。
- **禁止**把 A 分支 mysqldump 导入 B 分支端口（除非明确在做数据迁移）。
- `.env` 不入库；端口分配只改 `worktree-db.ps1` 一处。

#### Git 合并时（不能弄混）

| 场景 | 正确做法 |
|------|----------|
| feature 新增 `migration-*.sql` | 脚本保持幂等（`IF NOT EXISTS` / `INSERT IGNORE` / UPDATE 修复中文）；合并后各 worktree **各自重启后端**，迁移只作用于**本卷** |
| 修改 `schema.sql` 种子 | 只影响**新初始化**的空卷；已有卷靠 migration 或 `reset-db.ps1` |
| `main` 合并进 feature | 先 `git merge`，再 `sync-worktree-env`，再启后端；不要用 main 的 Docker 卷测 feature |
| PR / 代理说明 | 若变更 DB 结构，注明「各 worktree 需重启后端；是否需 reset-db 由开发者自判」 |
| 代理执行 | 在 feature worktree 开发时连接该分支注册端口；合入 main 时在 **main 目录**验证，不跨端口读库 |

#### 一次性迁移说明

从旧版共用 `material-ledger-*` 容器 / `storage_mysql_data` 卷迁移时：运行 `scripts/cleanup-legacy-docker.ps1`，再执行 `sync-worktree-env` + `docker compose --env-file .env up -d` 创建 `storage-{slug}_*` 新卷。若需保留 main 现有数据，迁移前先 `mysqldump` 备份再导入新 `storage-main-mysql` 容器。

### 第八期 DevX

日常开发优先使用 [`scripts/dev-up.ps1`](scripts/dev-up.ps1) / [`dev-up.cmd`](dev-up.cmd)（sync + Docker + wait MySQL + start-dev）。

| 脚本 | 用途 |
|------|------|
| `dev-up.ps1` | 一键启动完整开发环境 |
| `health-check.ps1` | 只读自检（分支、.env、容器、中文、前后端）；退出码 0/1 |
| `cleanup-legacy-docker.ps1` | 清理第七期前 `material-ledger-*` 遗留容器 |
| `wait-mysql.ps1` | 轮询 MySQL 端口与 `SELECT 1`，供 dev-up/reset-db 复用 |
| `sync-worktree-env.ps1` | 按分支生成本地 `.env` |

**代理执行要求（DevX）**：

- 切换 worktree 或启动开发环境前，建议先跑 `health-check.ps1`；失败时先排查，**不要**擅自 `docker compose down -v`。
- 物料台账中文乱码：重启后端触发 `migration-fix-chinese-data.sql`（含 `material_ledger` 幂等 UPDATE）；仍异常再用 `reset-db.ps1`。
- 遇端口占用且存在 `material-ledger-*`：先 `cleanup-legacy-docker.ps1`，再 `dev-up`。

## 目录结构

```
Storage/
├── frontend/                 # Vue3 + TS + Ant Design Vue 4
│   └── src/
│       ├── api/
│       │   ├── http.ts           # 共享 axios（withCredentials + 401 拦截）
│       │   ├── auth.ts           # 登录/注册/登出/当前用户
│       │   ├── menu.ts           # 导航树
│       │   ├── system/           # 用户/角色管理 API
│       │   └── materialLedger.ts # 物料台账 API
│       ├── composables/
│       │   ├── useAuth.ts        # 登录态 composable（hasPermission）
│       │   └── useWorkbenchTabs.ts # 顶部 Tab 状态
│       ├── components/
│       │   ├── layout/           # SideMenu（动态菜单）、TabBar
│       │   ├── common/           # ComingSoonPage 占位页
│       │   ├── system/           # RoleManagePanel
│       │   └── warehouse/        # MaterialLedgerFormModal
│       ├── constants/
│       │   ├── filter.ts         # 筛选常量（ALL_OPTION）
│       │   └── shellRouteRegistry.ts # 壳层占位路由 SSOT
│       ├── assets/auth/          # 登录页静态资源
│       ├── layouts/AppLayout.vue
│       ├── router/
│       │   ├── index.ts          # createRouter + 注册守卫
│       │   ├── routes.ts         # 路由表（含 system/*）
│       │   ├── guards.ts         # beforeEach 登录与 permission 拦截
│       │   └── meta.d.ts         # RouteMeta 类型扩展
│       ├── types/
│       │   ├── common.ts         # PageResult 等通用类型
│       │   ├── auth.ts
│       │   ├── system.ts
│       │   └── materialLedger.ts
│       ├── utils/                # download、format、selectOptions、icons、loginRemember
│       ├── views/auth/
│       │   └── LoginView.vue     # 登录/注册
│       ├── views/system/         # SystemManageLayout、UserManageView、CustomerManageView
│       ├── views/platform/       # ShellPlaceholderView
│       ├── views/warehouse/      # 出入库/安全库存/配置管理占位页
│       └── views/material-ledger/
│           └── MaterialLedgerView.vue
├── backend/                  # Spring Boot 3 + MyBatis Plus
│   └── src/main/java/com/storage/
│       ├── controller/       # Auth、System、Material、File、MenuNav
│       ├── config/           # CORS、Shiro、MinIO、WebMvc
│       ├── shiro/            # Realm、ShiroConfig、SubjectBindingFilter
│       ├── converter/        # DTO ↔ Entity 转换
│       ├── dto/
│       ├── entity/
│       ├── excel/            # Excel 列契约与 POI 工具
│       ├── exception/        # 全局异常（可复用）
│       ├── mapper/
│       ├── query/            # 查询条件构建
│       ├── service/
│       └── web/              # Excel 响应构建
├── docker-compose.yml        # MySQL 8 + MinIO（端口/卷由 .env 参数化，含 healthcheck）
├── dev-up.cmd                # 一键环境 + 前后端（第八期推荐入口）
├── scripts/
│   ├── worktree-db.ps1       # Worktree 分支→端口/容器/卷注册表（SSOT）
│   ├── sync-worktree-env.ps1 # 按当前分支生成本地 .env
│   ├── dev-up.ps1            # sync + docker + wait-mysql + start-dev
│   ├── health-check.ps1      # 开发环境只读自检
│   ├── cleanup-legacy-docker.ps1 # 清理 material-ledger-* 遗留容器
│   ├── wait-mysql.ps1        # MySQL 就绪轮询
│   ├── start-dev.ps1         # 启动前后端（自动 sync + 注入 DB 环境变量）
│   └── reset-db.ps1          # 重置当前 worktree 的 Docker 卷
├── .env.example
├── AGENTS.md
└── README.md
```

## 变更日志

| 日期 | 变更 |
|------|------|
| 2026-06-23 | 初始化 Git 仓库、.gitignore、AGENTS.md、README.md；首次推送到 GitHub |
| 2026-06-23 | 物料台账首版：前后端工程、MySQL 种子数据、完整壳层 UI、分页查询 API |
| 2026-06-23 | 物料台账二版：查看详情抽屉、按筛选条件导出 Excel、列表页 UI 优化 |
| 2026-06-23 | 物料台账三版：CRUD、Excel 导入、批量导出/删除、筛选联动、种子数据精简 |
| 2026-06-23 | 新增模块复用与可维护性门禁；main 合并物料台账 v2/v3 并同步至功能分支 |
| 2026-06-23 | 复用基础层：前端 http/types/utils/constants、后端 converter/query/excel/web |
| 2026-06-24 | 补充项目生命周期管理系统总体架构、一级域划分与仓库管理边界 |
| 2026-06-24 | 登录页 UI 第一期：`/login` 独立页、设计稿还原、路由默认入口 |
| 2026-06-24 | 鉴权第二期：Apache Shiro Session、用户/角色/菜单表、API 保护、前端登录打通 |
| 2026-06-24 | MinIO 对象存储基础设施与 `POST /api/files/upload` |
| 2026-06-24 | 系统管理第四期：用户/角色嵌套路由 Tab、授权只读面板、用户/角色 Excel、客户管理占位、移除菜单管理 UI |
| 2026-06-24 | 登录页第五期：左栏科技插画、localStorage 记住账号、URL Tab 同步、注册 3-32/密码≥6 校验、登录交互优化 |
| 2026-06-24 | 第六期平台壳层 UI：`migration-phase6-platform-shell.sql` 完整导航种子、仓库 4 项（配置管理含 Bin位/物料清单）、占位路由与 `ComingSoonPage`、新建 `feat/config-mgmt` worktree |
| 2026-06-24 | 第七期壳层 UI 补全：动态 TabBar（`useWorkbenchTabs`）、壳层 `/platform/*` 占位路由、`migration-phase7-ui-shell-paths.sql`、侧栏默认展开 |
| 2026-06-25 | Worktree 数据库隔离：`worktree-db.ps1` 五分支独立端口/卷、`sync-worktree-env.ps1`、参数化 `docker-compose.yml`、AGENTS 合并规范 |
| 2026-06-25 | 第八期 DevX：`dev-up`、`health-check`、`cleanup-legacy-docker`、`wait-mysql`、`material_ledger` 中文修复、MySQL healthcheck |
