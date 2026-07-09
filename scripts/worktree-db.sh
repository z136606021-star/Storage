#!/usr/bin/env bash

# Worktree database isolation registry (SSOT for Bash scripts).

set -euo pipefail

worktree_profile_field() {
  local branch="${1:?branch is required}"
  local field="${2:?field is required}"

  case "$branch" in
    main)
      case "$field" in
        branch) echo "main" ;;
        slug) echo "main" ;;
        worktree_path) echo "E:/Storage" ;;
        compose_project_name) echo "storage-main" ;;
        mysql_port) echo "3307" ;;
        minio_port) echo "9000" ;;
        mysql_container) echo "storage-main-mysql" ;;
        minio_container) echo "storage-main-minio" ;;
        mysql_volume) echo "storage-main_mysql_data" ;;
        minio_volume) echo "storage-main_minio_data" ;;
        *) return 1 ;;
      esac
      ;;
    feat/material-ledger)
      case "$field" in
        branch) echo "feat/material-ledger" ;;
        slug) echo "material-ledger" ;;
        worktree_path) echo "E:/Storage-worktrees/material-ledger" ;;
        compose_project_name) echo "storage-material-ledger" ;;
        mysql_port) echo "3308" ;;
        minio_port) echo "9010" ;;
        mysql_container) echo "storage-material-ledger-mysql" ;;
        minio_container) echo "storage-material-ledger-minio" ;;
        mysql_volume) echo "storage-material-ledger_mysql_data" ;;
        minio_volume) echo "storage-material-ledger_minio_data" ;;
        *) return 1 ;;
      esac
      ;;
    feat/material-io)
      case "$field" in
        branch) echo "feat/material-io" ;;
        slug) echo "material-io" ;;
        worktree_path) echo "E:/Storage-worktrees/material-io" ;;
        compose_project_name) echo "storage-material-io" ;;
        mysql_port) echo "3309" ;;
        minio_port) echo "9020" ;;
        mysql_container) echo "storage-material-io-mysql" ;;
        minio_container) echo "storage-material-io-minio" ;;
        mysql_volume) echo "storage-material-io_mysql_data" ;;
        minio_volume) echo "storage-material-io_minio_data" ;;
        *) return 1 ;;
      esac
      ;;
    feat/safety-stock)
      case "$field" in
        branch) echo "feat/safety-stock" ;;
        slug) echo "safety-stock" ;;
        worktree_path) echo "E:/Storage-worktrees/safety-stock" ;;
        compose_project_name) echo "storage-safety-stock" ;;
        mysql_port) echo "3310" ;;
        minio_port) echo "9030" ;;
        mysql_container) echo "storage-safety-stock-mysql" ;;
        minio_container) echo "storage-safety-stock-minio" ;;
        mysql_volume) echo "storage-safety-stock_mysql_data" ;;
        minio_volume) echo "storage-safety-stock_minio_data" ;;
        *) return 1 ;;
      esac
      ;;
    feat/config-mgmt)
      case "$field" in
        branch) echo "feat/config-mgmt" ;;
        slug) echo "config-mgmt" ;;
        worktree_path) echo "E:/Storage-worktrees/config-mgmt" ;;
        compose_project_name) echo "storage-config-mgmt" ;;
        mysql_port) echo "3311" ;;
        minio_port) echo "9040" ;;
        mysql_container) echo "storage-config-mgmt-mysql" ;;
        minio_container) echo "storage-config-mgmt-minio" ;;
        mysql_volume) echo "storage-config-mgmt_mysql_data" ;;
        minio_volume) echo "storage-config-mgmt_minio_data" ;;
        *) return 1 ;;
      esac
      ;;
    feat/knowledge-base)
      case "$field" in
        branch) echo "feat/knowledge-base" ;;
        slug) echo "knowledge-base" ;;
        worktree_path) echo "E:/Storage-worktrees/knowledge-base" ;;
        compose_project_name) echo "storage-knowledge-base" ;;
        mysql_port) echo "3312" ;;
        minio_port) echo "9050" ;;
        mysql_container) echo "storage-knowledge-base-mysql" ;;
        minio_container) echo "storage-knowledge-base-minio" ;;
        mysql_volume) echo "storage-knowledge-base_mysql_data" ;;
        minio_volume) echo "storage-knowledge-base_minio_data" ;;
        *) return 1 ;;
      esac
      ;;
    feat/design-guidelines)
      case "$field" in
        branch) echo "feat/design-guidelines" ;;
        slug) echo "design-guidelines" ;;
        worktree_path) echo "E:/Storage-worktrees/design-guidelines" ;;
        compose_project_name) echo "storage-design-guidelines" ;;
        mysql_port) echo "3313" ;;
        minio_port) echo "9060" ;;
        mysql_container) echo "storage-design-guidelines-mysql" ;;
        minio_container) echo "storage-design-guidelines-minio" ;;
        mysql_volume) echo "storage-design-guidelines_mysql_data" ;;
        minio_volume) echo "storage-design-guidelines_minio_data" ;;
        *) return 1 ;;
      esac
      ;;
    *)
      echo "Unknown branch '$branch'. Known branches: main, feat/material-ledger, feat/material-io, feat/safety-stock, feat/config-mgmt, feat/knowledge-base, feat/design-guidelines" >&2
      return 1
      ;;
  esac
}

