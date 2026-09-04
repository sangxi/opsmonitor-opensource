#!/bin/bash
# OpsMonitor 数据库初始化脚本
# 用法：bash init-db.sh
# 需要目标机器上有 mysql 客户端

set -e

PROJECT_ROOT=$(cd "$(dirname "$0")/.." && pwd)
SQL_FILE="$PROJECT_ROOT/sql/wgcloud-MySQL.sql"

GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m'

print_ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
print_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
print_err()  { echo -e "${RED}[ERR]${NC} $1"; }

# 从 .env 读取配置
if [ -f "$(dirname "$0")/.env" ]; then
    export $(grep -v '^#' "$(dirname "$0")/.env" | xargs)
fi

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-3306}
DB_NAME=${DB_NAME:-wgcloud}
DB_USER=${DB_USER:-root}
DB_PASSWORD=${DB_PASSWORD:-123456}

echo "============================="
echo " OpsMonitor 数据库初始化"
echo "============================="
echo " 数据库地址: $DB_HOST:$DB_PORT"
echo " 数据库名:   $DB_NAME"
echo " 用户名:     $DB_USER"
echo " SQL 文件:   $SQL_FILE"
echo "============================="
echo ""

# 1. 检查 mysql 客户端
if ! command -v mysql &> /dev/null; then
    print_err "未找到 mysql 客户端"
    print_warn "CentOS/RHEL: yum install -y mysql"
    print_warn "Ubuntu/Debian: apt-get install -y mysql-client"
    exit 1
fi

# 2. 检查 SQL 文件
if [ ! -f "$SQL_FILE" ]; then
    print_err "SQL 文件不存在: $SQL_FILE"
    exit 1
fi

# 3. 创建数据库（用 root）
print_ok "创建数据库 $DB_NAME ..."
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" \
    -e "CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 4. 导入表结构
print_ok "导入表结构..."
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$SQL_FILE"

# 5. 验证表数量
TABLE_COUNT=$(mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" \
    -s -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME';")
print_ok "导入完成，$DB_NAME 库当前表数量: $TABLE_COUNT"

echo ""
print_ok "数据库初始化成功！"
