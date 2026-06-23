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

## 目录结构

```
Storage/
├── frontend/                 # Vue3 + TS + Ant Design Vue 4
│   └── src/
│       ├── api/materialLedger.ts
│       ├── components/layout/  # SideMenu, TabBar
│       ├── layouts/AppLayout.vue
│       ├── router/index.ts
│       ├── types/materialLedger.ts
│       └── views/material-ledger/
│           ├── MaterialLedgerView.vue
│           └── MaterialLedgerFormModal.vue
├── backend/                  # Spring Boot 3 + MyBatis Plus
│   └── src/main/java/com/storage/
│       ├── controller/MaterialLedgerController.java
│       ├── entity/MaterialLedger.java
│       ├── exception/GlobalExceptionHandler.java
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
