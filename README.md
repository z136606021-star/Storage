# 仓库管理系统

项目管理平台中 **资源管理 → 仓库管理** 模块，包含物料台账、物料出入库、安全库存、库存统计、配置管理（Bin位/物料清单）与系统管理（用户/角色/菜单/客户）相关能力。

## 技术栈

- **前端**：Vue 3 + TypeScript + Vite + Ant Design Vue 4（组件自动按需导入）+ Less（样式预处理器）
- **后端**：Java 17+ + Spring Boot 3 + MyBatis Plus + Apache Shiro
- **数据库**：MySQL 8
- **对象存储**：MinIO

## 项目结构说明

本仓库根目录为 `Storage`；后端 Maven artifact/module 名称为 `storage-backend`，源码位于 [backend](backend)，前端源码位于 [frontend](frontend)。IDE 中看到的 `Storage` / `storage-backend` 模块分别对应仓库根与后端模块。

**业务域目录（P9 已完成）**：

- 后端：`com.storage.common.*`、`com.storage.system.*`、`com.storage.warehouse.*`、`com.storage.infrastructure.file.*`；详见 [ROADMAP.md](ROADMAP.md) P9。
- 前端：`views/warehouse`、`views/system`；canonical 路径 `api/warehouse`、`types/warehouse`、`api/system`、`types/system`（根目录 shim 兼容旧 import）。

## 快速启动

### 推荐：Docker Compose 一句命令

需已安装 Docker。默认部署流程统一为 `docker compose up -d`，不再依赖手动启动前后端进程或重置数据库卷。

开发环境（包含开发端口映射）：

```bash
docker compose --env-file .env -f docker-compose.yml -f docker-compose-dev.yml up -d --build
```

生产部署（最小暴露面）：

```bash
docker compose --env-file .env -f docker-compose.yml up -d --build
```

也可使用统一入口脚本：

- Windows：`.\scripts\deploy-cli.ps1 -Profile dev` / `-Profile prod`
- Linux/macOS/Git Bash：`./scripts/deploy-cli.sh --profile dev` / `--profile prod`

环境自检：

```powershell
.\scripts\health-check.ps1
```

遇旧版 `material-ledger-*` 容器冲突时：

```powershell
.\scripts\cleanup-legacy-docker.ps1
```

### 分步启动（进阶）

#### 1. 启动 MySQL 与 MinIO

需已安装 Docker。在项目根目录执行：

```powershell
.\scripts\sync-worktree-env.ps1
docker compose --env-file .env up -d
```

Linux / macOS / Git Bash 等价命令：

```bash
chmod +x scripts/*.sh
./scripts/sync-worktree-env.sh
docker compose --env-file .env up -d
```

若本机 PowerShell 执行策略阻止 `.ps1`，可使用：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\sync-worktree-env.ps1
```

`sync-worktree-env.ps1` / `sync-worktree-env.sh` 会根据**当前 git 分支**生成本地 `.env`（不入库），为各 worktree 分配独立端口、容器名与数据卷。

将自动创建 `storage` 数据库；**表结构与种子数据由后端启动时 Flyway 迁移**（`backend/src/main/resources/db/migration/`），首次启动会执行 `V001__baseline_schema.sql` 并写入 `flyway_schema_history`。

**Git worktree 端口分配**（逻辑库名均为 `storage`，隔离靠端口 + 独立 Docker 卷）：

| 分支 | Worktree 路径 | MySQL | MinIO API | MinIO 控制台 |
|------|---------------|-------|-----------|--------------|
| `main` | `E:/Storage` | **3307** | 9000 | 9001 |
| `feat/material-ledger` | `E:/Storage-worktrees/material-ledger` | **3308** | 9010 | 9011 |
| `feat/material-io` | `E:/Storage-worktrees/material-io` | **3309** | 9020 | 9021 |
| `feat/safety-stock` | `E:/Storage-worktrees/safety-stock` | **3310** | 9030 | 9031 |
| `feat/config-mgmt` | `E:/Storage-worktrees/config-mgmt` | **3311** | 9040 | 9041 |

切换 worktree 或分支后务必先执行 `sync-worktree-env.ps1`（Windows）或 `sync-worktree-env.sh`（Linux/macOS/Git Bash），再 `docker compose --env-file .env up -d`。详见 [AGENTS.md](AGENTS.md)「Worktree 数据库隔离」。

已有数据库卷升级时，Flyway 通过 `baseline-on-migrate` 兼容历史卷，仅执行新增版本脚本；禁止把 `down -v` / 清空卷作为常规升级路径。迁移脚本须为 **UTF-8** 编码；迁移失败会阻断启动以避免静默漏表/漏列。

默认连接信息见 [.env.example](.env.example)：

- 宿主机访问 MySQL/MinIO 走映射端口（dev compose）
- 容器内服务互联使用服务名：`mysql:3306`、`http://minio:9000`（不要用 `localhost`）

