#!/bin/bash
# OpsMonitor 停止脚本
# 用法：bash stop.sh

PROJECT_ROOT=$(cd "$(dirname "$0")/.." && pwd)
LOG_DIR="$PROJECT_ROOT/deploy/logs"

GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

print_ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
print_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

# 停止 Agent
AGENT_PID=$(pgrep -f "opsmonitor-agent.jar" 2>/dev/null || true)
if [ -n "$AGENT_PID" ]; then
    kill "$AGENT_PID" 2>/dev/null || true
    sleep 1
    print_ok "Agent 已停止 (PID: $AGENT_PID)"
else
    print_warn "Agent 未在运行"
fi

# 停止 Server
SERVER_PID=$(pgrep -f "opsmonitor-server.jar" 2>/dev/null || true)
if [ -n "$SERVER_PID" ]; then
    kill "$SERVER_PID" 2>/dev/null || true
    sleep 2
    if pgrep -f "opsmonitor-server.jar" > /dev/null 2>&1; then
        print_warn "Server 未响应，强制终止..."
        kill -9 "$SERVER_PID" 2>/dev/null || true
    fi
    print_ok "Server 已停止 (PID: $SERVER_PID)"
else
    print_warn "Server 未在运行"
fi

# 清理 PID 文件
rm -f "$LOG_DIR/server.pid" "$LOG_DIR/agent.pid" 2>/dev/null

print_ok "所有服务已停止"
