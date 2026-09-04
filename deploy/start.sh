#!/bin/bash
# OpsMonitor 一键启动脚本（适用于非 Docker 部署）
# 用法：bash start.sh

set -e

PROJECT_ROOT=$(cd "$(dirname "$0")/.." && pwd)
SERVER_JAR="$PROJECT_ROOT/opsmonitor-server/target/opsmonitor-server-release.jar"
AGENT_JAR="$PROJECT_ROOT/opsmonitor-agent/target/opsmonitor-agent-release.jar"
SERVER_CONFIG="$PROJECT_ROOT/deploy/server/config/application.yml"
AGENT_CONFIG="$PROJECT_ROOT/deploy/agent/config/application.yml"
LOG_DIR="$PROJECT_ROOT/deploy/logs"

mkdir -p "$LOG_DIR"

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m'

print_ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
print_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
print_err()  { echo -e "${RED}[ERR]${NC} $1"; }

# 1. 检查 Java
if ! command -v java &> /dev/null; then
    print_err "未找到 java 命令，请先安装 JDK 1.8+"
    exit 1
fi
JAVA_VER=$(java -version 2>&1 | head -n1 | awk -F\" '{print $2}')
print_ok "Java 版本: $JAVA_VER"

# 2. 检查 jar 文件
if [ ! -f "$SERVER_JAR" ]; then
    print_err "Server jar 不存在: $SERVER_JAR"
    print_warn "请先在项目根目录执行: mvn clean package -DskipTests"
    exit 1
fi

# 3. 启动 Server
SERVER_PID=$(pgrep -f "opsmonitor-server.jar" 2>/dev/null || true)
if [ -n "$SERVER_PID" ]; then
    print_warn "Server 已在运行，PID: $SERVER_PID"
else
    print_ok "启动 Server..."
    nohup java -Xms256m -Xmx512m -Dfile.encoding=UTF-8 \
        -jar "$SERVER_JAR" \
        --spring.config.location="$SERVER_CONFIG" \
        > "$LOG_DIR/server.out" 2>&1 &
    echo $! > "$LOG_DIR/server.pid"
    sleep 3
    if pgrep -f "opsmonitor-server.jar" > /dev/null; then
        print_ok "Server 启动成功，PID: $(cat $LOG_DIR/server.pid)"
        print_ok "访问地址: http://localhost:9999/opsmonitor/login/toLogin"
    else
        print_err "Server 启动失败，查看日志: $LOG_DIR/server.out"
        exit 1
    fi
fi

# 4. 启动 Agent（可选）
if [ -f "$AGENT_JAR" ]; then
    AGENT_PID=$(pgrep -f "opsmonitor-agent.jar" 2>/dev/null || true)
    if [ -n "$AGENT_PID" ]; then
        print_warn "Agent 已在运行，PID: $AGENT_PID"
    else
        print_ok "启动 Agent..."
        nohup java -Xms64m -Xmx128m -Dfile.encoding=UTF-8 \
            -jar "$AGENT_JAR" \
            --spring.config.location="$AGENT_CONFIG" \
            > "$LOG_DIR/agent.out" 2>&1 &
        echo $! > "$LOG_DIR/agent.pid"
        sleep 2
        if pgrep -f "opsmonitor-agent.jar" > /dev/null; then
            print_ok "Agent 启动成功，PID: $(cat $LOG_DIR/agent.pid)"
        else
            print_warn "Agent 启动失败，查看日志: $LOG_DIR/agent.out"
        fi
    fi
else
    print_warn "Agent jar 不存在，跳过 Agent 启动"
fi

echo ""
print_ok "部署完成！"
echo "  Server: http://localhost:9999/opsmonitor/login/toLogin"
echo "  账号: admin  密码: 111111"
echo "  日志目录: $LOG_DIR"
echo "  停止服务: bash $(dirname $0)/stop.sh"
