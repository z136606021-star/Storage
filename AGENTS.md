# AGENTS.md — AI 代理准则

> 本文件只放 AI 代理必须遵守的长期准则、边界与质量门禁。启动方式、功能清单、目录说明放 [README.md](README.md)，历史流水放 [CHANGELOG.md](CHANGELOG.md)。

## 项目边界

- 项目：仓库管理系统，属于项目生命周期管理平台的「资源管理 → 仓库管理」模块。
- 技术栈：Vue 3 + TypeScript + Vite + Ant Design Vue；Java 17 + Spring Boot 3.3 + MyBatis Plus + Apache Shiro；MySQL 8；MinIO。
- 仓库模块只承接物料相关资源流转：物料台账、出入库、安全库存、库存统计、Bin 位、物料清单、通用上传。
- 不要把项目管理、采购审批、财务结算、客户验收、项目进度编排等职责硬塞进仓库模块。
- 系统管理当前包含用户、角色、菜单、客户管理；新增系统级能力时要确认是否属于本仓库边界。

## 文档分工

- `AGENTS.md`：代理准则、质量门禁、架构边界、协作约定。保持短小，高信号。
- `README.md`：项目说明、启动/调试方式、当前功能、常用命令、交付说明。
- `CHANGELOG.md`：阶段记录、历史变更、已落地事项。
- 新增或修改启动方式、脚本、用户可见功能时，同步 `README.md`；新增阶段性记录时同步 `CHANGELOG.md`。
- 只有规则、边界、门禁发生变化时才更新 `AGENTS.md`。

## 工作方式

- 先读现有实现，再动手；优先复用本仓库已有组件、composable、API、DTO、Service、工具类与脚本。
- 保持小步、可审查变更；不要一次性铺开多个模块的大块生成。
- 不要回滚或覆盖用户已有改动；遇到不相关脏工作区，忽略它；遇到相关改动，先理解再衔接。
- 禁止提交 `.env`、真实密码、真实邮箱密码、API Key、访问令牌或本地构建产物。
- 提交信息遵循 Conventional Commits：`feat:`、`fix:`、`chore:` 等。
- 默认分支是 `main`；远端仓库是 `https://github.com/z136606021-star/Storage.git`。

## 复用门禁

新增页面、API、表结构或业务逻辑前，必须先检查可复用能力：

| 层级 | 优先检查 |
|------|----------|
| 前端组件 | `frontend/src/components/`、`frontend/src/layouts/` |
| 前端逻辑 | `frontend/src/composables/`、`frontend/src/utils/`、`frontend/src/constants/` |
| 前端 API/类型 | `frontend/src/api/`、`frontend/src/types/` |
| 后端通用 | `backend/src/main/java/com/storage/config/`、`exception/`、`dto/`、`web/` |
| 后端业务 | `service/`、`mapper/`、`query/`、`converter/`、`excel/` |
| 数据库 | `backend/src/main/resources/db/` |

仓库域 CRUD 页必须优先复用：

- `CrudListPage`
- `CrudToolbar`
- `CrudDetailDrawer`
- `CrudRowActions`
- `usePaginatedCrudList`
- `useExcelImportExport`
- `useLinkedFilterOptions`
- `useWarehouseMaterialFilters`
- `useWritePermission`
- `confirmDelete`
- `tableIndex`
- `warehouseMaterialTable`

参考实现：

- 主参考：`frontend/src/views/material-ledger/MaterialLedgerView.vue`
- 次参考：`frontend/src/views/system/UserManageView.vue`

硬性要求：

- 仓库域同类 CRUD 若骨架和数据流重复度预计达到 80%，必须复用公共层，只在 view 中保留列配置、表单字段和资源 API 绑定。
- 禁止复制粘贴超过 30 行结构相同代码而不抽象；确实不抽象时必须在说明中写理由。
- 禁止把通用逻辑长期留在某个 view、Controller 或 Service 内。
- 禁止为单个页面硬编码本应来自 `filter-options`、字典接口或主数据表的下拉值。

## 分层职责

- `views`：路由入口、状态编排、组件组合；不要塞大段重复 UI 或请求逻辑。
- `components`：可复用 UI 下沉到 `components/common/` 或 `components/warehouse/`。
- `api` + `types`：每个资源一个模块；分页、筛选、导出签名保持一致。
- `service`：业务规则、事务、权限、库存不变量；不要只靠前端判断。
- `query` / `converter` / `excel` / `web`：查询条件、DTO 转换、Excel 契约、响应构建等通用能力优先放这里。
- 数据库字段同义只保留一套命名，例如 `generic_name`、`bin_location`、`project_ref`。

## 环境变量门禁

- 连接串、端口、密钥、桶名、CORS 来源禁止硬编码在 Java / TypeScript / Vue 业务代码中。
- 配置 SSOT：
  - `.env.example`
  - `scripts/worktree-db.ps1`
  - `scripts/worktree-db.sh`
  - `backend/src/main/resources/application.yml`
  - `frontend/vite.config.ts`
  - `docker-compose.yml`
- 新增配置项时必须同步模板、脚本生成逻辑、应用读取处和文档说明。
- `docker-compose.yml` 必须读取 `.env` 中的 `MYSQL_*`、`MINIO_*`；healthcheck 禁止明文密码参数。
- 脚本访问 MySQL 必须读取 `MYSQL_USER`、`MYSQL_PASSWORD`、`MYSQL_DB`，禁止写死 `-pstorage123`。
- `CORS_ALLOWED_ORIGINS` 必须随前端端口或可信前端域名配置，禁止在 Java 配置中写死 `5173`。

