@echo off
setlocal
cd /d "%~dp0"
java Build.java
if errorlevel 1 (
    echo A compilacao requer JDK 17 ou superior, com o compilador incluido.
    pause
    exit /b 1
)
echo.
echo Compilado. Abra INICIAR_WINDOWS.bat para jogar.
pause
endlocal
