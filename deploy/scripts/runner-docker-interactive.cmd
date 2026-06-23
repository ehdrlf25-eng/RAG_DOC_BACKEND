@echo off
REM Run runner-docker-interactive.ps1 with ExecutionPolicy Bypass and Administrator elevation.
cd /d "%~dp0"
echo Requesting Administrator privileges...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "Start-Process powershell -ArgumentList '-NoProfile','-ExecutionPolicy','Bypass','-File','%~dp0runner-docker-interactive.ps1' -Verb RunAs -Wait"
echo.
pause
