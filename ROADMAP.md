# 后续架构优化方案

> 本文以待办清单维护后续优化路线。状态更新约定：`[x]` 已完成，`[~]` 进行中或持续执行的要求，`[ ]` 待开始。约定仅适用于 P0–P10 的执行清单；「当前事实」「设计原则参考」「维护规则」为背景说明与长期准则，不纳入勾选进度。每次完成阶段性实现、验证或范围调整时同步更新本文。

## 当前状态总览

- [x] P0：Docker 部署路线纠偏（Compose 一键部署、Nginx 反代、删除 reset-db 主路径）
- [x] P1：文档和规范落地（README/CHANGELOG/AGENTS 分工收敛）
- [x] P2：引入 Pinia 全局状态（auth/menu 等状态迁移）
- [x] P3：Shiro + JWT 鉴权迁移（Bearer token 主路径）
- [x] P4：动态菜单与动态路由（`component_key` + 动态注册业务路由）
- [x] P5：样式预处理器统一（27/27 个带 `<style>` 的 `.vue` 已迁 `lang="less"`；登录页品牌 token 已收敛）
- [x] P6：Service 接口化试点（12 个主业务 Service 已拆接口+实现；Import/Export/基础设施类保持独立）
- [~] P7：模块化检查清单（规则已落地，后续 PR 持续执行）
- [x] P8：Flyway 正式接管数据库版本管理
- [x] P9：业务域模块拆分（大仓小仓模式；Phase 0–12 已落地）
- [x] P10：Excel 声明式框架评估（导出 8/8 已迁 EasyExcel；导入保留 POI + `ExcelCellUtils`）

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

- [x] 将 Flyway 正式接管迁移后，再复测“旧卷升级仅执行增量迁移”。
- [x] 根据最终 Flyway 方案清理或降级历史 `schema.sql + migration-*.sql` 入口。

### 可选优化项（非阻塞，供后续 PR 顺手处理）

- [x] `docker-compose.yml` / `docker-compose-dev.yml` 中 `backend` 服务补充 `healthcheck`，`frontend` 的 `depends_on.backend` 改为 `condition: service_healthy`，避免容器刚起来的几秒内 `/api` 请求短暂失败。
- [~] Docker 部署与 P1–P10 收敛改动本地验证通过后，由维护者确认并提交 commit（代理不自动提交）。

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
- [x] store 级前端测试基线（`auth.test.ts` / `menu.test.ts`：token 恢复、登录/登出、Bearer 注入、动态路由注册/清理、系统管理嵌套路由）；后续按风险追加。

## P3：Shiro + JWT 鉴权迁移

- [x] 后端新增 JWT 配置、签名密钥、过期时间、签发与校验服务。
- [x] 配置项进入 `.env.example`、worktree 脚本和 `application.yml`。
- [x] 登录/注册成功返回包含 JWT 的会话响应。
- [x] `/api/auth/me` 改为基于 Bearer token 解析当前用户。
- [x] Shiro 改造为无状态 JWT 过滤链。
- [x] 前端退出登录清理 token。
- [x] 响应 401 时清理 Pinia 状态并跳转登录页。
- [x] JWT 服务端强制失效：`jti` + `jwt_revoked_token` 黑名单；`POST /api/auth/logout` 撤销当前 token；refresh token 暂不引入。

## P4：动态菜单与动态路由

- [x] 后端菜单配置 `component_key` 契约。
- [x] 前端维护组件 key 到懒加载组件的映射。
- [x] 固定路由只保留登录、根布局和必要重定向。
- [x] 登录后拉取菜单树并动态 `addRoute` 注册业务路由。
- [x] 权限变化、退出登录时清理动态路由和菜单状态。
- [x] 菜单管理支持维护组件 Key。
- [~] 后续新增业务页时继续走 `component_key` 契约，不再硬编码完整业务路由表（持续门禁）。

### 已知例外

