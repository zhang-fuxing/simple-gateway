#!/bin/bash

# Linux shutdown script for simple-gateway

# 应用名称
APP_NAME="simple-gateway"

# 设置脚本所在目录
BASE_DIR=$(cd "$(dirname "$0")" && pwd)
# 转到项目根目录
cd "${BASE_DIR}/.." || exit 1

# 检查PID文件是否存在
if [ ! -f "${APP_NAME}.pid" ]; then
    echo "No PID file found. Checking for running processes..."

    # 查找正在运行的进程
    PID=$(ps -ef | grep "${APP_NAME}" | grep -v grep | awk '{print $2}')

    if [ -z "$PID" ]; then
        echo "No running ${APP_NAME} process found"
        exit 1
    fi
else
    # 从PID文件读取PID
    PID=$(cat "${APP_NAME}.pid")
fi

# 检查进程是否存在
if ps -p ${PID} > /dev/null; then
    echo "Stopping ${APP_NAME} (PID: ${PID})..."
    kill ${PID}

    # 等待进程结束
    TIMEOUT=30
    COUNT=0
    while ps -p ${PID} > /dev/null; do
        if [ ${COUNT} -ge ${TIMEOUT}; then
            echo "Timeout reached. Force killing process..."
            kill -9 ${PID}
            break
        fi
        echo "Waiting for process to stop... (${COUNT}s)"
        sleep 1
        COUNT=$((COUNT + 1))
    done

    # 删除PID文件
    rm -f "${APP_NAME}.pid"

    echo "${APP_NAME} stopped successfully"
else
    echo "Process with PID ${PID} is not running"
    rm -f "${APP_NAME}.pid"
fi
