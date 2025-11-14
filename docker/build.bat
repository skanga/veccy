@echo off
REM Docker build script for Veccy (Windows)

setlocal enabledelayedexpansion

REM Configuration
if "%IMAGE_NAME%"=="" set IMAGE_NAME=veccy
if "%IMAGE_TAG%"=="" set IMAGE_TAG=latest
if "%REGISTRY%"=="" set REGISTRY=
set PUSH=false

REM Parse arguments
:parse_args
if "%~1"=="" goto :done_parsing
if /i "%~1"=="-t" (
    set IMAGE_TAG=%~2
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="--tag" (
    set IMAGE_TAG=%~2
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="-r" (
    set REGISTRY=%~2
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="--registry" (
    set REGISTRY=%~2
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="-p" (
    set PUSH=true
    shift
    goto :parse_args
)
if /i "%~1"=="--push" (
    set PUSH=true
    shift
    goto :parse_args
)
if /i "%~1"=="-h" goto :help
if /i "%~1"=="--help" goto :help

echo Unknown option: %~1
exit /b 1

:help
echo Usage: %~nx0 [OPTIONS]
echo.
echo Options:
echo   -t, --tag TAG        Image tag (default: latest^)
echo   -r, --registry REG   Registry to push to
echo   -p, --push           Push image to registry after build
echo   -h, --help           Show this help message
echo.
echo Examples:
echo   %~nx0 -t v1.0.0
echo   %~nx0 -t v1.0.0 -r myregistry.com/veccy -p
exit /b 0

:done_parsing

REM Build full image name
if not "%REGISTRY%"=="" (
    set FULL_IMAGE_NAME=%REGISTRY%/%IMAGE_NAME%:%IMAGE_TAG%
) else (
    set FULL_IMAGE_NAME=%IMAGE_NAME%:%IMAGE_TAG%
)

echo [INFO] Building Docker image...
echo [INFO] Image: %FULL_IMAGE_NAME%

REM Build image
docker build -t %FULL_IMAGE_NAME% -t %IMAGE_NAME%:latest .

if errorlevel 1 (
    echo [ERROR] Build failed
    exit /b 1
)

REM Push if requested
if "%PUSH%"=="true" (
    echo [INFO] Pushing image to registry...
    docker push %FULL_IMAGE_NAME%

    if errorlevel 1 (
        echo [ERROR] Push failed
        exit /b 1
    )

    if not "%IMAGE_TAG%"=="latest" (
        echo [INFO] Also pushing as latest...
        if not "%REGISTRY%"=="" (
            docker tag %FULL_IMAGE_NAME% %REGISTRY%/%IMAGE_NAME%:latest
            docker push %REGISTRY%/%IMAGE_NAME%:latest
        ) else (
            docker push %IMAGE_NAME%:latest
        )
    )
)

echo [INFO] Build complete!
echo.
echo To run the image:
echo   docker run -d -p 8080:8080 -p 8081:8081 %FULL_IMAGE_NAME%
echo.
echo To test:
echo   curl http://localhost:8080/health

endlocal