- [x] 系统管理子路由已收敛：`nav-tree` 返回含 `visible` 的隐藏路由节点，侧栏过滤后仅展示可见菜单；`/system/users` 下用户/角色/菜单由菜单数据嵌套注册（`V002` 迁移）。

## P5：样式预处理器统一

> **当前完成度（2026-07-06 收敛）**：全仓库 27 个带 `<style>` 的 `.vue` 已全部迁 `<style scoped lang="less">`；补充登录页品牌色 token（`@color-login-*`）与 `@color-border-info`。

- [x] 在 Less 与 Sass 中确定项目统一预处理器；采用 Less，与 Ant Design Vue 生态更贴近。
- [x] 新增变量、mixins、布局间距、颜色语义等公共样式入口（`frontend/src/styles/`）。
- [x] 全局入口迁移为 `style.less`；后续新组件默认使用 `<style scoped lang="less">`。
- [x] 存量纯 CSS 先迁移公共布局、公共 CRUD 组件，再逐步迁移业务页。
- [x] 代表页验证：`MaterialLedgerView`、`UserManageView` 已接入公共 token/mixins。
- [x] 其余业务页与弹窗组件已分批迁移；`LoginView` 使用独立登录页语义 token，不硬套仓库业务色值。

## P6：Service 接口化试点

> **当前完成度（2026-07-06 收尾）**：`service` 包内 29 个 `@Service` 类中，12 个主业务 Service 已拆为 `XxxService` 接口 + `XxxServiceImpl`（含 `WarehouseStatsService`、`system.customer.SysCustomerService`）；Import/Export、`JwtService`、`FileStorageService`、`PasswordResetMailService` 等保持直接实现类。

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
- [x] 台账导出逻辑已合并回 `MaterialLedgerService`，不再保留独立 `MaterialLedgerExportService` 文件。

### 第四阶段（物料出入库，已完成）

- [x] 试点 `MaterialIoService` → 接口 + `MaterialIoServiceImpl` 实现类。
- [x] `MaterialIoController`、`MaterialIoImportService` 依赖接口注入。
- [x] `importBatch` 保留在接口契约中，供 Excel 导入写入路径调用。
- [x] `MaterialIoImportService`、`MaterialIoExportService`、`MaterialStockMutationService` 继续独立。

### 第五阶段（鉴权服务，已完成）

- [x] 试点 `AuthService` → 接口 + `AuthServiceImpl` 实现类。
- [x] `AuthController`、`FileController`、`SysMenuServiceImpl`、`SysUserServiceImpl`、`MaterialIoServiceImpl` 依赖接口注入。
- [x] `currentUser()` 保留在接口契约中，供跨服务获取 Shiro principal。
- [x] `PasswordResetMailService`、`JwtService`、`UserRealm` 继续独立。

### 第六阶段（Bin 位 / 物料清单，已完成）

- [x] 试点 `WarehouseBinService`、`WarehouseBomService` → 接口 + `*ServiceImpl` 实现类。
- [x] `WarehouseBinController`、`WarehouseBomController`、`MaterialLedgerServiceImpl`、`MaterialLedgerImportService`、各 Import Service 依赖接口注入。
- [x] `WarehouseBinImportService`、`WarehouseBinExportService`、`WarehouseBomImportService`、`WarehouseBomExportService` 继续独立。

### 第七阶段（系统管理，已完成）

- [x] 试点 `SysMenuService`、`SysRoleService`、`SysUserService` → 接口 + `*ServiceImpl` 实现类。
- [x] `SysMenuController`、`MenuNavController`、`SysRoleController`、`SysUserController` 依赖接口注入。
- [x] `SysUserImportService` 保留 `@Lazy` 循环依赖处理；各 Import/Export Service 继续独立。
- [x] 客户域仅保留 `com.storage.system.customer.*` 路由，旧扁平包 `SysCustomer*` 不再并存。

### 第八阶段（库存统计 / 客户域，已完成）