其他 worktree 端口见上表；以 `scripts/sync-worktree-env.ps1` 或 `scripts/sync-worktree-env.sh` 生成的 `.env` 为准。

应用端口（各 worktree 通常相同，见 `.env`）：

- `BACKEND_PORT`：后端 HTTP 端口（默认 `8080`）
- `FRONTEND_PORT`：Vite 开发端口（默认 `5173`）
- `VITE_API_PROXY`：仅本地 Vite 开发模式使用；Compose Nginx 部署不依赖该值
- `CORS_ALLOWED_ORIGINS`：后端允许的前端来源（默认跟随 `FRONTEND_PORT`）
- `JWT_SECRET`：JWT HMAC 签名密钥（本地默认仅用于开发，生产必须改为部署侧强密钥）
- `JWT_TTL_MINUTES`：JWT access token 有效期分钟数（默认 `120`）

`sync-worktree-env.ps1` / `sync-worktree-env.sh` 会按分支重写 MySQL/MinIO 端口、容器名和卷名，但会保留已有 `.env` 或当前进程中的数据库/MinIO 凭据、`BACKEND_PORT` / `FRONTEND_PORT` / `VITE_API_PROXY` / `CORS_ALLOWED_ORIGINS` / `SESSION_COOKIE_*` / `RESET_ADMIN_PASSWORD_ON_STARTUP` / `JWT_*` / `UPLOAD_*` / `APP_PUBLIC_BASE_URL` / `PASSWORD_RESET_TOKEN_TTL_MINUTES` / `MAIL_*`，便于本地避开端口冲突和使用自定义本地密码。

### 2. 启动后端

#### 推荐：IntelliJ IDEA 断点调试（日常开发）

功能开发与排错**优先**在 IDEA 中单步调试，而非每次 `mvn package` 进容器或仅靠 `mvn spring-boot:run` 看日志。

