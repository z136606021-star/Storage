# 后续架构优化方案

> 本文以待办清单维护后续优化路线。状态更新约定：`[x]` 已完成，`[~]` 进行中或持续执行的要求，`[ ]` 待开始。约定仅适用于 P0–P8 的执行清单；「当前事实」「设计原则参考」「维护规则」为背景说明与长期准则，不纳入勾选进度。每次完成阶段性实现、验证或范围调整时同步更新本文。

## 当前状态总览

- [x] P0：Docker 部署路线纠偏（Compose 一键部署、Nginx 反代、删除 reset-db 主路径）
- [x] P1：文档和规范落地（README/CHANGELOG/AGENTS 分工收敛）
- [x] P2：引入 Pinia 全局状态（auth/menu 等状态迁移）
- [x] P3：Shiro + JWT 鉴权迁移（Bearer token 主路径）
- [x] P4：动态菜单与动态路由（`component_key` + 动态注册业务路由）
- [x] P5：样式预处理器统一（Less 公共 token + 布局/CRUD 层迁移）
- [x] P6：Service 接口化试点（仓库域核心 Service + AuthService 已完成）
- [~] P7：模块化检查清单（规则已落地，后续 PR 持续执行）
- [ ] P8：Flyway 正式接管数据库版本管理

## P0：Docker 部署路线纠偏

### 背景问题

- [x] 明确禁止把“改表结构后重置数据库卷”作为常规升级流程，避免历史数据丢失。
- [x] 明确 `reset-db` / `docker compose down -v` 只能作为显式危险操作，不进入默认开发/部署主路径。
- [x] 部署流程回归 Docker 原生能力：容器内通过 hostname 通信，Compose 负责依赖拉起与复用，数据库通过持久卷保留数据。
- [x] 前端通过 Dockerfile 构建并由 Nginx 对外提供访问，`/api` 由 Nginx 反向代理后端。

### 已完成

- [x] 新增/调整 `docker-compose.yml`，纳入 `mysql`、`minio`、`backend`、`frontend` 完整服务编排。
- [x] 新增 `docker-compose-dev.yml`，用于开发端口映射与本地调试场景。
- [x] 新增 `backend/Dockerfile`。
- [x] 新增 `frontend/Dockerfile`。
- [x] 新增 `frontend/nginx.conf`，由 Nginx 托管静态资源并反代 `/api`。
- [x] 删除 `scripts/reset-db.ps1` 与 `scripts/reset-db.sh`。
- [x] 新增 `scripts/deploy-cli.ps1` 与 `scripts/deploy-cli.sh`，作为可选 Compose 包装入口。
- [x] `dev-up` / `start-dev` 脚本降级为兼容 wrapper，不再承担默认部署主路径。
- [x] `README.md` 改为 Docker Compose 一句命令启动说明。
- [x] 实测 `docker compose --env-file .env -f docker-compose.yml -f docker-compose-dev.yml up -d --build` 可启动完整环境。
- [x] 实测前端 `http://localhost:5173` 与 `http://localhost` 返回 Nginx 静态页面。
- [x] 实测 `http://localhost:5173/api/auth/me` 经 Nginx 反代后端并返回预期 401。
- [x] 实测重复 `compose up -d` 后 `material_ledger` 数据仍保留（11 行）。

### 待完成

- [ ] 将 Flyway 正式接管迁移后，再复测“旧卷升级仅执行增量迁移”。
- [ ] 根据最终 Flyway 方案清理或降级历史 `schema.sql + migration-*.sql` 入口。

### 可选优化项（非阻塞，供后续 PR 顺手处理）

- [ ] `docker-compose.yml` / `docker-compose-dev.yml` 中 `backend` 服务补充 `healthcheck`，`frontend` 的 `depends_on.backend` 改为 `condition: service_healthy`，避免容器刚起来的几秒内 `/api` 请求短暂失败。
- [ ] 本轮 Docker 部署纠偏改动（`docker-compose*.yml`、`backend/Dockerfile`、`frontend/Dockerfile`、`frontend/nginx.conf`、`deploy-cli.*`、删除 `reset-db.*` 等）本地验证通过后，尽快提交一次 commit，避免长期停留在未提交状态。

### 禁止事项

