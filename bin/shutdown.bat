@echo off
rem Windows shutdown script for simple-gateway

echo Stopping Simple Gateway...

rem 查找并终止Java进程
for /f "tokens=1" %%i in ('jps -l ^| findstr "simple-gateway"') do (
    echo Found process %%i, terminating...
    taskkill /PID %%i /F
    goto process_found
)

:process_found
if %errorlevel% equ 0 (
    echo Simple Gateway stopped successfully.
) else (
    echo No running Simple Gateway process found.
)

pause