- [x] 试点 `WarehouseStatsService` → 接口 + `WarehouseStatsServiceImpl` 实现类。
- [x] `WarehouseStatsController`、集成测试依赖接口注入。
- [x] 试点 `system.customer.SysCustomerService` → 接口 + `SysCustomerServiceImpl` 实现类。
- [x] `SysCustomerController`、`SysCustomerImportService` 依赖接口；`formatStatusLabel` / `parseStatus` 保留为接口 static 方法。

### 试点结论（已落地）

- [x] 采用 `XxxService` 接口 + `XxxServiceImpl` 实现类命名。
- [x] Controller 与上层服务依赖接口，事务和业务规则保留在实现类。
- [x] 导入、导出、库存变更等独立能力保留独立接口，未重新耦合回大 Service。
- [x] 后续新增主业务 Service 继续沿用该模式；Import/Export/基础设施类有意保持独立，不一刀切接口化。

## P7：模块化检查清单

- [x] 新功能设计前检查高内聚、低耦合、单一职责、接口隔离。
- [x] 对外依赖尽量依赖接口、类型和稳定契约，不依赖具体实现细节。
- [x] 公共能力优先沉淀到 composable、utils、query、converter、excel、web 等既有公共层。
- [x] PR 或代理说明中增加“模块化与复用结论”：复用了什么、拆分了什么、为什么不抽象。
- [~] 后续每个新功能/重构 PR 持续执行该清单（持续性要求，无终点）。

## P8：Flyway 正式接管数据库版本管理

### 当前事实

- Flyway 已接管运行时 schema 版本管理，脚本位于 `backend/src/main/resources/db/migration/`。
- `spring.sql.init` 主路径已关闭；Compose 不再挂载 `schema.sql` 到 initdb。
- 已有卷通过 `baseline-on-migrate` + `baseline-version=1` 兼容，新空库由 `V001__baseline_schema.sql` 初始化。
- CI 增加 MySQL Flyway migrate/validate job；H2 业务测试仍使用 `schema-test.sql` 快照。

### 待办清单

- [x] 引入 Flyway 依赖与 Spring Boot 配置。
- [x] 设计 Flyway baseline 策略，兼容已有数据库卷和新空库。
- [x] 建立 `db/migration` 版本号命名规范（`Vxxx__description.sql`）。
- [x] 将后续所有结构变更迁移到 Flyway 版本脚本。
- [x] 收敛历史 `migration-phase*.sql` 与 `schema.sql` 职责边界。
- [x] 关闭或降级 `spring.sql.init.mode=always`，避免与 Flyway 双重执行冲突。
- [x] CI 增加 Flyway 迁移校验。
- [x] 复测新库初始化、旧库升级、重复启动不重复执行迁移。

## P9：业务域模块拆分

### 当前事实

- **路线已冻结**：采用单 JAR 内按顶层业务域拆分（`com.storage.common.*` / `com.storage.system.*` / `com.storage.warehouse.*` / `com.storage.infrastructure.*`），暂不拆 Maven 多模块。
- **跨域规则**：仓库域只能依赖系统域接口或只读契约，禁止直接依赖 `*Impl` 或系统 mapper；REST path、权限码、DTO JSON、Flyway 脚本不因拆包而变更。
- **已完成（Phase 0–13）**：
  - `@MapperScan` 按域精确扫描 mapper 包（禁止扫描 `com.storage` 根包）。
  - `com.storage.common.*`：共享 DTO/异常/Excel/Web/Config（`CorsConfig`、`WebMvcConfig`、`MybatisPlusConfig`、`PasswordConfig`）。
  - `com.storage.system.{customer,role,user,menu,auth}.*`：系统域垂直切片；`OperatorResolver` 供仓库域解析操作人；鉴权配置（`JwtProperties`、`AdminPasswordInitializer`）归入 `system.auth.config`。
  - `com.storage.warehouse.*`：仓库域作为单一 module，按 `controller` / `service` / `mapper` / `dto` / `entity` / `excel` 等层级组织，不再按 bin/bom/io/ledger/safety/stats 二次业务分包。
  - `com.storage.infrastructure.file.*`：文件上传/MinIO 边界（含 `file.config`：`MinioConfig`、`MinioProperties`、`FileUploadProperties`）。
  - 前端：`views/warehouse`、`views/system`；`api/warehouse`、`types/warehouse`、`api/system`、`types/system` 为 canonical 路径，根目录 shim 保留兼容。