- [x] 禁止在 README/脚本中推荐“改表结构就 reset-db / down -v”。
- [x] 禁止把强制 `npm install` 作为默认部署步骤。
- [x] 禁止要求先手动启动后端/前端本地进程才能完成部署验收。
- [x] 禁止把容器内数据库地址继续写成 `localhost`。

## P1：文档和规范落地

- [x] 整理优化方向、当前事实、边界与验收标准。
- [x] 保持 `AGENTS.md` 短小，只放代理准则和质量门禁。
- [x] `README.md` 增加 `ROADMAP.md` 入口。
- [x] `CHANGELOG.md` 记录阶段性方案沉淀。
- [x] `README.md` 与 `AGENTS.md` 增加 Cursor 多模型协作说明。
- [x] `ROADMAP.md` 改为待办清单模式。

## P2：引入 Pinia 全局状态

- [x] 增加 Pinia 依赖和应用入口注册。
- [x] 迁移 auth 状态：登录态、JWT、用户信息、权限判断、401 清理。
- [x] 迁移 menu 状态：菜单树、动态路由注册状态。
- [x] 前端 Axios 请求拦截器读取 store/token 并注入 Bearer token。
- [x] 登录刷新后可恢复用户与菜单状态。
- [ ] 持续补齐 store 级前端测试（后续按风险追加）。

## P3：Shiro + JWT 鉴权迁移

- [x] 后端新增 JWT 配置、签名密钥、过期时间、签发与校验服务。
- [x] 配置项进入 `.env.example`、worktree 脚本和 `application.yml`。
- [x] 登录/注册成功返回包含 JWT 的会话响应。
- [x] `/api/auth/me` 改为基于 Bearer token 解析当前用户。
- [x] Shiro 改造为无状态 JWT 过滤链。
- [x] 前端退出登录清理 token。
- [x] 响应 401 时清理 Pinia 状态并跳转登录页。
- [ ] 如需服务端强制失效，后续再设计 token blacklist / refresh token。

## P4：动态菜单与动态路由

- [x] 后端菜单配置 `component_key` 契约。
- [x] 前端维护组件 key 到懒加载组件的映射。
- [x] 固定路由只保留登录、根布局和必要重定向。
- [x] 登录后拉取菜单树并动态 `addRoute` 注册业务路由。
- [x] 权限变化、退出登录时清理动态路由和菜单状态。
- [x] 菜单管理支持维护组件 Key。
- [ ] 后续新增业务页时继续走 `component_key` 契约，不再硬编码完整业务路由表。

## P5：样式预处理器统一

- [x] 在 Less 与 Sass 中确定项目统一预处理器；采用 Less，与 Ant Design Vue 生态更贴近。
- [x] 新增变量、mixins、布局间距、颜色语义等公共样式入口（`frontend/src/styles/`）。
- [x] 全局入口迁移为 `style.less`；后续新组件默认使用 `<style scoped lang="less">`。
- [x] 存量纯 CSS 先迁移公共布局、公共 CRUD 组件，再逐步迁移业务页。
- [x] 代表页验证：`MaterialLedgerView`、`UserManageView` 已接入公共 token/mixins。
- [ ] 其余业务页与弹窗组件按页面改动顺带迁移，避免一次性大面积 diff。

## P6：Service 接口化试点

### 第一阶段（安全库存，已完成）

- [x] 试点 `SafetyStockService` → 接口 + `SafetyStockServiceImpl` 实现类。
- [x] `SafetyStockController`、`WarehouseStatsService` 依赖接口注入。
- [x] 事务与业务规则保留在实现类；`SafetyStockExportService` 继续独立。

### 第二阶段（库存变更，已完成）

- [x] 试点 `MaterialStockMutationService` → 接口 + `MaterialStockMutationServiceImpl` 实现类。
- [x] `MaterialIoService`、`MaterialIoImportService` 依赖接口注入。
- [x] 嵌套 `ImportStockSimulationRow` 保留在接口契约中，调用方路径不变。

### 第三阶段（物料台账，已完成）

- [x] 试点 `MaterialLedgerService` → 接口 + `MaterialLedgerServiceImpl` 实现类。
- [x] `MaterialLedgerController`、`MaterialIoService`、`MaterialIoImportService`、`SafetyStockServiceImpl` 依赖接口注入。
- [x] `MaterialLedgerImportService`、`MaterialLedgerExportService` 继续独立，未合并回主 Service。

### 第四阶段（物料出入库，已完成）

