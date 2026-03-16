!include "MUI2.nsh"

!ifndef ROOT_DIR
!define ROOT_DIR "${__FILEDIR__}\\.."
!endif

!define APP_NAME "ModernMusicBot"
!define APP_PUBLISHER "DiscordModernMusicBot"
!ifndef APP_VERSION
!define APP_VERSION "0.0.0"
!endif

Name "${APP_NAME}"
OutFile "${ROOT_DIR}\\dist\\releases\\DiscordModernMusicBot-v${APP_VERSION}-windows-installer.exe"
InstallDir "$PROGRAMFILES\\${APP_NAME}"
InstallDirRegKey HKCU "Software\\${APP_NAME}" "InstallDir"
RequestExecutionLevel user
ShowInstDetails nevershow
AutoCloseWindow false
Unicode true

!define MUI_ABORTWARNING

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_LANGUAGE "English"

Section "Install"
  SetOutPath "$INSTDIR"
  WriteRegStr HKCU "Software\\${APP_NAME}" "InstallDir" "$INSTDIR"
  File /r "${ROOT_DIR}\\dist\\jpackage\\ModernMusicBot\\*"
  File "${ROOT_DIR}\\ModernMusicBot.properties.example"
  File "${ROOT_DIR}\\README.md"
  File "${ROOT_DIR}\\README_INSTALLATION.md"
  File "${ROOT_DIR}\\LICENSE"
  CreateShortCut "$DESKTOP\\ModernMusicBot.lnk" "$INSTDIR\\ModernMusicBot.exe"
SectionEnd
