@echo off
REM Veccy REST API Server launcher for Windows

setlocal enabledelayedexpansion

REM Find Java
if defined JAVA_HOME (
    set JAVA_CMD=%JAVA_HOME%\bin\java
) else (
    set JAVA_CMD=java
)

REM Get the directory where this script is located
set VECCY_HOME=%~dp0

REM Find the fat JAR
set FAT_JAR=
for %%f in ("%VECCY_HOME%target\veccy-*-fat.jar") do (
    set FAT_JAR=%%f
    goto :found_jar
)

:not_found
echo Error: Fat JAR not found. Please build the project first:
echo   mvn clean package
exit /b 1

:found_jar
REM Set default JVM options if not set
if not defined JAVA_OPTS (
    set JAVA_OPTS=-Xmx2g -Xms512m
)

REM Default port and host
set PORT=7878
set HOST=localhost

REM Parse command line arguments
:parse_args
if "%~1"=="" goto :done_args
if "%~1"=="-p" (
    set PORT=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--port" (
    set PORT=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="-h" (
    set HOST=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--host" (
    set HOST=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--help" (
    goto :show_help
)
shift
goto :parse_args

:done_args
echo Starting Veccy REST API Server on %HOST%:%PORT%...
echo.

REM Run the REST server
"%JAVA_CMD%" %JAVA_OPTS% -cp "%FAT_JAR%" com.veccy.rest.VeccyRestServer --port %PORT% --host %HOST%
goto :eof

:show_help
echo Veccy REST API Server
echo.
echo Usage: veccy-server.bat [options]
echo.
echo Options:
echo   -p, --port ^<port^>       Server port (default: 7878)
echo   -h, --host ^<host^>       Server host (default: localhost)
echo   --help                  Show this help message
echo.
echo Example:
echo   veccy-server.bat --port 8080 --host 0.0.0.0
goto :eof

endlocal
