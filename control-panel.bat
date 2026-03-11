@echo off
setlocal
cd /d "%~dp0"

mvn -q -DskipTests package
if errorlevel 1 (
  echo Build failed.
  pause
  exit /b 1
)

set "JAR_PATH="
for %%F in (target\*-jar-with-dependencies.jar) do (
  set "JAR_PATH=%%F"
  goto :run_panel
)

echo Built jar not found in target\
pause
exit /b 1

:run_panel
java -cp "%JAR_PATH%" com.artem.musicbot.ControlPanelApp
pause
