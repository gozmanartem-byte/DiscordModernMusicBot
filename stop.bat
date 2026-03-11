@echo off
setlocal

for /f "usebackq delims=" %%i in (`powershell -NoProfile -Command "Get-CimInstance Win32_Process ^| Where-Object { $_.CommandLine -match 'jar-with-dependencies.jar|DiscordModernMusicBot.jar' } ^| Select-Object -ExpandProperty ProcessId"`) do (
  taskkill /PID %%i /F >nul 2>&1
)

echo Stop command finished.
pause
