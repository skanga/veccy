@echo off
REM Veccy CLI launcher for Windows

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

REM Run the CLI using the fat JAR
"%JAVA_CMD%" %JAVA_OPTS% -jar "%FAT_JAR%" %*

endlocal