- **持续要求**：新增能力优先落入对应域包；PR 附带模块化与复用结论。

### 目标

- 按顶层业务域拆分模块：至少区分 **仓库域**（`warehouse`：台账、出入库、安全库存、Bin、清单、统计等）与 **系统域**（`system`：用户、角色、菜单、客户、鉴权等）；仓库域内部保持按层分包，避免过多小 service/interface 文件。
- 各域内保持高内聚、低耦合；跨域依赖通过接口或稳定契约，不直接穿透实现细节。

### 待办清单

- [x] 调研并确定分包策略：单模块内按域顶层包拆分，不先拆 Maven 多模块。
- [x] 制定分阶段迁移计划并落地首批样板（客户管理垂直切片 + common 层 + 前端轻量对齐）。
- [x] 前端 `views` / `api` / `types` 按域对齐（仓库域目录 + shim；`component_key` 不变）。
- [x] 文档与 AGENTS 复用门禁同步更新域边界说明。
- [x] Phase 4–8：曾按仓库子业务垂直切片迁移，后续 Phase 13 已回收为仓库单 module 按层分包。
- [x] Phase 9：`com.storage.system.role.*` / `user.*` / `menu.*`（含 `MenuNavController`）。
- [x] Phase 10：`com.storage.system.auth.*`（Auth、Shiro、PasswordReset）。
- [x] Phase 11：前端 import 收敛、动态路由例外文档化、删除扁平包遗留。
- [x] Phase 12：根级 `com.storage.config` 收口至 `common.config` / `system.auth.config` / `infrastructure.file.config`；删除根级技术包。
- [x] Phase 13：仓库域从 `warehouse.{bin,bom,ledger,shared,safety,stats,io}.*` 回收为单 module 按层分包；简单 CRUD/计数优先 MyBatis-Plus Service/Wrapper，复杂 SQL 保留在 mapper。

### 仓库域后续迁移顺序（Phase 4+）

1. **低风险**：`WarehouseBin*`（域内依赖为主，仅只读引用 `MaterialLedgerMapper`）。
2. **中风险**：文件基础设施边界 → `WarehouseBom*` → 仓库共享 DTO → `MaterialLedger*`。
3. **中风险**：`SafetyStock*`、`WarehouseStats*`（依赖台账与安全库存聚合）。
4. **高风险**：操作人契约 → `MaterialIo*`（库存变更、`AuthService` 跨域）。

### 系统域后续迁移顺序

1. `SysCustomer`（已完成）。
2. `SysRole*`、`SysUser*`、`SysMenu*`（含 `MenuNavController`）。
3. `Auth*`、`shiro/*`、`PasswordReset*`（全局鉴权，牵动所有 Controller 测试）。
4. `FileStorageService` / `FileController` → `com.storage.infrastructure.file.*`（与 BOM 迁移配合，最终收口）。

## P10：Excel 声明式框架评估

### 当前事实

- `backend/pom.xml` 已引入 `org.apache.poi:poi-ooxml:5.2.5` 与 `com.alibaba:easyexcel:4.0.3`；导出路径统一 EasyExcel + `*ExportRow`，导入路径仍使用 POI + `ExcelCellUtils`。
- 各域 `*ImportService` / `*ExportService` 共 15 个；导出 8 个、导入 7 个。已有 `ExcelCellUtils`、`ExcelExportWriter`、`ExcelExportStyleHandlers`、`*ExcelColumn` 枚举与 `ImportResultVO` 契约。
- 导出迁移进度 **8/8**：全部 `*ExportService` 已迁 EasyExcel + `*ExportRow` + 行为测试；`MaterialIoImportTemplateRow` / `MaterialIoImportTemplateColumn` 单独承载导入模板表头与列序（不含操作人列，操作时间 index 11）。