1. 确保 Docker 已运行：`.\scripts\sync-worktree-env.ps1` 或 `./scripts/sync-worktree-env.sh` → `docker compose --env-file .env up -d`
2. IDEA 打开 `backend` 模块，新建 **Spring Boot** Run/Debug Configuration：
   - Main class：`com.storage.StorageApplication`
   - Working directory：`backend` 目录
   - 环境变量：安装 [EnvFile](https://plugins.jetbrains.com/plugin/7861-envfile) 插件并勾选项目根目录 `.env`；或手动填入 `MYSQL_*`、`MINIO_*`、`BACKEND_PORT`
3. 在 `*Controller` / `*Service` 打断点（见 [AGENTS.md](AGENTS.md)「开发与调试规范 → 断点推荐位置」）
4. 以 **Debug** 启动后端
5. 另开终端：`cd frontend && npm run dev`
6. 浏览器或 Postman 复现请求，单步跟进

**注意**：IDEA Debug 运行后端时，不要与 Compose 内后端容器同时占用同一端口；如需本地断点，建议暂时停止 `backend` 容器。

#### 备选：命令行启动

需 Java 17+ 与 Maven。在 `backend` 目录：

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`（或 `.env` 中 `BACKEND_PORT`）

### 3. 启动前端

需 Node.js 20+。在 `frontend` 目录：

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`（或 `.env` 中 `FRONTEND_PORT`），开发环境通过 Vite 代理将 `/api` 转发至 `VITE_API_PROXY`（默认后端 8080）。

### Postman / curl 边界测试

前端表单 `rules` 仅为体验；**后端校验才是安全门禁**。改 API 后建议用 Postman 直接打接口（可绕过前端）：

1. `POST /api/auth/login` 获取 JWT access token，后续请求携带 `Authorization: Bearer <token>`
2. 故意发送非法 body，例如：注册空用户名、客户必填字段缺失、出库数量超过库存
3. 期望 **HTTP 400** 与 JSON `message` 字段；不应 500 或静默成功

校验失败入口断点：`GlobalExceptionHandler.handleValidation`；业务规则断点：对应 `*Service.save` / `update`。

**页面入口**：

- 默认打开 `/login`（当前为 Shiro + JWT 鉴权，前端通过 Pinia + localStorage 保存 access token）；注册 Tab：`/login?tab=register`；忘记密码申请 Tab：`/login?tab=forgot`；邮件链接重置 Tab：`/login?tab=reset&token=...`
- 支持开放注册，新用户默认 `USER` 角色（仅物料台账只读）；注册账号 3-32 字符、密码至少 6 位
- 注册支持可选邮箱；忘记密码通过账号 + 邮箱申请邮件一次性链接，链接默认 30 分钟有效，后端统一错误提示并限流（同一账号 15 分钟最多 5 次失败）
- 邮件配置通过 `.env` 预留：`APP_PUBLIC_BASE_URL`、`PASSWORD_RESET_TOKEN_TTL_MINUTES`、`MAIL_HOST`、`MAIL_PORT`、`MAIL_USERNAME`、`MAIL_PASSWORD`、`MAIL_FROM`、`MAIL_SMTP_AUTH`、`MAIL_SMTP_STARTTLS_ENABLE`；Gmail 默认 `smtp.gmail.com:587` + STARTTLS，`MAIL_PASSWORD` 使用 Google 应用专用密码
- 「记住密码」仅 localStorage 保存账号（不存密码）
- 未登录访问业务页会自动跳转到登录页
- 默认管理员：`admin` / `admin123`（可管理用户/角色/菜单）
- 系统管理新建用户默认初始密码后缀：`@123`（即 `{username}@123`）

**动态菜单与路由**：

- 业务路由由后端菜单树驱动：`sys_menu.path` 决定路由路径，`permission` 决定访问权限，`component_key` 决定前端懒加载组件。
- 前端固定路由仅保留登录、根布局和必要重定向；登录或刷新恢复后由 Pinia menu store 拉取 `/api/menus/nav-tree` 并动态注册业务路由。
- 菜单管理新增“组件 Key”字段；可见且有路由路径的 `MENU` 必须填写组件 Key，隐藏动作权限可不填写。
- 当前可用组件 Key：`MaterialLedger`、`MaterialIo`、`SafetyStock`、`InventoryStats`、`BinManage`、`BomManage`、`SystemManageLayout`、`UserManage`、`RoleManagePanel`、`MenuManagePanel`、`CustomerManage`、`ShellPlaceholder`。

### 样式约定（Less）

- 统一预处理器为 **Less**；全局入口为 [frontend/src/style.less](frontend/src/style.less)。
- 公共 token 与 mixins 位于 [frontend/src/styles/](frontend/src/styles/)（`variables.less`、`mixins.less`、`index.less`）。
- 新增或改动带样式的组件时，使用 `<style scoped lang="less">`，优先复用 `@color-*`、`@spacing-*`、`@radius-*` 等变量，以及 `btn-success-primary`、`row-highlight`、`filter-form-label` 等 mixin。
- 存量带 `<style>` 的页面已全部迁移 Less；登录页使用独立 `@color-login-*` 品牌 token。

### Compose 部署访问方式

启动完成后：

- 前端入口：`http://localhost:${APP_PORT}`（或 dev profile 下 `http://localhost:${FRONTEND_PORT}`）
- 前端请求 `/api/**` 由 Nginx 反向代理至 `backend:8080`
- 无需手动启动 `mvn spring-boot:run` / `npm run dev` 即可完成完整部署访问

### 手动测试文件上传（MinIO）

登录后可用 curl 测试通用上传 API（需先 `docker compose up -d` 启动 MinIO）：

```bash
curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@README.md"
```

先调用 `POST /api/auth/login` 获取 `accessToken`，再用 Bearer token 携带凭证上传。MinIO 控制台：`http://localhost:9001`（`minioadmin` / `minioadmin123`）。

物料清单图片可在 **配置管理 → 物料清单** 新增/编辑弹窗中上传（JPG/PNG/WebP/GIF，≤5MB）；后端会校验大小、MIME 白名单与图片魔数，保存后列表与详情展示缩略图。Excel 导入导出不含图片列。

## 当前功能

- [x] 登录页 + Shiro JWT 登录/登出/当前用户 + Pinia 全局 auth store + **开放注册** + **第五期优化**（左栏插画、记住账号、URL Tab、注册校验、交互打磨）
- [x] 路由守卫与 API 401 拦截（Bearer token）+ **路由 permission 校验**
- [x] **系统管理**：用户管理（含角色/菜单子 Tab、多角色分配、授权只读面板、Excel 导入导出）+ **菜单管理 Tab（CRUD）** + **客户管理 CRUD**（Excel 导入导出）+ SideMenu 动态导航（侧栏：用户管理、客户管理）
- [x] MinIO 对象存储基础设施 + `POST /api/files/upload`
- [x] **第六期平台壳层**：DB 导航种子（个人中心/项目/采购/设计/技能/经验/财务 + 仓库 5 项含库存统计与配置管理）、占位路由、`ComingSoonPage` 复用组件
- [x] **第七期壳层 UI 补全**：动态 TabBar（ADMIN 预置个人中心/项目中心）、壳层 `/platform/*` 路由、侧栏点击无 toast
- [x] **第八期 DevX**：`dev-up` 一键环境、`health-check` 自检、`cleanup-legacy-docker`、MySQL 就绪等待、`material_ledger` 中文修复迁移
- [x] 完整平台壳层（侧栏动态导航 + 顶部可关闭页签 + 退出登录）
- [x] 物料台账列表页（筛选、分页、行选择）
- [x] 后端分页查询 API 与筛选选项 API
- [x] 物料台账查看详情（右侧抽屉）
- [x] 物料台账导出 Excel（按当前筛选条件导出全部结果）
- [x] 物料台账 CRUD（新增/编辑/删除，库存数量只读）
- [x] 物料台账 Excel 导入与批量导出/批量删除
- [x] 物料台账筛选联动（品类 → 统称 → 品牌 → 型号；Bin 位来自 Bin 主数据）
- [x] 公共复用基础层（前端 http/types/utils、后端 converter/query/excel/web）
- [x] **第十一期 11.1 Bin位管理**：CRUD + Excel + 台账 Bin 下拉/保存校验（`warehouse_bin` 主数据）
- [x] **第十一期 11.2 物料清单管理**：CRUD + 品类联动筛选 + Excel（`warehouse_bom` 主数据）
- [x] **第十一期 11.3 台账 ↔ 物料清单关联**：表单「从清单选择」、四元组严格校验、`GET /api/materials/bom-catalog`、清单删除引用保护
- [x] **第十一期 11.4 物料清单 MinIO 图片**：`image_object_key` 持久化、表单上传/预览/清除、列表与详情缩略图
- [x] 客户管理业务 CRUD、忘记密码
- [x] **第十三期物料出入库**：`material_io_record` 流水、批量入库/出库、台账选择器、库存联动、Excel 导入导出（`warehouse:material-io:write`）
- [x] **第十四期物料出入库完善**：筛选联动修复、Excel 原子导入、台账选择器联动筛选、批量表单库存列、台账删除 IO 引用保护、只读用户可下载模板
- [x] **第十五期物料出入库优化**：后端集成测试、可用库存 UI、台账↔出入库追溯、编辑禁改类型、出库选择器增强
- [x] **第十六期物料出入库复用与契约**：`useWarehouseMaterialFilters`/`WarehouseMaterialFilterPanel`、深链 composable、IO↔台账互跳、后端 ioType/重复物料不变量
- [x] **第十七期追溯闭环与测试**：台账 `?materialLedgerId=` 打开详情、`importBatch` 重复拦截、H2 集成测试扩展
- [x] **第十八期追溯体验与复用深化**：深链新增预填、IO↔台账读权限对称、编辑禁改物料、列/query 复用、`useMaterialLedgerList`、`MaterialIoDetailDescriptions`
- [x] **第十九期流水深链与质量基建**：`?id=` 打开出入库详情、`MaterialStockMutationService`、Vitest + `useMaterialIoStock` 单测
- [x] **第二十期列表复用与深链闭环**：`useMaterialIoList`、深链列表定位与 URL 同步、Excel 导入出库超库存行级报错、深链/库存单测扩展
- [x] **第二十一期追溯快捷与契约瘦身**：上下文条新增入库/出库、`useCrudRouteDetail`、`MaterialIoUpdateDTO`、详情复制链接
- [x] **第二十二期安全库存管理**：`safety_stock` 表、预警黄行、导出/编辑 upsert、`SafetyStockView`、`warehouse:safety-stock:write`
- [x] **第二十三期出入库业务语义补全**：`purpose` 用途枚举、出库必填、列表筛选/Excel；`GET /api/material-io/safety-hints` 出库预警；新增补录 `operatedAt`；`useMaterialIoSafetyHint`
- [x] **第二十四期出入库 UI 优化**：`MaterialIoFilterPanel`、`MaterialIoContextBar`、工具栏「更多」、新增弹窗条件列与预警列、安全确认 checkbox
- [x] **第二十五期库存统计与项目关联**：`GET /api/warehouse-stats/overview`、`InventoryStatsView`、`project_ref` 项目编号、出入库工具栏新增下拉
- [x] **第二十六期客户管理与忘记密码**：`sys_customer` CRUD/Excel、`CustomerManageView`、`POST /api/auth/forgot-password`、登录页 `?tab=forgot`
- [x] **第三十期忘记密码邮件链接重置**：Google SMTP 环境变量预留、`password_reset_token`、`POST /api/auth/reset-password`、登录页 `?tab=reset&token=...`
- [x] **第二十七期差距收敛**：文档与客户占位表述同步、台账路由读权限、注册可选邮箱 + admin 种子邮箱、库存统计 `recentDays` 选择器、台账/Bin/客户 import 集成测试、GitHub Actions CI
- [x] **第二十八期开发与调试规范**：环境变量门禁、IDEA 断点工作流、前后端双端校验 checklist、启动脚本端口参数化
- [x] **第三十四期跨平台 DevX**：补齐 Linux/macOS/Git Bash 版 `deploy-cli.sh` / `sync-worktree-env.sh` / `wait-mysql.sh` / `health-check.sh` / `cleanup-legacy-docker.sh`
- [x] **第二十九期质量门禁与安全加固**：Docker/CORS 凭据参数化、迁移 fail-fast + 条件 DDL、忘记密码统一提示与限流、Auth Controller 集成测试、CI 增加前端 build、路由懒加载 + AntD 按需导入
- [x] **第三十一期模块复用复查与废弃物清理**：复核仓库/系统同类列表页仍复用公共 CRUD/composable/utils 层，清理未引用脚手架注释文件与 Vite 模板残留
- [x] **第三十二期安全门禁复查与上传加固**：复核环境变量、CORS、Session、忘记密码、迁移脚本与敏感信息门禁；上传增加图片魔数校验
- [x] **第三十三期系统环境变量一致性审核**：`.env.example` 与 `worktree-db.ps1` 变量清单一致；启动脚本显式注入后端运行环境变量；文档补齐 SSOT
- [x] **第三十五期 Pinia + JWT 鉴权迁移**：Pinia auth store、JWT access token、本地刷新恢复、Bearer 请求注入、无状态 Shiro JWT 认证链路
- [x] **第三十六期动态菜单与动态路由**：菜单 `component_key` 契约、Pinia menu store、登录后动态注册业务路由、菜单管理维护组件 Key
- [x] **第三十七期样式预处理器统一**：引入 Less、`frontend/src/styles/` 公共 token/mixins、布局与 CRUD 公共层迁移、代表业务页验证
- [x] **P8 Flyway 数据库版本管理**：Flyway 接管 schema 迁移、`V001__baseline_schema.sql` baseline、关闭 `spring.sql.init` 主路径、CI MySQL Flyway 校验

## 协作约定（多模型）

在 Cursor 中建议按任务属性分工，而不是固定单模型完成所有环节：

- `Composer 2.5`：执行与验证主力（改代码、跑命令、调试、补测试、闭环验收）
- `GPT-5.5`：规划主力（需求拆解、方案比较、风险识别、验收标准）
- `Claude Sonnet`：文档主力（README/ROADMAP/PR 描述/发布说明/复盘）

推荐流水线：

1. `GPT-5.5` 输出计划与边界；
2. `Composer 2.5` 按计划实施并验证；
3. `Claude Sonnet` 产出文档与沟通材料。

## 安全与生产部署要点

- 本仓库默认凭据（`admin123`、`storage123`、`minioadmin123`）仅用于本地开发，生产部署必须轮换为强密码
- `POST /api/auth/register` 是否对公网开放须由部署侧显式评估；忘记密码接口已改为邮件一次性链接，但仍需配合 HTTPS、可信前端域名与限流策略
- 忘记密码 token 仅明文出现在邮件链接中，数据库保存 SHA-256 哈希，默认 30 分钟过期且使用后失效；失败限流为单实例内存级（15 分钟 5 次），多实例生产需迁移到 Redis/网关限流
- Google SMTP 默认按 Gmail 直连预留：`smtp.gmail.com:587` + STARTTLS；`MAIL_PASSWORD` 使用 Google 应用专用密码，不提交真实邮箱密码。公司 Workspace relay 可通过环境变量切换到 `smtp-relay.gmail.com`
- 当前鉴权主路径为 Shiro + JWT；access token 存储在前端 localStorage。生产环境需启用 HTTPS，`JWT_SECRET` 必须使用部署侧强密钥，`CORS_ALLOWED_ORIGINS` 必须仅配置可信前端域名，并持续防护 XSS 风险
- 默认管理员密码自动重置能力仅建议本地排障开启，生产应关闭该初始化开关，避免启动时回退弱密码
- 文件上传以后端校验为准：当前限制 JPG/PNG/WebP/GIF、默认 ≤5MB，并校验图片魔数，前端限制仅用于体验优化
- GitHub Actions CI：[`CI workflow`](https://github.com/z136606021-star/Storage/blob/main/.github/workflows/ci.yml)（后端测试 + 前端测试/构建）

后端测试：`cd backend && mvn test "-Dspring.profiles.active=test"`

前端测试：`cd frontend && npm run test`

前端构建：`cd frontend && npm run build`（CI 同步执行，用于捕获类型与模板错误）

## 远端仓库

https://github.com/z136606021-star/Storage.git

历史变更记录见 [CHANGELOG.md](CHANGELOG.md)。

后续优化路线见 [ROADMAP.md](ROADMAP.md)。

协作者与 AI 代理请参阅 [AGENTS.md](AGENTS.md)。新增功能前须按 `AGENTS.md` 的「模块复用与可维护性门禁」检查并优先复用 `CrudListPage`、`usePaginatedCrudList`、`useExcelImportExport`、`api/http.ts`、`types/common.ts`、`utils/`、`converter/`、`query/`、`excel/` 等公共层。
