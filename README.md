# 仓库管理系统

项目管理平台中 **资源管理 → 仓库管理** 模块，包含物料台账、物料出入库、安全库存管理与配置管理（Bin位、物料清单）子系统。

## 技术栈

- **前端**：Vue 3 + TypeScript + Vite + Ant Design Vue 4
- **后端**：Java 17+ + Spring Boot 3 + MyBatis Plus + Apache Shiro
- **数据库**：MySQL 8
- **对象存储**：MinIO

## 快速启动

### 1. 启动 MySQL 与 MinIO

需已安装 Docker。在项目根目录执行：

```bash
docker compose up -d
```

将自动创建 `storage` 数据库并导入 [backend/src/main/resources/db/schema.sql](backend/src/main/resources/db/schema.sql) 中的表结构与种子数据。

**Git worktree 说明**：本目录 MySQL 使用 **3307** 端口（容器名 `material-ledger-mysql`），MinIO API **9000**、控制台 **9001**，与主仓库互不干扰。并行 worktree：`E:/Storage`（main）、`E:/Storage-worktrees/material-io`、`safety-stock`、`config-mgmt`。

若修改了种子数据或表结构（如鉴权表），需重建数据库卷后重新导入：

```bash
docker compose down -v
docker compose up -d
```

已有物料数据、仅需补齐鉴权/系统管理表时，后端启动时会自动执行 `db/migration-*.sql`（`CREATE/ALTER IF NOT EXISTS` + `INSERT IGNORE` + 中文数据修复）。迁移脚本须为 **UTF-8** 编码；`application.yml` 已配置 `spring.sql.init.encoding=UTF-8`。

Windows 也可执行 [scripts/reset-db.ps1](scripts/reset-db.ps1)。

默认连接信息见 [.env.example](.env.example)：

- MySQL: `localhost:3307` / `storage` / `storage` / `storage123`
- MinIO API: `http://localhost:9000`（控制台 `http://localhost:9001`，`minioadmin` / `minioadmin123`）

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

**页面入口**：

- 默认打开 `/login`（Apache Shiro Session 鉴权）；注册 Tab：`/login?tab=register`
- 支持开放注册，新用户默认 `USER` 角色（仅物料台账只读）；注册账号 3-32 字符、密码至少 6 位
- 「记住密码」仅 localStorage 保存账号（不存密码）
- 未登录访问业务页会自动跳转到登录页
- 默认管理员：`admin` / `admin123`（可管理用户/角色/菜单）

**若仍直接进入物料台账而非登录页**：重新运行 `start-dev` 即可（脚本会自动结束占用 5173 的旧 Vite 进程）。

### 一键启动前后端（Windows）

双击项目根目录 [start-dev.cmd](start-dev.cmd)，或在 PowerShell 中执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-dev.ps1
```

将分别打开两个终端窗口运行后端（8080）与前端（5173）。**脚本会先等待后端就绪，再启动前端**，避免登录时 `ECONNREFUSED`。

**端口冲突**：若 8080 / 5173 已被本项目的 `mvn spring-boot:run` 或 `npm run dev` 占用，脚本会**自动结束旧进程**后再启动，无需手动 `taskkill`。改过后端代码后，直接重新运行此脚本即可加载新代码。若端口被其他程序占用，脚本会报错并提示手动处理。

可选参数：

- `-Install`：强制执行 `npm install`
- `-WithDocker`：同时执行 `docker compose up -d` 启动 MySQL 与 MinIO
- `-NoKill`：端口被占用时仅警告，不自动结束进程

### 手动测试文件上传（MinIO）

登录后可用 curl 测试通用上传 API（需先 `docker compose up -d` 启动 MinIO）：

```bash
curl -X POST http://localhost:8080/api/files/upload \
  -b cookies.txt -c cookies.txt \
  -F "file=@README.md"
```

先调用 `POST /api/auth/login` 获取 Session Cookie，再携带 Cookie 上传。MinIO 控制台：`http://localhost:9001`（`minioadmin` / `minioadmin123`）。

## 当前功能

- [x] 登录页 + Shiro Session 登录/登出/当前用户 + **开放注册** + **第五期优化**（左栏插画、记住账号、URL Tab、注册校验、交互打磨）
- [x] 路由守卫与 API 401 拦截（Cookie 会话）+ **路由 permission 校验**
- [x] **系统管理**：用户管理（含角色子 Tab、授权只读面板、Excel 导入导出）+ **客户管理占位** + SideMenu 动态导航（侧栏：用户管理、客户管理）
- [x] MinIO 对象存储基础设施 + `POST /api/files/upload`
- [x] **第六期平台壳层**：DB 导航种子（个人中心/项目/采购/设计/技能/经验/财务 + 仓库 4 项含配置管理）、占位路由、`ComingSoonPage` 复用组件
- [x] **第七期壳层 UI 补全**：动态 TabBar（ADMIN 预置个人中心/项目中心）、壳层 `/platform/*` 路由、侧栏点击无 toast
- [x] 完整平台壳层（侧栏动态导航 + 顶部可关闭页签 + 退出登录）
- [x] 物料台账列表页（筛选、分页、行选择）
- [x] 后端分页查询 API 与筛选选项 API
- [x] 物料台账查看详情（右侧抽屉）
- [x] 物料台账导出 Excel（按当前筛选条件导出全部结果）
- [x] 物料台账 CRUD（新增/编辑/删除，库存数量只读）
- [x] 物料台账 Excel 导入与批量导出/批量删除
- [x] 物料台账筛选联动（品类 → 统称 → 品牌 → 型号/Bin位）
- [x] 公共复用基础层（前端 http/types/utils、后端 converter/query/excel/web）
- [ ] 客户管理业务 CRUD、忘记密码
- [ ] 物料出入库、安全库存管理、Bin位管理、物料清单管理（业务 CRUD，当前为占位页）

## 远端仓库

https://github.com/z136606021-star/Storage.git

协作者与 AI 代理请参阅 [AGENTS.md](AGENTS.md)。新增功能前须按 `AGENTS.md` 的「模块复用与可维护性门禁」检查并优先复用 `api/http.ts`、`types/common.ts`、`utils/`、`converter/`、`query/`、`excel/` 等公共层。