## 数据库迁移门禁

- 运行时 schema 版本管理由 **Flyway** 负责，脚本位于 `backend/src/main/resources/db/migration/`，命名规范 `V{version}__{description}.sql`（例如 `V002__add_foo_column.sql`）。
- 新增结构变更只追加 Flyway 版本脚本，禁止回改已发布版本；禁止绕过 Flyway 手工改已有卷结构。
- Flyway 迁移失败必须阻断启动；`spring.sql.init` 主路径已关闭（`mode: never`）。
- MySQL 不支持的 `ADD COLUMN IF NOT EXISTS`、`CREATE INDEX IF NOT EXISTS` 必须用 `INFORMATION_SCHEMA` 条件 DDL guard。
- 迁移脚本不得 `USE storage` 或写死库名，必须使用连接串当前库。
- 历史 `migration-*.sql` 与 `schema.sql` 仅作参考快照，不再参与运行时执行；H2 测试仍使用 `schema-test.sql` 快照。
- 禁止把 `reset-db` / `docker compose down -v` 作为常规升级步骤；已有卷升级必须优先通过 Flyway 增量迁移完成。
- 涉及 DB 结构变更时，说明是否需要重启后端、是否影响已有卷、以及迁移脚本如何保障兼容历史数据。

## API 与校验门禁

新增或修改写 API 时必须满足：

- DTO 使用 Jakarta 校验注解：`@NotBlank`、`@Size`、`@Min` 等。
- Controller 对请求体使用 `@Valid @RequestBody`；嵌套列表使用 `@Valid` + `@NotEmpty`。
- 枚举、状态、用途等字段必须在 DTO 或 Service 层白名单校验。
- Service 层必须校验引用存在、权限、库存、重复物料、删除保护等业务规则。
- `MethodArgumentNotValidException` 等参数错误应返回 400 和明确 `message`，不得 500。
- 至少补一条非法 body 或关键业务不变量测试；涉及前端表单时补前端测试或构建验证。

## 安全门禁

- 开发默认凭据（如 `admin123`、`storage123`、`minioadmin123`）仅限本地开发；生产部署必须轮换。
- `.env` 和真实 SMTP 密码不得入库；`MAIL_PASSWORD` 应使用应用专用密码或部署侧密钥。
- 忘记密码 token 只允许明文出现在邮件链接中，数据库只存哈希；过期和使用后必须失效。
- 注册、登录、忘记密码、重置密码等公网入口必须配合 HTTPS、可信 CORS、JWT 安全配置和限流策略；当前鉴权主路径为 Shiro + JWT Bearer token。
- 文件上传安全校验以后端为准，至少校验大小、MIME 白名单和文件魔数；前端限制只做体验。
- 生产环境必须评估是否关闭开放注册和 admin 自动重置密码。

## 测试与验证

- 后端常规验证：在 `backend` 目录运行 `mvn test "-Dspring.profiles.active=test"`。
- 前端常规验证：在 `frontend` 目录运行 `npm run test`；涉及构建、路由、组件自动导入时运行 `npm run build`。
- 修改 Service / Controller 后，在说明中写明建议断点位置和验证请求。
- 修改配置、脚本或端口时，说明 `.env.example`、worktree 脚本、`application.yml`、`vite.config.ts`、Docker/README 是否同步。

## Worktree 与脚本规则

- 各 worktree 的数据库隔离靠端口、容器名和 Docker 卷；注册表在 `scripts/worktree-db.ps1` 与 `scripts/worktree-db.sh`，两者必须同步。
- `.env` 不入库；切换分支或 worktree 后必须先同步本地 `.env` 再启动依赖服务。
- 代码在 Git 合并，数据库卷不合并；不要跨分支共用另一个 worktree 的数据库端口验证。
- 禁止在未确认目录、分支、compose project 和卷名时执行会删除卷的操作。
- Windows PowerShell 脚本与 Bash 脚本是同等维护对象；新增脚本能力时两端尽量同步。

## 协作与 AI 使用原则

- 开发前先说明复用结论和拟改文件范围；仓库域 CRUD 必须对照参考实现。
- 单次变更聚焦一个子模块或一层，避免一个提交同时大规模改多个 CRUD 页面。
- 维护者应逐文件审查 diff；不清楚内容的代码不得合入。
- 合入标准是可维护、可对比、差异可解释，不是“能跑就行”。

## 多模型分工建议（Cursor）

团队使用多模型协作时，建议按任务属性分工，而不是固定“一个模型做完全部工作”。

- `Composer 2.5`：执行主力。用于改代码、跑命令、调试修复、补测试、完成闭环验收。
- `GPT-5.5`：规划主力。用于需求拆解、方案比较、任务排期、风险与验收标准定义。
- `Claude Sonnet`：文档主力。用于 README/ROADMAP/PR 描述/发布说明/复盘文档等高可读交付材料。

推荐流水线：

1. `GPT-5.5` 产出实现计划与边界；
2. `Composer 2.5` 按计划执行并验证；
3. `Claude Sonnet` 沉淀文档与对外沟通稿。

使用原则：

- 复杂执行与验证优先交给执行主力模型；
- 文档与表达优先交给文档主力模型；
- 在同一工具链内完成计划、执行、文档与验收，减少上下文切换和信息损耗。
