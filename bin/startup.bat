@echo off
rem Windows startup script for simple-gateway
chcp 65001 > nul

rem 初始化变量
set JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC
set APP_ARGS=

rem 收集所有参数作为应用程序参数
set APP_ARGS=%*

rem 设置标题
title Simple Gateway

rem 获取脚本所在目录
set BASE_DIR=%~dp0
rem 转到项目根目录(假设startup.bat在bin目录下)
cd /d "%BASE_DIR%\.."

rem 检查Java是否安装
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not in PATH
    pause
    exit /b 1
)

rem 查找jar文件
set JAR_FILE=
for /f "tokens=*" %%i in ('dir /b /s "*.jar" 2^>nul') do (
    set JAR_FILE=%%i
    goto found_jar
)

:found_jar
if "%JAR_FILE%"=="" (
    echo Error: No JAR file found in the current directory or subdirectories
    pause
    exit /b 1
)

echo Starting Simple Gateway...
echo Using JAR file: %JAR_FILE%
echo JAVA_OPTS: %JAVA_OPTS%
echo Application args: %APP_ARGS%

rem 启动应用
java %JAVA_OPTS% -jar "%JAR_FILE%" %APP_ARGS%

rem 如果应用退出，保持窗口打开以便查看错误信息
if %errorlevel% neq 0 (
    echo.
    echo Application exited with error code %errorlevel%
    pause
)

exit /b %errorlevel%

