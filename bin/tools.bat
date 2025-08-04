@echo off
setlocal enabledelayedexpansion

if "%1" == "-h" goto :help
if "%1" == "--help" goto :help

if "%1" == "kill" (
    if "%2" == "" (
        echo Error: PID is required for kill command
        echo Usage: tools.bat kill ^<PID^>
        exit /b 1
    )
    taskkill /F /PID %2
    exit /b 0
)

if "%1" == "ps" (
    if "%2" == "" (
        tasklist
    ) else (
        tasklist | findstr %2
    )
    exit /b 0
)

if "%1" == "ls" (
    if "%2" == "" (
        dir
    ) else (
        dir %2
    )
    exit /b 0
)

if "%1" == "rm" (
    if "%2" == "-rf" (
        if "%3" == "" (
            echo Error: Directory or file path is required
            echo Usage: tools.bat rm -rf ^<path^>
            exit /b 1
        )
        rd /s /q %3
    ) else if "%2" neq "" (
        del %2
    ) else (
        echo Error: File path is required
        echo Usage: tools.bat rm ^<file^>
        exit /b 1
    )
    exit /b 0
)

if "%1" == "cp" (
    if "%2" == "" (
        echo Error: Source file is required
        echo Usage: tools.bat cp ^<source^> ^<destination^>
        exit /b 1
    )
    if "%3" == "" (
        echo Error: Destination is required
        echo Usage: tools.bat cp ^<source^> ^<destination^>
        exit /b 1
    )
    copy %2 %3
    exit /b 0
)

if "%1" == "mv" (
    if "%2" == "" (
        echo Error: Source file is required
        echo Usage: tools.bat mv ^<source^> ^<destination^>
        exit /b 1
    )
    if "%3" == "" (
        echo Error: Destination is required
        echo Usage: tools.bat mv ^<source^> ^<destination^>
        exit /b 1
    )
    move %2 %3
    exit /b 0
)

if "%1" == "mkdir" (
    if "%2" == "" (
        echo Error: Directory name is required
        echo Usage: tools.bat mkdir ^<directory^>
        exit /b 1
    )
    mkdir %2
    exit /b 0
)

if "%1" == "rmdir" (
    if "%2" == "" (
        echo Error: Directory name is required
        echo Usage: tools.bat rmdir ^<directory^>
        exit /b 1
    )
    rmdir %2
    exit /b 0
)

if "%1" == "cat" (
    if "%2" == "" (
        echo Error: File name is required
        echo Usage: tools.bat cat ^<file^>
        exit /b 1
    )
    type %2
    exit /b 0
)

if "%1" == "pwd" (
    cd
    exit /b 0
)

echo Unknown command: %1
echo Use -h or --help for usage information
exit /b 1

:help
echo.
echo Simple Gateway Tools - Windows Command Translator
echo.
echo Usage: tools.bat [command] [arguments]
echo.
echo Commands:
echo   kill PID               Kill a process by PID (taskkill /F /PID PID)
echo   ps [pattern]           List processes (tasklist ^| findstr pattern)
echo   ls [directory]         List directory contents (dir)
echo   rm file                Delete a file (del)
echo   rm -rf directory       Delete directory recursively (rd /s /q)
echo   cp source dest         Copy file (copy)
echo   mv source dest         Move file (move)
echo   mkdir directory        Create directory (mkdir)
echo   rmdir directory        Remove directory (rmdir)
echo   cat file               Display file contents (type)
echo   pwd                    Print working directory (cd)
echo.
echo Options:
echo   -h, --help             Show this help message
echo.
exit /b 0
