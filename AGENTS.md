# AGENTS.md — 仓库管理系统

> **维护要求**：每次项目结构、技术栈、约定或功能范围发生变化时，代理必须同步更新本文件。

## 项目概述

- **名称**：仓库管理系统（Storage Management System）
- **所属平台**：项目管理平台 → 资源管理 → 仓库管理
- **远端仓库**：https://github.com/z136606021-star/Storage.git
- **当前阶段**：物料台账三版（CRUD + 导入 + 批量操作 + 筛选联动）已实现

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3、TypeScript、Vite、Ant Design Vue 4、Vue Router、Axios |
| 后端 | Java 17+、Spring Boot 3.3、MyBatis Plus 3.5、Apache POI |
| 数据库 | MySQL 8 |

## 子系统范围

| 子系统 | 状态 | 路由 | 组件 | API |
|--------|------|------|------|-----|
| 物料台账 | CRUD + 导入 + 批量操作 + 筛选联动已实现 | `/warehouse/material-ledger` | `MaterialLedgerView.vue`、`MaterialLedgerFormModal.vue` | `GET/POST/PUT/DELETE /api/materials`、`DELETE /api/materials/batch`、`GET /api/materials/filter-options`、`GET /api/materials/{id}`、`GET /api/materials/export`、`GET /api/materials/import-template`、`POST /api/materials/import` |
| 物料出入库 | 待实现 | — | — | — |
| 安全库存管理 | 待实现 | — | — | — |

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
3. **Worktree 并行开发**：公共能力优先合入 `main`，各功能分支通过 `git merge origin/main` 同步，不在多个分支各自维护一份相同公共代码。
4. **重构优先于堆叠**：发现重复实现时，优先抽取再扩展，而非再写第三份副本。

## 目录结构

```
Storage/
├── frontend/                 # Vue3 + TS + Ant Design Vue 4
│   └── src/
│       ├── api/                # 按资源拆分的 API 客户端
│       ├── components/
│       │   ├── layout/         # SideMenu, TabBar（平台壳层）
│       │   ├── common/         # 跨子系统通用组件（待抽取）
│       │   └── warehouse/      # 仓库模块通用组件（待抽取）
│       ├── layouts/AppLayout.vue
│       ├── router/index.ts
│       ├── types/materialLedger.ts
│       └── views/material-ledger/
│           ├── MaterialLedgerView.vue
│           └── MaterialLedgerFormModal.vue
├── backend/                  # Spring Boot 3 + MyBatis Plus
│   └── src/main/java/com/storage/
│       ├── controller/
│       ├── dto/
│       ├── entity/
│       ├── exception/        # 全局异常（可复用）
│       ├── mapper/MaterialLedgerMapper.java
│       └── service/
│           ├── MaterialLedgerService.java
│           ├── MaterialLedgerExportService.java
│           └── MaterialLedgerImportService.java
├── docker-compose.yml        # MySQL 8 本地开发
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
