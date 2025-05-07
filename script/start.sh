#!/bin/bash

DEFAULT_PORT=8777
JAR_NAME="hardware-watcher.jar"  # 替换为你的jar包名

# 如果通过systemd启动，则跳过脚本中的管理逻辑
if [ -n "$SYSTEMD_EXEC_PID" ]; then
    echo "[INFO] 检测到已经通过systemd启动..."
    exec /opt/jdk-21.0.7/bin/java -jar $JAR_NAME --server.port=$DEFAULT_PORT
    exit 0
fi

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 默认配置
APP_NAME="${JAR_NAME%.*}"        # 从jar名提取应用名
LOG_DIR="./logs"
CURRENT_LOG="$LOG_DIR/$APP_NAME.log"
PID_FILE="/tmp/$APP_NAME.pid"

# 检查是否是systemd服务
is_systemd_service() {
    if [ -f "/etc/systemd/system/$APP_NAME.service" ] ||
       [ -f "/lib/systemd/system/$APP_NAME.service" ] ||
       systemctl list-units --full -all | grep -Fq "$APP_NAME.service"; then
        return 0
    else
        return 1
    fi
}

# 检查端口是否被占用
is_port_available() {
    local port=$1
    if ! lsof -i :$port > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# 查找随机可用端口
find_random_port() {
    local min_port=49152
    local max_port=65535
    local port

    while true; do
        port=$(shuf -i $min_port-$max_port -n 1)
        if is_port_available $port; then
            echo $port
            return
        fi
    done
}

# 归档旧日志
archive_logs() {
    if [ ! -d "$LOG_DIR" ]; then
        mkdir -p "$LOG_DIR"
    fi

    if [ -f "$CURRENT_LOG" ]; then
        local timestamp=$(date +"%Y%m%d_%H%M%S")
        local archived_log="$LOG_DIR/${APP_NAME}_${timestamp}.log"
        mv "$CURRENT_LOG" "$archived_log"
        echo -e "${CYAN}已归档旧日志: $archived_log${NC}"
        gzip "$archived_log" &  # 后台压缩以加快启动速度
    fi
}

# 检查服务是否正在运行
is_service_running() {
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if ps -p $pid > /dev/null 2>&1; then
            return 0
        else
            # 清除无效的pid文件
            rm -f "$PID_FILE"
        fi
    fi
    return 1
}

# 停止正在运行的服务
stop_running_service() {
    if is_service_running; then
        local pid=$(cat "$PID_FILE")
        echo -e "${YELLOW}发现正在运行的服务(PID: $pid)，正在停止...${NC}"
        kill $pid
        # 等待进程结束
        local count=0
        while ps -p $pid > /dev/null 2>&1 && [ $count -lt 10 ]; do
            sleep 1
            count=$((count+1))
        done
        if ps -p $pid > /dev/null 2>&1; then
            echo -e "${RED}无法正常停止服务，尝试强制终止...${NC}"
            kill -9 $pid
        fi
        rm -f "$PID_FILE"
        echo -e "${GREEN}服务已停止${NC}"
    fi
}

# 启动服务
start_service() {
    local port=$1

    local cmd="/opt/jdk-21.0.7/bin/java -jar $JAR_NAME --server.port=$port"

    echo -e "${BLUE}启动命令: $cmd${NC}"
    echo -e "${CYAN}日志输出: $CURRENT_LOG${NC}"

    # 启动服务并记录PID
    nohup $cmd >> "$CURRENT_LOG" 2>&1 &
    local pid=$!
    echo $pid > "$PID_FILE"

    echo -e "${PURPLE}服务启动中(PID: $pid)...${NC}"

    # 验证启动是否成功
    verify_startup $pid $port
}

# 验证启动是否成功
verify_startup() {
    local pid=$1
    local port=$2
    local max_attempts=30
    local attempt=0
    local success=0

    echo -e "${CYAN}正在验证启动状态...${NC}"

    while [ $attempt -lt $max_attempts ]; do
        # 检查进程是否存活
        if ! ps -p $pid > /dev/null 2>&1; then
            echo -e "${RED}服务进程已退出，启动失败！${NC}"
            echo -e "${YELLOW}最后几行日志输出:${NC}"
            tail -n 20 "$CURRENT_LOG"
            rm -f "$PID_FILE"
            exit 1
        fi

        # 检查端口是否被监听
        if lsof -i :$port -a -p $pid > /dev/null 2>&1; then
            success=1
            break
        fi

        attempt=$((attempt+1))
        sleep 1
    done

    if [ $success -eq 1 ]; then
        echo -e "${GREEN}服务启动成功！${NC}"
        echo -e "${BLUE}PID: $pid${NC}"
        echo -e "${BLUE}端口: $port${NC}"
        echo -e "${CYAN}可以使用以下命令查看日志: tail -f $CURRENT_LOG${NC}"
    else
        echo -e "${RED}服务启动验证超时，可能启动失败${NC}"
        echo -e "${YELLOW}最后几行日志输出:${NC}"
        tail -n 20 "$CURRENT_LOG"
        exit 1
    fi
}

# 主函数
main() {
    # 解析端口参数
    local port=$DEFAULT_PORT
    if [ $# -ge 1 ]; then
        if [[ "$1" =~ ^[0-9]+$ ]]; then
            port=$1
        else
            echo -e "${RED}错误: 无效的端口号 '$1'${NC}"
            exit 1
        fi
    fi

    # 停止正在运行的服务
    stop_running_service

    # 归档日志
    archive_logs

    # 检查端口是否可用
    if ! is_port_available $port; then
        echo -e "${YELLOW}端口 $port 已被占用，尝试查找随机可用端口...${NC}"
        port=$(find_random_port)
        echo -e "${YELLOW}使用随机端口: $port${NC}"
    fi

    # 如果是systemd服务，则使用systemctl管理
    if is_systemd_service; then
        echo -e "${BLUE}检测到systemd服务，使用systemctl管理${NC}"
        sudo systemctl restart $APP_NAME
        sudo systemctl status $APP_NAME
        exit 0
    fi



    # 启动服务
    start_service $port
}

# 执行主函数
main "$@"