repo_root_from_script() {
  local script_dir
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  cd "$script_dir/.." && pwd
}

current_branch_name() {
  local repo_root="${1:?repo root is required}"
  local branch
  branch="$(git -C "$repo_root" branch --show-current 2>/dev/null || true)"
  if [[ -z "$branch" ]]; then
    echo "Not a git repository or detached HEAD: $repo_root" >&2
    return 1
  fi
  echo "$branch"
}

load_current_worktree_profile() {
  local repo_root="${1:?repo root is required}"
  WORKTREE_BRANCH="$(current_branch_name "$repo_root")"
  WORKTREE_SLUG="$(worktree_profile_field "$WORKTREE_BRANCH" slug)"
  WORKTREE_PATH="$(worktree_profile_field "$WORKTREE_BRANCH" worktree_path)"
  COMPOSE_PROJECT_NAME="$(worktree_profile_field "$WORKTREE_BRANCH" compose_project_name)"
  STORAGE_MYSQL_PORT="$(worktree_profile_field "$WORKTREE_BRANCH" mysql_port)"
  STORAGE_MINIO_PORT="$(worktree_profile_field "$WORKTREE_BRANCH" minio_port)"
  STORAGE_MYSQL_CONTAINER="$(worktree_profile_field "$WORKTREE_BRANCH" mysql_container)"
  STORAGE_MINIO_CONTAINER="$(worktree_profile_field "$WORKTREE_BRANCH" minio_container)"
  STORAGE_MYSQL_VOLUME="$(worktree_profile_field "$WORKTREE_BRANCH" mysql_volume)"
  STORAGE_MINIO_VOLUME="$(worktree_profile_field "$WORKTREE_BRANCH" minio_volume)"

  local normalized_root normalized_expected
  normalized_root="$(cd "$repo_root" && pwd -P)"
  normalized_expected="$WORKTREE_PATH"
  if [[ "$normalized_expected" == /* ]] && [[ "$normalized_root" != "$normalized_expected" ]]; then
    echo "Warning: current path ($normalized_root) does not match registered worktree for branch '$WORKTREE_BRANCH' ($normalized_expected). Using branch profile anyway." >&2
  fi
}

read_dotenv_value() {
  local env_path="${1:?env path is required}"
  local name="${2:?name is required}"

  [[ -f "$env_path" ]] || return 0
  awk -F= -v key="$name" '
    /^[[:space:]]*#/ { next }
    /^[[:space:]]*$/ { next }
    {
      lhs=$1
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", lhs)
      if (lhs == key) {
        sub(/^[^=]*=/, "")
        gsub(/^[[:space:]]+|[[:space:]]+$/, "")
        print
        exit
      }
    }
  ' "$env_path"
}

env_or_existing() {
  local name="${1:?name is required}"
  local default_value="${2-}"
  local env_path="${3:?env path is required}"
  local current_value="${!name-}"

  if [[ -n "$current_value" ]]; then
    echo "$current_value"
    return
  fi

  local existing_value
  existing_value="$(read_dotenv_value "$env_path" "$name")"
  if [[ -n "$existing_value" ]]; then
    echo "$existing_value"
    return
  fi

  echo "$default_value"
}

write_worktree_env_file() {
  local repo_root="${1:?repo root is required}"
  load_current_worktree_profile "$repo_root"

  local env_path="$repo_root/.env"
  local mysql_db mysql_user mysql_password mysql_root_password
  local minio_access_key minio_secret_key minio_bucket
  local backend_port frontend_port vite_api_proxy
  local session_cookie_http_only session_cookie_secure reset_admin_password_on_startup
  local jwt_secret jwt_ttl_minutes
  local upload_max_size_bytes upload_allowed_content_types
  local app_public_base_url password_reset_token_ttl_minutes
  local mail_host mail_port mail_username mail_password mail_from mail_smtp_auth mail_smtp_starttls_enable

  mysql_db="$(env_or_existing MYSQL_DB storage "$env_path")"
  mysql_user="$(env_or_existing MYSQL_USER storage "$env_path")"
  mysql_password="$(env_or_existing MYSQL_PASSWORD storage123 "$env_path")"
  mysql_root_password="$(env_or_existing MYSQL_ROOT_PASSWORD root123 "$env_path")"
  minio_access_key="$(env_or_existing MINIO_ACCESS_KEY minioadmin "$env_path")"
  minio_secret_key="$(env_or_existing MINIO_SECRET_KEY minioadmin123 "$env_path")"
  minio_bucket="$(env_or_existing MINIO_BUCKET storage "$env_path")"
  backend_port="$(env_or_existing BACKEND_PORT 8080 "$env_path")"
  frontend_port="$(env_or_existing FRONTEND_PORT 5173 "$env_path")"
  vite_api_proxy="$(env_or_existing VITE_API_PROXY "http://localhost:$backend_port" "$env_path")"
  session_cookie_http_only="$(env_or_existing SESSION_COOKIE_HTTP_ONLY true "$env_path")"
  session_cookie_secure="$(env_or_existing SESSION_COOKIE_SECURE false "$env_path")"
  reset_admin_password_on_startup="$(env_or_existing RESET_ADMIN_PASSWORD_ON_STARTUP true "$env_path")"
  jwt_secret="$(env_or_existing JWT_SECRET "dev-only-change-this-jwt-secret-at-least-32-bytes" "$env_path")"
  jwt_ttl_minutes="$(env_or_existing JWT_TTL_MINUTES 120 "$env_path")"
  upload_max_size_bytes="$(env_or_existing UPLOAD_MAX_SIZE_BYTES 5242880 "$env_path")"
  upload_allowed_content_types="$(env_or_existing UPLOAD_ALLOWED_CONTENT_TYPES "image/jpeg,image/png,image/webp,image/gif,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,text/plain" "$env_path")"
  app_public_base_url="$(env_or_existing APP_PUBLIC_BASE_URL "http://localhost" "$env_path")"
  if [[ "$app_public_base_url" == "http://localhost:$frontend_port" ]]; then
    app_public_base_url="http://localhost"
  fi
  password_reset_token_ttl_minutes="$(env_or_existing PASSWORD_RESET_TOKEN_TTL_MINUTES 30 "$env_path")"
  mail_host="$(env_or_existing MAIL_HOST smtp.gmail.com "$env_path")"
  mail_port="$(env_or_existing MAIL_PORT 587 "$env_path")"
  mail_username="$(env_or_existing MAIL_USERNAME "" "$env_path")"
  mail_password="$(env_or_existing MAIL_PASSWORD "" "$env_path")"
  mail_from="$(env_or_existing MAIL_FROM "" "$env_path")"
  mail_smtp_auth="$(env_or_existing MAIL_SMTP_AUTH true "$env_path")"
  mail_smtp_starttls_enable="$(env_or_existing MAIL_SMTP_STARTTLS_ENABLE true "$env_path")"

  cat > "$env_path" <<EOF
# Auto-generated by scripts/sync-worktree-env.sh - do not commit (.gitignore)
# Branch: $WORKTREE_BRANCH | Compose: $COMPOSE_PROJECT_NAME
# Existing credentials, app ports, API proxy, JWT, upload, and mail values are preserved when this file is regenerated.

MYSQL_HOST=mysql
MYSQL_PORT=3306
MYSQL_DB=$mysql_db
MYSQL_USER=$mysql_user
MYSQL_PASSWORD=$mysql_password
MYSQL_ROOT_PASSWORD=$mysql_root_password

MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=$minio_access_key
MINIO_SECRET_KEY=$minio_secret_key
MINIO_BUCKET=$minio_bucket

BACKEND_PORT=$backend_port
FRONTEND_PORT=$frontend_port
APP_PORT=80
VITE_API_PROXY=$vite_api_proxy
SESSION_COOKIE_HTTP_ONLY=$session_cookie_http_only
SESSION_COOKIE_SECURE=$session_cookie_secure
RESET_ADMIN_PASSWORD_ON_STARTUP=$reset_admin_password_on_startup
JWT_SECRET=$jwt_secret
JWT_TTL_MINUTES=$jwt_ttl_minutes
UPLOAD_MAX_SIZE_BYTES=$upload_max_size_bytes
UPLOAD_ALLOWED_CONTENT_TYPES=$upload_allowed_content_types

APP_PUBLIC_BASE_URL=$app_public_base_url
PASSWORD_RESET_TOKEN_TTL_MINUTES=$password_reset_token_ttl_minutes
MAIL_HOST=$mail_host
MAIL_PORT=$mail_port
MAIL_USERNAME=$mail_username
MAIL_PASSWORD=$mail_password
MAIL_FROM=$mail_from
MAIL_SMTP_AUTH=$mail_smtp_auth
MAIL_SMTP_STARTTLS_ENABLE=$mail_smtp_starttls_enable

COMPOSE_PROJECT_NAME=$COMPOSE_PROJECT_NAME
STORAGE_MYSQL_PORT=$STORAGE_MYSQL_PORT
STORAGE_MINIO_PORT=$STORAGE_MINIO_PORT
STORAGE_MYSQL_CONTAINER=$STORAGE_MYSQL_CONTAINER
STORAGE_MINIO_CONTAINER=$STORAGE_MINIO_CONTAINER
STORAGE_MYSQL_VOLUME=$STORAGE_MYSQL_VOLUME
STORAGE_MINIO_VOLUME=$STORAGE_MINIO_VOLUME
EOF

  echo "$env_path"
}

import_worktree_env_file() {
  local repo_root="${1:?repo root is required}"
  local env_path="$repo_root/.env"
  if [[ ! -f "$env_path" ]]; then
    echo ".env not found at $env_path. Run scripts/sync-worktree-env.sh first." >&2
    return 1
  fi

  local line name value
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line#"${line%%[![:space:]]*}"}"
    line="${line%"${line##*[![:space:]]}"}"
    [[ -z "$line" || "$line" == \#* ]] && continue
    [[ "$line" == *=* ]] || continue
    name="${line%%=*}"
    value="${line#*=}"
    name="${name%"${name##*[![:space:]]}"}"
    value="${value#"${value%%[![:space:]]*}"}"
    export "$name=$value"
  done < "$env_path"
}
