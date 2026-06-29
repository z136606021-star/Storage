/**
 * 仓库域分页 CRUD 列表页公共模块索引（第二十期）。
 *
 * 新页面优先组合以下模块，勿从零复制整页：
 * - `CrudListPage` + `CrudToolbar` — 筛选/表格/工具栏壳层
 * - `usePaginatedCrudList` — 分页、勾选、搜索
 * - `useExcelImportExport` — 导入/导出/模板
 * - `useWritePermission` — 写权限门禁（toolbar + 操作列 + 勾选）
 * - `useCrudDetailDrawer` + `CrudDetailDrawer` — 详情抽屉
 * - `CrudRowActions` — 查看/编辑/删除操作列
 * - `confirmDelete` / `confirmBatchDelete` — 单条/批量删除确认
 * - `getTableRowIndex` — 序号列
 * - `useWarehouseMaterialFilters` + `WarehouseMaterialFilterPanel` — 6 字段品类联动筛选（台账/出入库/选择器）
 * - `useMaterialLedgerList` — 台账分页列表（台账页 + 选择器共用）
 * - `useMaterialIoList` — 出入库分页列表（含 ioType/操作时间筛选）
 * - `useSafetyStockList` — 安全库存分页列表（含安全库存数/预警期筛选、黄行高亮）
 * - `warehouseMaterialTable` — 六字段 query 与物料身份列定义 SSOT
 * - `materialLedgerRouteQuery` — `?materialLedgerId=` 解析 SSOT
 * - `materialIoRouteQuery` — 出入库 `?id=` 解析 SSOT
 * - `useMaterialLedgerDeepLink` — 出入库 `?materialLedgerId=` 列表筛选深链与上下文条
 * - `useCrudRouteDetail` — 通用 `?queryKey=` 深链打开详情（IO/台账薄包装）
 * - `useMaterialIoRouteDetail` — 出入库 `?id=` 自动打开详情抽屉
 * - `useMaterialLedgerRouteDetail` — 台账 `?materialLedgerId=` 自动打开详情抽屉
 * - `MaterialIdentityDescriptions` — 详情抽屉物料身份六字段块
 * - `SafetyStockDetailDescriptions` — 安全库存详情专属块（库存/安全库存/预警/跳转）
 * - `MaterialLedgerPickerModal` — 从台账选择物料（出入库等）
 * - `useMaterialIoStock` — 批量出入库表单可用库存计算
 *
 * 追溯深链：
 * - 台账 → 出入库：`/warehouse/material-io?materialLedgerId={id}`
 * - 出入库 → 台账：`/warehouse/material-ledger?materialLedgerId={id}`
 * - 出入库流水详情：`/warehouse/material-io?id={recordId}`
 *
 * 业务差异仅保留：columns、筛选扩展字段、专属 FormModal、API 绑定。
 */
export {}
