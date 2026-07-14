# CHANGELOG

| 2026-07-14 | 移除每条记录文件数量上限：删除 `UPLOAD_MAX_FILES_PER_RECORD` 与 upload-policy `maxFilesPerRecord`；BOM/经验库不再限制单条记录附件个数，上传安全仅保留单文件大小与 HTTP 请求体大小 |
| 2026-07-14 | 固定基础设施入 Compose + 生产外部 MySQL：移除 `.env` 中 `COMPOSE_PROJECT_NAME`/`STORAGE_*`；生产 `docker-compose.yml` 去掉 MySQL 容器，仅保留 backend/frontend；dev Compose 写死 3307/9000/9001 与 `storage-*` 容器名；prod 校验外部 `MYSQL_*`/`MINIO_*` |
| 2026-07-14 | 生产复用外部 MinIO：`docker-compose.yml` 移除 MinIO 服务/卷/端口与 backend 硬编码 endpoint；`docker-compose-dev.yml` 保留本地 MinIO；`deploy-cli` prod 不再自动 sync `.env` 并要求外部 `MINIO_*`；`health-check` 增加 `-Profile dev|prod`；README/.env.example 区分本地与生产 MinIO 配置 |
| 2026-07-13 | 邮箱找回密码：忘记密码改为仅凭绑定邮箱申请重置链接，移除账号输入；限流键改为邮箱 |
| 2026-07-13 | 个人手机号维护：个人中心账号信息新增手机号行与齿轮编辑；`PUT /api/auth/me/phone` 供当前用户自行维护可选手机号（无短信验证）；管理员继续在用户管理维护 |
| 2026-07-13 | 注册安全与账号登录：`sys_user.email` 唯一约束（`V025`，重复邮箱阻断迁移）、注册邮箱验证码（`V026` + `POST /api/auth/register/verification-code`）、账号/邮箱登录、本人完整邮箱展示、账号/显示名称禁止空白 |
| 2026-07-13 | 内网容器访问与 MinIO 上传修复：Compose 显式 `container_name` + hostname（`mysql`/`minio`/`backend`/`frontend`）；生产 compose 开放 MySQL 3307、MinIO API 9000、Console 9001；backend/MinIO 凭据对齐与健康检查增强；移除 cleanup 脚本删卷能力，数据库/对象存储仅 DBeaver/Console 手工维护 |
| 2026-07-13 | 邮箱统一小写：用户与客户邮箱写入时 `trim + lowercase`（`Locale.ROOT`）；Flyway `V024` 归一化历史 `sys_user.email` / `sys_customer.email`；前端用户管理、客户表单与注册/忘记密码提交同步规范化 |
| 2026-07-13 | 第四十一期内网上传逻辑重写：修正 Spring Boot 3.3 `spring.servlet.multipart` 配置绑定，Nginx 请求体上限对齐 `UPLOAD_MAX_REQUEST_SIZE_BYTES` 并关闭请求缓冲；通用附件取消 MIME/魔数/并发限制，BOM 图片与 Excel 导入保留业务校验；前端上传取消固定超时并增强 413/网关错误诊断 |
| 2026-07-13 | 修复分组 SUB 导航：无 permission 的子菜单（如配置管理）在侧栏作为容器展示已授权子页面；菜单校验与菜单管理表单支持新建纯分组 SUB |
| 2026-07-13 | 第四十期个人中心与权限树：Flyway `V022` 菜单类型统一为 TOP/SUB/BUTTON 并重排写权限层级、恢复个人中心为侧栏首位；`V023` 新增邮箱验证码表；登录态原密码/验证码改密 API 与 `token_version` 全会话 JWT 失效；角色授权仅向上继承；前端 `PersonalCenterView`、菜单/角色授权树改造 |
| 2026-07-13 | 第三十九期菜单/上传/台账优化：Flyway `V020` 将侧栏收敛为仓库管理+系统管理两个顶层（其他模块仅隐藏），仓库管理新增 `InboxOutlined` 图标；Nginx `client_max_body_size` 与后端上传默认上限对齐到 5GB，前端移除文件大小预检；出入库后返回台账自动刷新；非空单价最后提交覆盖台账；台账新增 `last_operated_at` 并按最后出入库时间倒序 |
| 2026-07-10 | CRUD 筛选布局、页签缓存与空值筛选：各业务列表页统一响应式筛选栅格（桌面四列/平板两列/移动单列）；工作台为每个已打开页签维持独立 `KeepAlive`，切换保留查询/分页/表格状态，右键刷新或关闭页签才销毁对应缓存；仓库域“不限”筛选统一为空值语义（`placeholder="全部"` + `allow-clear`，不再把“全部”写入请求参数）；后端 QueryBuilder 移除“全部”/ALL 特判 |
| 2026-07-10 | 导航页签交互增强：顶部 TabBar 移除独立“清空全部”按钮，改为页签右键菜单（刷新、关闭左侧/右侧/其它、清空全部）；支持桌面端拖拽调整页签顺序；页签记录 `fullPath` 并在切换时恢复查询参数 |
| 2026-07-10 | 菜单层级与默认页优化：Flyway `V013__optimize_navigation_hierarchy.sql` 将库存统计置顶、系统管理用户/角色/菜单改为独立侧栏菜单；前端默认页优先库存统计，旧 `/system/users/roles`、`/system/users/menus` 重定向到新路径 |
| 2026-07-08 | 新增经验库独立域：后端 `com.storage.experience.*`、前端 `views/experience`/`api/experience`/`types/experience`，落地经验类型与记录 CRUD、项目标签、附件上传/下载、Excel 导入导出；新增 Flyway `V008__experience_library.sql` 与 `platform:experience:write` 权限 |
| 2026-07-08 | 补齐 `feat/knowledge-base` worktree 数据库隔离注册表：Windows/Bash 脚本新增 MySQL 3312、MinIO 9050、独立 compose project/卷，并同步扩展默认上传 MIME 白名单 |
| 2026-07-08 | 补充 `feat/design-guidelines` worktree 数据库隔离注册表：PowerShell/Bash 同步新增 3313/9060 端口与 `storage-design-guidelines` Compose/容器/卷名，README worktree 表同步 |
| 2026-07-08 | 新增独立设计指引模块：`com.storage.design.*` 后端域、`views/design` 前端页、产品类型/阶段配置、设计指引 CRUD/Excel、`V009__add_design_guidelines.sql` Flyway 表结构与菜单权限迁移；README/AGENTS 同步新域边界 |
| 2026-07-08 | 后端 DTO/Entity/VO 映射接入 MapStruct：替换客户与仓库 converter 中手写 `new + setter` 字段搬运，抽取 `StringMapping` 处理 trim/空值规则，并在 AGENTS 写入禁止手写 DTO2Entity 搬运的门禁 |
| 2026-07-08 | 仓库后端包结构瘦身：移除 `warehouse` 下 bin/bom/io/ledger/safety/stats 二次业务分包，收敛为 controller/service/mapper/dto/entity/excel 等按层目录；台账导出与引用计数并回 `MaterialLedgerService`，简单计数改用 MyBatis-Plus Wrapper |
| 2026-07-08 | README/AGENTS 补充发布 tag 规则：正式发布 tag 使用 `vMAJOR.MINOR.PATCH`（如 `v1.0.0`），打 tag 前需本地跑通 `docker-compose.yml` 服务与健康检查 |
| 2026-07-08 | 增加 Flyway 迁移历史防回归测试：锁定已发布 `V001__baseline_schema.sql` 的 checksum，并校验台账自然键约束保留在 `V007` 增量迁移 |
| 2026-07-08 | 修复 Flyway 历史迁移 checksum mismatch：恢复已发布 `V001__baseline_schema.sql`，保留 `V007` 承接台账自然键约束；README/AGENTS 补充已有数据环境先备份、禁止清卷重建、禁止修改已应用迁移的升级流程 |
| 2026-07-07 | 出入库 Excel 导入跟齐业务图：入库导入按物料清单 + Bin 位校验配置，缺台账时自动创建后写入流水；台账前端写 API 与旧表单组件清理，`useExcelImportExport` 支持纯导出页面并复用采购清单下载 |
| 2026-07-07 | 按业务逻辑图补全安全库存采购闭环：安全库存页新增“生成采购清单”，后端新增 `GET /api/safety-stock/purchase-list/export`，建议采购数按安全库存数减当前库存计算；外部台账写 API 收口为只读查询/导出，新增 `V007` 台账自然键唯一约束并对历史重复数据 fail-fast |
| 2026-07-07 | README 补充仓库业务流程图解读：配置管理维护 Bin 位与物料清单，入库基于配置写入台账，出库从台账领用，安全库存预警联动采购清单 |
| 2026-07-07 | 物料台账只读化与入库配置选择：台账页隐藏新增/导入/模板/编辑/删除入口；批量入库改从物料清单/BOM 选择物料、从 Bin 位管理按编号/排/列/层选择位置；后端入库校验 Bin 位存在并按 BOM+Bin 创建/累加台账 |
| 2026-07-07 | 动态路由组件路径收口：移除前端人工组件映射表，`sys_menu.component_key` 改为菜单维护的 `views/` / `components/` 模块路径，新增 Flyway `V005` 迁移旧短 key，菜单管理文案同步为“组件路径” |
| 2026-07-07 | MinIO/CORS 部署修复：Compose MinIO command 改为 `server --address 0.0.0.0:9000 /data`；移除后端全局 `CorsFilter` 与 `CORS_ALLOWED_ORIGINS`，统一通过 Nginx/Vite proxy 同源转发 `/api/**`，避免 `Invalid CORS request` |
| 2026-07-07 | Compose/Nginx 部署收口：backend 改用 service 级 `env_file: .env` 注入配置，仅保留容器网络覆盖；Nginx 增加 gzip、静态资源缓存、`index.html` no-cache、SPA fallback 与代理超时/转发头 |
| 2026-07-07 | Linux 适配与文档收口：Bash 脚本提交可执行位并固定 LF；`dev-up.sh` 默认不再隐式 `--build`；README 补齐 Linux/macOS/Git Bash 自检、清理旧容器与部署核验；AGENTS 增加脚本跨平台门禁 |
| 2026-07-07 | Docker 部署细节收口：dev compose 覆盖 frontend 端口避免同时暴露 80/5173；`APP_PUBLIC_BASE_URL` 默认回归 base Nginx 入口，dev profile 单独覆盖为 `FRONTEND_PORT` |
| 2026-07-07 | Docker 启动口径收敛：README 与 Windows wrapper 默认改为 `docker compose up -d`，`--build`/`-Build` 仅保留为显式可选重建；本地前端 `npm install` 改为首次或锁文件变更时执行 |
| 2026-07-07 | Service 接口隔离收口：导入/导出、JWT、密码重置邮件、出入库读模型与台账引用查询服务统一改为 `*Service` 接口 + `*ServiceImpl` 实现；调用方保持依赖接口，后端 98 项测试通过 |
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
| 2026-06-25 | AGENTS：仓库同类 CRUD 20% 复用门禁与协作式 AI（补全式、可审查）原则 |
| 2026-06-25 | 第九期系统管理 RBAC 补全：恢复菜单管理 Tab（`/system/users/menus`）、`MenuManagePanel` CRUD、用户表单多角色分配 |
| 2026-06-25 | 第十期 CRUD 复用层：`usePaginatedCrudList`、`useExcelImportExport`、`useLinkedFilterOptions`、`CrudListPage`、`CrudToolbar`；物料台账/用户/角色列表页迁移至公共层 |
| 2026-06-25 | 第十一期 Bin位管理：`warehouse_bin` 表与 CRUD/Excel API、`BinManageView`；台账 Bin 下拉改读主数据、保存/导入校验；`GET /api/materials/bin-codes` |
| 2026-06-25 | 第十一期 11.2 物料清单：`warehouse_bom` 表与 CRUD/Excel API、`BomManageView`（品类联动筛选；图片列占位；从台账 DISTINCT 回填种子） |
| 2026-06-25 | 第十一期 11.3 台账 ↔ 清单：`GET /api/materials/bom-catalog`、表单「从清单选择」、四元组严格校验、清单删除台账引用保护 |
| 2026-06-25 | 第十一期 11.4 物料清单 MinIO 图片：`image_object_key` 字段、`BomFormModal` 上传/预览/清除、列表与详情缩略图；上传权限 OR `warehouse:bom:write` |
| 2026-06-25 | 第十二期物料台账编辑链路：修复 `warehouse_bom.image_object_key` 迁移（MySQL 兼容）；编辑弹窗独立加载选项与 bom-catalog；`MaterialLedgerView` 写权限门禁对齐 Bin/用户管理 |
| 2026-06-25 | 第十三期 CRUD 复用层补全：`confirmBatchDelete`、`useWritePermission`、`useCrudDetailDrawer`、`CrudDetailDrawer`、`CrudRowActions`、`getTableRowIndex`；台账/Bin/清单三页迁移；台账 Bin 筛选改走 `filter-options.binLocations` |
| 2026-06-25 | 第十三期物料出入库：`material_io_record` 表与 CRUD/Excel API、`MaterialIoView` 批量入库/出库、`MaterialLedgerPickerModal`、`warehouse:material-io:write`；`material_ledger.stock_quantity` 唯一由出入库模块写入 |
| 2026-06-26 | 第十四期物料出入库完善：筛选联动 bug 修复、Excel 原子导入 `importBatch`、台账选择器 composables 升级、批量表单库存列/实时库存、台账删除 IO 引用保护、`CrudToolbar` 模板只读下载 |
| 2026-06-26 | 第十五期物料出入库优化：H2 集成测试、`useMaterialIoStock`、可用库存列、编辑禁改类型、出库选择器零库存拦截、`materialLedgerId` 深链追溯、台账详情跳转出入库、删除占位页 |
| 2026-06-26 | 第十六期物料出入库复用与契约：`useWarehouseMaterialFilters`/`WarehouseMaterialFilterPanel` 消除三处筛选重复、`useMaterialLedgerDeepLink`、`MaterialIdentityDescriptions`、出库 InputNumber max、IO↔台账互跳、台账只读模板下载；后端禁改 ioType、批量重复物料拦截、`MaterialLedgerService.findByMaterialKey` |
| 2026-06-26 | 第十七期追溯闭环与测试：`useMaterialLedgerRouteDetail` 台账 `?materialLedgerId=` 自动打开详情；`importBatch`/Excel 导入重复物料拦截；`MaterialIoImportServiceIntegrationTest` + Service 层 ioType/查询/重复测试扩展 |
| 2026-06-26 | 第十八期追溯体验与复用深化：深链 `?materialLedgerId=` 新增预填、上下文条补全、IO↔台账读权限对称；编辑禁改物料（前后端）；`warehouseMaterialTable`/`materialLedgerRouteQuery`/`useMaterialLedgerList`/`MaterialIoDetailDescriptions`；台账页与选择器共用列表 composable |
| 2026-06-26 | 第十九期流水深链与质量基建：`useMaterialIoRouteDetail` + `?id=` 打开出入库详情；`MaterialStockMutationService` 库存逻辑下沉；Vitest 基建 + `useMaterialIoStock` 单测 |
| 2026-06-26 | 第二十期列表复用与深链闭环：`useMaterialIoList` 抽取瘦身 `MaterialIoView`；`?id=` 列表 `ids` 定位 + 查看 URL 同步；Excel 导入出库超库存行级 `ImportResultVO`；`MaterialStockMutationServiceTest` + 深链/route Vitest 扩展 |
| 2026-06-26 | 第二十一期追溯快捷与契约瘦身：上下文条一键新增入库/出库、`useCrudRouteDetail`、`MaterialIoUpdateDTO`、详情复制链接 |
| 2026-06-26 | 第二十二期安全库存管理：`safety_stock` 表与 API、`SafetyStockView`、预警黄行、导出/编辑 upsert、`warehouse:safety-stock:write`、H2 集成测试 |
| 2026-06-29 | 第二十三期出入库业务语义：`purpose` 用途枚举、出库必填、列表筛选/Excel；`GET /api/material-io/safety-hints` 出库预警；新增补录 `operatedAt`；`useMaterialIoSafetyHint` |
| 2026-06-29 | 第二十六期客户管理与忘记密码：`sys_customer` CRUD/Excel、`CustomerManageView`、`POST /api/auth/forgot-password`、登录页 `?tab=forgot` |
| 2026-06-29 | 第二十四期出入库 UI 优化：`MaterialIoFilterPanel`、`MaterialIoContextBar`、工具栏「更多」、用途 Tag 列、新增弹窗顶栏/条件列/预警列、安全确认 checkbox |
| 2026-06-29 | 第二十七期差距收敛：文档与客户占位表述同步、台账路由读权限、死代码清理、注册可选邮箱 + admin 种子邮箱、库存统计 `recentDays` 选择器、台账/Bin/客户 import 集成测试、GitHub Actions CI |
| 2026-06-29 | 第二十八期开发与调试规范：AGENTS/README 环境变量门禁、IDEA 断点工作流、双端校验 checklist、断点表；`BACKEND_PORT` / `FRONTEND_PORT` / `VITE_API_PROXY` 参数化，脚本保留应用端口并读取 MySQL 环境变量 |
| 2026-06-29 | 第二十九期质量门禁与安全加固：Docker/CORS 凭据参数化、迁移关闭 `continue-on-error` 并改条件 DDL、忘记密码统一提示与限流、Auth Controller 集成测试、CI 增加前端 build、路由懒加载 + AntD 组件按需导入、清理 Vite 脚手架残留 |
| 2026-06-30 | 第三十期忘记密码邮件链接重置：Google SMTP 环境变量预留、`password_reset_token`、`POST /api/auth/reset-password`、登录页 `?tab=reset&token=...` |
| 2026-06-30 | 第三十一期模块复用复查与废弃物清理：复核仓库/系统同类列表页公共层复用，移除未引用 `_shared/warehouseListScaffold.ts` 注释脚手架，文档改记录真实公共模块 |
| 2026-06-30 | 第三十二期安全门禁复查与上传加固：复核环境变量、CORS、Session、忘记密码、迁移脚本与敏感信息门禁；文件上传增加 JPG/PNG/WebP/GIF 魔数校验 |
| 2026-06-30 | 第三十三期系统环境变量一致性审核：核对 `.env.example` 与 `Format-WorktreeEnvContent` 变量清单一致；`start-dev.ps1` 显式注入 Session/Admin/Upload/Mail/PasswordReset 环境变量；补齐文档 SSOT |
| 2026-06-30 | 第三十四期跨平台 DevX：补齐 Linux/macOS/Git Bash 版 `worktree-db.sh`、`sync-worktree-env.sh`、`dev-up.sh`、`start-dev.sh`、`wait-mysql.sh`、`reset-db.sh`、`health-check.sh`、`cleanup-legacy-docker.sh`，README/AGENTS 同步双平台启动入口 |
| 2026-06-30 | 精简 AGENTS：仅保留代理准则与质量门禁，启动说明留 README，历史流水迁移到 CHANGELOG |
| 2026-06-30 | 整理后续架构优化方案：Pinia、动态路由、样式预处理器、Service 接口化与模块化原则 |
| 2026-06-30 | 明确鉴权优化目标为 Shiro + JWT，当前 Shiro Session Cookie 仅作为待迁移遗留实现记录 |
| 2026-06-30 | 第三十五期 Pinia + JWT 鉴权迁移：前端引入 Pinia auth store 与 localStorage access token，后端改为 Shiro + JWT Bearer token 认证，配置 SSOT 同步 `JWT_SECRET` / `JWT_TTL_MINUTES` |
| 2026-06-30 | 第三十六期动态菜单与动态路由：`sys_menu.component_key` 契约、导航树返回组件 Key、前端 Pinia menu store 动态 `addRoute`、菜单管理维护组件 Key |
| 2026-07-06 | Docker 部署路线纠偏：新增 `docker-compose-dev.yml`、`deploy-cli.ps1/.sh`、后端/前端 Dockerfile 与 Nginx 反代配置；移除 reset-db 脚本；README/ROADMAP 改为 `docker compose up -d` 主路径并明确禁止日常 `down -v` 清卷升级 |
| 2026-07-06 | 第三十七期样式预处理器统一：前端引入 Less 依赖；新增 `frontend/src/styles/` 公共变量与 mixins；全局 `style.less` 入口；迁移 `AppLayout`、侧栏/TabBar、CrudListPage/CrudToolbar/CrudDetailDrawer 及 `MaterialLedgerView`、`UserManageView` 代表页 |
| 2026-07-06 | P6 Service 接口化试点（第一阶段）：`SafetyStockService` 抽取为接口，`SafetyStockServiceImpl` 承接实现；`SafetyStockController`、`WarehouseStatsService` 依赖接口；`SafetyStockExportService` 保持独立 |
| 2026-07-06 | P6 Service 接口化试点（第二阶段）：`MaterialStockMutationService` 抽取为接口，`MaterialStockMutationServiceImpl` 承接实现；`MaterialIoService`、`MaterialIoImportService` 依赖接口；`ImportStockSimulationRow` 保留在接口契约 |
| 2026-07-06 | P6 Service 接口化试点（第三阶段）：`MaterialLedgerService` 抽取为接口，`MaterialLedgerServiceImpl` 承接实现；`MaterialLedgerController`、`MaterialIoService`、`MaterialIoImportService`、`SafetyStockServiceImpl` 依赖接口；`MaterialLedgerImportService`、`MaterialLedgerExportService` 保持独立 |
| 2026-07-06 | P6 Service 接口化试点（第四阶段）：`MaterialIoService` 抽取为接口，`MaterialIoServiceImpl` 承接实现；`MaterialIoController`、`MaterialIoImportService` 依赖接口；`importBatch` 保留在接口契约；`MaterialIoImportService`、`MaterialIoExportService`、`MaterialStockMutationService` 保持独立 |
| 2026-07-06 | P6 Service 接口化试点（第五阶段）：`AuthService` 抽取为接口，`AuthServiceImpl` 承接实现；`AuthController`、`FileController`、`SysMenuService`、`SysUserService`、`MaterialIoServiceImpl` 依赖接口；`currentUser()` 保留在接口契约；`PasswordResetMailService`、`JwtService`、`UserRealm` 保持独立 |
| 2026-07-06 | P8 Flyway 正式接管数据库版本管理：引入 `flyway-core`/`flyway-mysql`、关闭 `spring.sql.init` 主路径、`V001__baseline_schema.sql` baseline、Compose 移除 initdb schema 挂载、CI 增加 MySQL Flyway migrate/validate job；历史 `migration-*.sql`/`schema.sql` 降级为参考快照 |
| 2026-07-06 | ROADMAP 审计与修正（对照聊天记录）：P5/P6 顶层状态改为 `[~]` 并补充量化完成度（样式 8/27 页已迁 Less、Service 5/29 已接口化）；P4 备注 `/system/users` 子路由硬编码例外；新增 P9 业务域模块拆分、P10 Excel 声明式框架评估；维护规则增加持续死代码清理 |
| 2026-07-06 | P9 业务域模块拆分（Phase 0–3）：单 JAR 内按域拆包路线冻结；`@MapperScan` 扩展；抽取 `com.storage.common.*`；迁移 `com.storage.system.customer.*` 垂直切片；前端 `MaterialLedgerView` 归并 `views/warehouse/`、`api/warehouse`/`types/warehouse` 目录 + re-export shim |
| 2026-07-06 | P5 样式预处理器收敛：27/27 带 `<style>` 的 `.vue` 全部迁移 `lang="less"`；补充登录页品牌色 token（`@color-login-*`）与 `@color-border-info`；仓库业务页/弹窗/系统面板复用 `variables.less`/`mixins.less` |
| 2026-07-06 | P6 Service 接口化收尾（第八阶段）：`WarehouseStatsService`、`system.customer.SysCustomerService` 抽取接口 + `*ServiceImpl`；主业务 Service 接口化共 12 个；Import/Export/基础设施类保持独立 |
| 2026-07-06 | P9 业务域模块拆分续作：ROADMAP 补齐 Phase 4–11 全量待办；仓库域 `warehouse.bin.*` 起逐批垂直切片迁移；系统域/基础设施/前端 shim 按依赖顺序收口 |
| 2026-07-06 | P9 业务域模块拆分完成（Phase 4–11）：仓库域 `warehouse.{bin,bom,ledger,shared,safety,stats,io}.*`、系统域 `system.{role,user,menu,auth}.*`、`infrastructure.file.*`；`OperatorResolver` 跨域契约；前端 domain import 收敛；71 后端集成测试通过 |
| 2026-07-06 | P9 配置包收口（Phase 12）：根级 `com.storage.config` 迁移至 `common.config`（CORS/WebMvc/MyBatis/Password）、`system.auth.config`（JWT/AdminPasswordInitializer）、`infrastructure.file.config`（MinIO/上传）；71 后端测试通过 |
| 2026-07-06 | P10 Excel 声明式框架评估与试点：`easyexcel:4.0.3` 引入；对比 EasyExcel/EasyPOI/POI 结论写入 ROADMAP；`SafetyStockExportService` 迁 EasyExcel + `SafetyStockExportRow` + `ExcelExportStyleHandlers`；新增 `SafetyStockExportServiceTest`；73 后端测试通过；导入路径暂保留 POI |
| 2026-07-06 | P10 导出迁移第二批：`ExcelExportWriter` 公共写出层；`WarehouseBinExportService`、`SysRoleExportService`、`SysCustomerExportService` 迁 EasyExcel + `*ExportRow` + 行为测试；导出进度 4/8；80 后端测试通过 |
| 2026-07-06 | P10 导出迁移第三批：`WarehouseBomExportService`、`MaterialLedgerExportService` 迁 EasyExcel + `*ExportRow` + 行为测试；保留 `exportTemplate()` 与单价格式；导出进度 6/8；85 后端测试通过 |
| 2026-07-06 | P10 导出迁移第四批（收尾）：`SysUserExportService`、`MaterialIoExportService` 迁 EasyExcel + `*ExportRow`；新增 `MaterialIoImportTemplateRow` 承载导入模板表头；导出 8/8 完成；92 后端测试通过；导入路径继续保留 POI |
| 2026-07-06 | P10 查漏补缺：修正 `MaterialIoImportService` 与 EasyExcel 导入模板列序不一致（`操作时间` index 11）；新增 `MaterialIoImportTemplateColumn` 与导入模板回归测试；93 后端测试通过 |
| 2026-07-06 | P1–P10 缺漏收敛：Compose `backend` healthcheck（`GET /health`）+ `frontend` 依赖 `service_healthy`；JWT `jti` 黑名单与登出撤销；动态路由移除系统管理硬编码子路由（`nav-tree.visible` + 菜单嵌套）；Pinia store 测试补全；Flyway `V002__system_menu_nested_routes_and_jwt_revocation.sql` |
| 2026-07-06 | 导航修复：删除冗余 `项目中心读` 隐藏根菜单（`V003__remove_redundant_project_read_menu.sql`）；nav-tree 仅允许隐藏子路由参与动态注册，避免动作/占位权限泄漏到侧栏；重建 dev Compose 后 backend/frontend 均恢复 |
