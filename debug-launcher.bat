@echo off
setlocal enabledelayedexpansion

echo ========================================
echo YOTTA Ledger Debug Launcher
echo ========================================
echo.

REM Get current directory
set "APP_DIR=%~dp0"
echo Current directory: %APP_DIR%
echo.

REM Check JRE
set "JRE_DIR=%APP_DIR%jre\"
echo Checking JRE directory: %JRE_DIR%
if exist "%JRE_DIR%" (
    echo [OK] JRE directory exists
) else (
    echo [ERROR] JRE directory not found!
    goto :end
)

REM Check java.exe
set "JAVA_EXE=%JRE_DIR%bin\java.exe"
echo Checking Java executable: %JAVA_EXE%
if exist "%JAVA_EXE%" (
    echo [OK] Java executable found
) else (
    echo [ERROR] Java executable not found!
    goto :end
)

REM Test Java version
echo.
echo Testing Java version:
"%JAVA_EXE%" -version
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java executable failed to run!
    goto :end
)

REM Check application JAR
set "APP_JAR=%APP_DIR%app.jar"
echo.
echo Checking application JAR: %APP_JAR%
if exist "%APP_JAR%" (
    echo [OK] Application JAR found
) else (
    echo [ERROR] Application JAR not found!
    goto :end
)

REM Check wrapper class
set "WRAPPER_CLASS=%APP_DIR%EZLedger.class"
echo Checking wrapper class: %WRAPPER_CLASS%
if exist "%WRAPPER_CLASS%" (
    echo [OK] Wrapper class found
) else (
    echo [ERROR] Wrapper class not found!
    goto :end
)

echo.
echo ========================================
echo All checks passed! Attempting to launch...
echo ========================================
echo.

REM Launch with verbose output
"%JAVA_EXE%" -version
echo.
echo Launching application...
"%JAVA_EXE%" -cp "%APP_JAR%;%APP_DIR%" YOTTALedger %*

echo.
echo Application exit code: %ERRORLEVEL%

:end
echo.
echo Press any key to exit...
pause >nul