@echo off
setlocal
cd /d "%~dp0"

set "CONFIG_FILE=ModernMusicBot.properties"

if not exist "%CONFIG_FILE%" (
  if exist "application.properties" (
    echo Migrating application.properties to %CONFIG_FILE%
    ren "application.properties" "%CONFIG_FILE%"
  )
)

if not exist "%CONFIG_FILE%" (
  echo Missing %CONFIG_FILE%
  echo Copy ModernMusicBot.properties.example to %CONFIG_FILE% and set your bot token.
  pause
  exit /b 1
)

mvn -q -DskipTests package
if errorlevel 1 (
  echo Build failed.
  pause
  exit /b 1
)

set "JAR_PATH="
for %%F in (target\*-jar-with-dependencies.jar) do (
  set "JAR_PATH=%%F"
  goto :run_bot
)

echo Built jar not found in target\
pause
exit /b 1

:run_bot
echo Using config: %CONFIG_FILE%
java --enable-native-access=ALL-UNNAMED -jar "%JAR_PATH%" "%CONFIG_FILE%"
pause