- [x] 试点 `MaterialIoService` → 接口 + `MaterialIoServiceImpl` 实现类。
- [x] `MaterialIoController`、`MaterialIoImportService` 依赖接口注入。
- [x] `importBatch` 保留在接口契约中，供 Excel 导入写入路径调用。
- [x] `MaterialIoImportService`、`MaterialIoExportService`、`MaterialStockMutationService` 继续独立。

### 第五阶段（鉴权服务，已完成）

- [x] 试点 `AuthService` → 接口 + `AuthServiceImpl` 实现类。
- [x] `AuthController`、`FileController`、`SysMenuService`、`SysUserService`、`MaterialIoServiceImpl` 依赖接口注入。
- [x] `currentUser()` 保留在接口契约中，供跨服务获取 Shiro principal。
- [x] `PasswordResetMailService`、`JwtService`、`UserRealm` 继续独立。

### 试点结论（已落地）

- [x] 采用 `XxxService` 接口 + `XxxServiceImpl` 实现类命名。
- [x] Controller 与上层服务依赖接口，事务和业务规则保留在实现类。
- [x] 导入、导出、库存变更等独立能力保留独立接口，未重新耦合回大 Service。
- [ ] 后续新增 Service 继续沿用该模式；如需进一步接口隔离（ISP），按业务能力单独规划。

## P7：模块化检查清单

- [x] 新功能设计前检查高内聚、低耦合、单一职责、接口隔离。
- [x] 对外依赖尽量依赖接口、类型和稳定契约，不依赖具体实现细节。
- [x] 公共能力优先沉淀到 composable、utils、query、converter、excel、web 等既有公共层。
- [x] PR 或代理说明中增加“模块化与复用结论”：复用了什么、拆分了什么、为什么不抽象。
- [~] 后续每个新功能/重构 PR 持续执行该清单（持续性要求，无终点）。

## P8：Flyway 正式接管数据库版本管理

### 当前事实

- 已停止将 `reset-db` / `down -v` 作为默认升级路径。
- 当前 Compose 验收确认重复 `up -d` 不会清空业务数据。
- 当前数据库尚未出现 `flyway_schema_history`，说明 Flyway 还未正式接管迁移，历史结构变更仍靠 `schema.sql` + `migration-*.sql` 承载。

### 待办清单

- [ ] 引入 Flyway 依赖与 Spring Boot 配置。
- [ ] 设计 Flyway baseline 策略，兼容已有数据库卷和新空库。
- [ ] 建立 `db/migration` 版本号命名规范（`Vxxx__description.sql`）。
- [ ] 将后续所有结构变更迁移到 Flyway 版本脚本。
- [ ] 收敛历史 `migration-phase*.sql` 与 `schema.sql` 职责边界。
- [ ] 关闭或降级 `spring.sql.init.mode=always`，避免与 Flyway 双重执行冲突。
- [ ] CI 增加 Flyway 迁移校验。
- [ ] 复测新库初始化、旧库升级、重复启动不重复执行迁移。

## 设计原则参考

> 以下为长期遵循的工程原则，非一次性任务，不使用勾选状态；新功能设计与代码评审时对照检查。

- 开闭原则：新增业务能力优先扩展配置、组件映射、接口实现，不直接修改大量既有调用方。
- 单一职责原则：页面只编排，组件只展示/交互，Service 只处理业务规则，导入导出等能力独立拆分。
- 里氏替换原则：Service 接口实现应保持行为契约一致，替换实现不影响 Controller 和调用方。
- 接口隔离原则：接口按业务能力拆小，不让调用方依赖用不到的方法。
- 依赖倒置原则：Controller、任务编排和上层模块依赖 Service 接口或稳定契约，而非具体实现类。
- 迪米特法则：模块只依赖必要邻居，避免跨层直接读取或修改其他模块内部状态。

## 维护规则

> 以下为长期生效的文档维护约定，不使用勾选状态。

- 每次完成路线图内事项后，立刻把对应项从 `[ ]` 改为 `[x]`。
- 如果事项开始但未完成，标记为 `[~]` 并补一句当前阻塞。
- 新增方向时只加到 `ROADMAP.md`，不要把路线图正文塞回 `AGENTS.md`。
- 重要完成项同步 `CHANGELOG.md`；用户可见启动/部署变化同步 `README.md`。
