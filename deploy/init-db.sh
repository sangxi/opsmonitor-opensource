#!/bin/bash
# OpsMonitor 数据库初始化脚本
# 用法：bash init-db.sh
# 需要目标机器上有 mysql 客户端
# 说明：自动导入核心表(opsmonitor-MySQL.sql)与使用教程数据(opsmonitor-help.sql)

set -e

PROJECT_ROOT=$(cd "$(dirname "$0")/.." && pwd)
SQL_FILE="$PROJECT_ROOT/sql/opsmonitor-MySQL.sql"
HELP_SQL_FILE="$PROJECT_ROOT/sql/opsmonitor-help.sql"
M5_SQL_FILE="$PROJECT_ROOT/sql/opsmonitor-m5-upgrade.sql"
ENHANCE_SQL_FILE="$PROJECT_ROOT/sql/opsmonitor-enhance-upgrade.sql"

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
DB_NAME=${DB_NAME:-opsmonitor}
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

# 2.1 检查教程 SQL 文件（可选）
if [ ! -f "$HELP_SQL_FILE" ]; then
    print_warn "教程 SQL 文件不存在(不影响核心表): $HELP_SQL_FILE"
else
    print_ok "检测到教程 SQL: $HELP_SQL_FILE"
fi

# 3. 创建数据库（用 root）
print_ok "创建数据库 $DB_NAME ..."
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" \
    -e "CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 4. 导入表结构
print_ok "导入表结构..."
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$SQL_FILE"

# 4.1 导入教程数据（help 表 + 种子教程，可选但有则导入）
if [ -f "$HELP_SQL_FILE" ]; then
    print_ok "导入教程数据..."
    mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$HELP_SQL_FILE"
fi

# 4.2 导入 M5 功能增强表（告警中心/操作审计/开放API，幂等可重复）
if [ -f "$M5_SQL_FILE" ]; then
    print_ok "导入 M5 功能增强表..."
    mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$M5_SQL_FILE"
else
    print_warn "M5 升级 SQL 不存在(不影响基础功能): $M5_SQL_FILE"

# 4.3 导入 M6 功能增强（主机分组标签/定时报表，幂等可重复）
if [ -f "$ENHANCE_SQL_FILE" ]; then
    print_ok "导入 M6 功能增强..."
    mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$ENHANCE_SQL_FILE"
else
    print_warn "M6 升级 SQL 不存在(不影响基础功能): $ENHANCE_SQL_FILE"
fi
fi

# 5. 验证表数量
TABLE_COUNT=$(mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" \
    -s -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME';")
print_ok "导入完成，$DB_NAME 库当前表数量: $TABLE_COUNT"

echo ""
print_ok "数据库初始化成功！"
