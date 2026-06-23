# 仓库管理系统

项目管理平台中 **资源管理 → 仓库管理** 模块，包含物料台账、物料出入库、安全库存管理三个子系统。

## 技术栈

- **前端**：Vue 3 + TypeScript + Vite + Ant Design Vue 4
- **后端**：Java 17+ + Spring Boot 3 + MyBatis Plus
- **数据库**：MySQL 8

## 快速启动

### 1. 启动 MySQL

需已安装 Docker。在项目根目录执行：

```bash
docker compose up -d
```

将自动创建 `storage` 数据库并导入 [backend/src/main/resources/db/schema.sql](backend/src/main/resources/db/schema.sql) 中的表结构与种子数据（约 15 条）。

**Git worktree 说明**：本目录 MySQL 使用 **3307** 端口（容器名 `material-ledger-mysql`），与主仓库 `Storage` 的 3306 端口互不干扰。后端默认连接 `localhost:3307`，见 [.env.example](.env.example)。

若修改了种子数据或表结构，需重建数据库卷后重新导入：

```bash
docker compose down -v
docker compose up -d
```

Windows 也可执行 [scripts/reset-db.ps1](scripts/reset-db.ps1)。

默认连接信息见 [.env.example](.env.example)（worktree 使用 **3307** 端口）：

- Host: `localhost:3307`（worktree 独立 MySQL，勿与主仓库 3306 混用）
- Database: `storage`
- User / Password: `storage` / `storage123`

### 2. 启动后端

需 Java 17+ 与 Maven。在 `backend` 目录：

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`

### 3. 启动前端

需 Node.js 18+。在 `frontend` 目录：

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`，开发环境通过 Vite 代理将 `/api` 转发至后端。

## 当前功能

- [x] 完整平台壳层（侧栏导航 + 顶部页签）
- [x] 物料台账列表页（筛选、分页、行选择）
- [x] 后端分页查询 API 与筛选选项 API
- [x] 物料台账查看详情（右侧抽屉）
- [x] 物料台账导出 Excel（按当前筛选条件导出全部结果）
- [x] 物料台账 CRUD（新增/编辑/删除，库存数量只读）
- [x] 物料台账 Excel 导入与批量导出/批量删除
- [x] 物料台账筛选联动（品类 → 统称 → 品牌 → 型号/Bin位）
- [x] 公共复用基础层（前端 http/types/utils、后端 converter/query/excel/web）
- [ ] 物料出入库、安全库存管理

## 远端仓库

https://github.com/z136606021-star/Storage.git

协作者与 AI 代理请参阅 [AGENTS.md](AGENTS.md)。新增功能前须按 `AGENTS.md` 的「模块复用与可维护性门禁」检查并优先复用 `api/http.ts`、`types/common.ts`、`utils/`、`converter/`、`query/`、`excel/` 等公共层。