### 评估结论（2026-07-06）

| 路线 | Spring Boot 3 / Java 17 | 维护状态 | 与现有契约契合 | 结论 |
|------|-------------------------|----------|----------------|------|
| **EasyExcel 4.0.3** | 兼容（内置 POI 5.2.5） | 官方维护模式，GitHub 已归档 | 导出注解模型清晰；导入需自定义 `AnalysisEventListener` 才能保留 `ImportResultVO` 行号语义 | **导出试点推荐** |
| **EasyPOI（社区 Jakarta 版）** | 需 `top.rwocj` 等社区包 | 原版对 SB3 支持弱，javax/jakarta 混用风险 | 注解 + Controller 导出风格，但与当前 Service 分层不一致 | **不作为默认落地依赖** |
| **继续 POI + 公共层** | 完全可控 | 无第三方生命周期风险 | 与 `ImportResultVO`、行级错误、`.xls/.xlsx` 完全契合 | **导入路径继续保留** |

**试点收益**：`SafetyStockExportService` 从 ~63 行 POI 样板降至 ~45 行（含行映射），表头/数据/样式由 `SafetyStockExportRow` + `ExcelExportStyleHandlers` 声明；行为测试保障 sheet 名、列顺序、预警标签不变。

**试点风险**：EasyExcel 进入维护模式；列宽策略与手写 `autoSizeColumn` 略有差异（采用 `LongestMatchColumnWidthStyleStrategy`）；导入若迁移需重写行号错误聚合与 `MaterialIoImportService` 原子校验逻辑。

### 目标

- 评估引入 EasyExcel（或同类声明式框架）替代手写 POI，缩短代码、提升可维护性与复用性；与 P6 试点模式一致，先单域验证再推广。

### 待办清单

- [x] 对比 EasyExcel / EasyPOI 与当前 POI + `ExcelCellUtils` 的契合度（注解模型、校验、大文件、与现有 `ImportResultVO` 契约）。
- [x] 选定 1 个 Import 或 Export Service 做试点（`SafetyStockExportService` export-only）。
- [x] 试点通过后制定其余 Export/Import Service 的迁移顺序与是否保留 `excel` 公共层（见下）。
- [x] 引入新依赖并同步 CI 与 AGENTS 复用门禁说明。

### 后续迁移顺序（试点后）

**Export（优先 EasyExcel + `*ExportRow` + `ExcelExportStyleHandlers`）**：

1. `SafetyStockExportService`（已完成试点）
2. `WarehouseBinExportService`、`SysRoleExportService`、`SysCustomerExportService`（已完成第二批）
3. `WarehouseBomExportService`（已完成第三批）；物料台账导出已并回 `MaterialLedgerService`
4. `SysUserExportService`、`MaterialIoExportService`（已完成第四批；含导入模板 `MaterialIoImportTemplateRow`）

**Import（暂保留 POI + `ExcelCellUtils` + `*ExcelColumn`）**：

- 7 个 `*ImportService` 需保留 `ImportResultVO` 行号、空行过滤、业务校验与 `MaterialIoImportService` 原子写入；待导出迁移稳定后再评估 `AnalysisEventListener` 抽象，不一次性切换。

**公共层保留**：

- `ExcelCellUtils`（导入读单元格）、`ExcelExportWriter`（EasyExcel 写出）、`ExcelExportStyleHandlers`（表样式）、`ExcelResponseBuilder`（HTTP 响应）、`ImportResultVO`、`*ExcelColumn`（列序 SSOT）；导出使用 `*ExportRow` + `@ExcelProperty`。

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
- **持续清理未引用代码**：每个功能 PR 或阶段性重构时顺带删除废弃变量、未引用文件与脚手架残留（非一次性任务）；避免仅依赖单期「废弃物清理」后不再跟踪。
