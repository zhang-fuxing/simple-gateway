#!/bin/bash

# Linux startup script for simple-gateway

# 初始化变量
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
APP_ARGS=""

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -h)
            echo "Usage: $0 [OPTIONS] [PROGRAM_ARGS]"
            echo "Options:"
            echo "  --help, -h        Show this help message"
            echo ""
            echo "All arguments after options will be passed to the application."
            echo "Example:"
            echo "  $0 --config=proxy.json --port=8080"
            exit 0
            ;;
        *)
            # 将所有未识别的参数作为应用程序参数
            APP_ARGS="$APP_ARGS $1"
            shift
            ;;
    esac
done

# 设置脚本所在目录
BASE_DIR=$(cd "$(dirname "$0")" && pwd)
# 转到项目根目录(假设startup.sh在bin目录下)
cd "${BASE_DIR}/.." || exit 1

# 应用名称
APP_NAME="simple-gateway"

# 日志目录
LOG_DIR="logs"
if [ ! -d "$LOG_DIR" ]; then
    mkdir -p "$LOG_DIR"
fi

# 日志文件
LOG_FILE="${LOG_DIR}/${APP_NAME}_$(date +%Y%m%d_%H%M%S).log"

# 检查Java是否安装
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    exit 1
fi

# 查找jar文件
JAR_FILE=$(find . -maxdepth 2 -name "*.jar" -type f | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo "Error: No JAR file found in the current directory or subdirectories"
    exit 1
fi

# 显示启动信息
echo "Starting ${APP_NAME}..."
echo "Using JAR file: ${JAR_FILE}"
echo "JAVA_OPTS: ${JAVA_OPTS}"
echo "Application args: ${APP_ARGS}"
echo "Log file: ${LOG_FILE}"

# 启动应用
nohup java ${JAVA_OPTS} -jar "${JAR_FILE}" ${APP_ARGS} > "${LOG_FILE}" 2>&1 &

# 获取进程ID
PID=$!

# 将PID保存到文件
echo ${PID} > "${APP_NAME}.pid"

# 等待一段时间检查进程是否还在运行
sleep 2

if ps -p ${PID} > /dev/null; then
    echo "${APP_NAME} started successfully with PID ${PID}"
    echo "Log file: ${LOG_FILE}"
    exit 0
else
    echo "Failed to start ${APP_NAME}"
    rm -f "${APP_NAME}.pid"
    exit 1
fi
