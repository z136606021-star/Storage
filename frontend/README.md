# Frontend Workspace

前端工程基于 Vue 3 + TypeScript + Vite，作为仓库管理系统 Web UI 实现。

## 开发命令

```bash
cd frontend
npm ci
npm run dev
```

常用校验命令：

```bash
npm run test
npm run build
```

## 配置说明

- 开发代理读取根目录 `.env` 中的 `VITE_API_PROXY`
- Vite 开发端口读取根目录 `.env` 中的 `FRONTEND_PORT`
- `.env` 由根目录 `scripts/sync-worktree-env.ps1`（Windows）或 `scripts/sync-worktree-env.sh`（Linux/macOS/Git Bash）维护，切换分支/worktree 后需先执行同步

## 文档入口

项目总览、环境启动、安全门禁与工作流请以根目录 [`README.md`](../README.md) 与 [`AGENTS.md`](../AGENTS.md) 为准。
