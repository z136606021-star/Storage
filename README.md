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

将自动创建 `storage` 数据库并导入 [backend/src/main/resources/db/schema.sql](backend/src/main/resources/db/schema.sql) 中的表结构与种子数据。

默认连接信息见 [.env.example](.env.example)：

- Host: `localhost:3306`
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
- [ ] 导出 Excel（占位）
- [ ] 查看详情（占位）
- [ ] 物料出入库、安全库存管理

## 远端仓库

https://github.com/z136606021-star/Storage.git

协作者与 AI 代理请参阅 [AGENTS.md](AGENTS.md)。
