@echo off
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0FLOW_UI_ICONOS_PRO.ps1"
if errorlevel 1 (
  echo.
  echo ERROR: NO se ha aplicado ningun cambio.
) else (
  echo.
  echo SUCCESS: iconos PRO aplicados.
)
pause
