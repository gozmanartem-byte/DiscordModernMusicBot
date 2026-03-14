!include "nsDialogs.nsh"
!include "LogicLib.nsh"
!include "WinMessages.nsh"

!ifndef ROOT_DIR
!define ROOT_DIR "${__FILEDIR__}\\.."
!endif

!define APP_NAME "ModernMusicBot"
!define APP_PUBLISHER "DiscordModernMusicBot"
!define APP_ID "{4f8d1f3b-9f6b-4d28-9c58-5e55b7c4d7a1}"
!ifndef APP_VERSION
!define APP_VERSION "0.0.0"
!endif

Name "${APP_NAME}"
OutFile "dist\\releases\\DiscordModernMusicBot-v${APP_VERSION}-windows-installer.exe"
InstallDir "$PROGRAMFILES\\${APP_NAME}"
InstallDirRegKey HKCU "Software\\${APP_NAME}" "InstallDir"
RequestExecutionLevel user
ShowInstDetails nevershow
AutoCloseWindow false
Unicode true

Var Dialog
Var BgImage
Var NextBtn
Var BackBtn
Var CancelBtn
Var InstallBtn
Var FinishBtn
Var DirInput
Var BrowseBtn
Var ProgressNative
Var ProgressCustom
Var ProgressFrame

!ifndef IMAGE_BITMAP
!define IMAGE_BITMAP 0
!endif
!ifndef LR_LOADFROMFILE
!define LR_LOADFROMFILE 0x0010
!endif
!ifndef BS_BITMAP
!define BS_BITMAP 0x00000080
!endif
!ifndef BS_FLAT
!define BS_FLAT 0x00008000
!endif
!ifndef WS_TABSTOP
!define WS_TABSTOP 0x00010000
!endif
!ifndef BM_SETIMAGE
!define BM_SETIMAGE 0x00F7
!endif
!ifndef SWP_NOSIZE
!define SWP_NOSIZE 0x0001
!endif
!ifndef SWP_NOMOVE
!define SWP_NOMOVE 0x0002
!endif
!ifndef HWND_BOTTOM
!define HWND_BOTTOM 1
!endif
!ifndef PBM_SETRANGE
!define PBM_SETRANGE 0x0401
!endif
!ifndef PBM_SETPOS
!define PBM_SETPOS 0x0402
!endif
!ifndef PBM_GETPOS
!define PBM_GETPOS 0x0408
!endif
!ifndef PBM_SETBARCOLOR
!define PBM_SETBARCOLOR 0x0409
!endif
!ifndef PBM_SETBKCOLOR
!define PBM_SETBKCOLOR 0x2001
!endif
!ifndef PBS_SMOOTH
!define PBS_SMOOTH 0x00000001
!endif

Function .onInit
  InitPluginsDir
  File /oname=$PLUGINSDIR\\bg.bmp "${ROOT_DIR}\\release-assets\\windows\\installer\\nsis\\bg.bmp"
  File /oname=$PLUGINSDIR\\btn_next.bmp "${ROOT_DIR}\\release-assets\\windows\\installer\\nsis\\btn_next.bmp"
  File /oname=$PLUGINSDIR\\btn_back.bmp "${ROOT_DIR}\\release-assets\\windows\\installer\\nsis\\btn_back.bmp"
  File /oname=$PLUGINSDIR\\btn_cancel.bmp "${ROOT_DIR}\\release-assets\\windows\\installer\\nsis\\btn_cancel.bmp"
  File /oname=$PLUGINSDIR\\btn_install.bmp "${ROOT_DIR}\\release-assets\\windows\\installer\\nsis\\btn_install.bmp"
  File /oname=$PLUGINSDIR\\btn_finish.bmp "${ROOT_DIR}\\release-assets\\windows\\installer\\nsis\\btn_finish.bmp"
  File /oname=$PLUGINSDIR\\bar_frame.bmp "${ROOT_DIR}\\release-assets\\windows\\installer\\nsis\\bar_frame.bmp"
FunctionEnd

Function HideDefaultButtons
  GetDlgItem $0 $HWNDPARENT 1
  ShowWindow $0 0
  GetDlgItem $0 $HWNDPARENT 2
  ShowWindow $0 0
  GetDlgItem $0 $HWNDPARENT 3
  ShowWindow $0 0
FunctionEnd

Function CreateBg
  ${NSD_CreateBitmap} 0 0 100% 100% ""
  Pop $BgImage
  ${NSD_SetImage} $BgImage $PLUGINSDIR\\bg.bmp $PLUGINSDIR\\bg.bmp
FunctionEnd

Function ApplyBackgroundToParent
  Exch $0
  System::Call 'user32::CreateWindowExW(i 0, w "STATIC", w "", i 0x5000000E, i 0, i 0, i 640, i 480, i $0, i 0, i 0, i 0) i.r1'
  System::Call 'user32::LoadImageW(i 0, w "$PLUGINSDIR\\bg.bmp", i ${IMAGE_BITMAP}, i 0, i 0, i ${LR_LOADFROMFILE}) i.r2'
  SendMessage $1 ${BM_SETIMAGE} ${IMAGE_BITMAP} $2
  System::Call 'user32::SetWindowPos(i $1, i ${HWND_BOTTOM}, i 0, i 0, i 0, i 0, i ${SWP_NOMOVE}|${SWP_NOSIZE})'
FunctionEnd

Function ApplyButtonBitmap
  Exch $1 ; bmp path
  Exch 1
  Exch $0 ; hwnd
  System::Call 'user32::SetWindowLongW(i $0, i -16, i ${BS_BITMAP}|${BS_FLAT}|${WS_TABSTOP})'
  System::Call 'user32::LoadImageW(i 0, w "$1", i ${IMAGE_BITMAP}, i 0, i 0, i ${LR_LOADFROMFILE}) i.r2'
  SendMessage $0 ${BM_SETIMAGE} ${IMAGE_BITMAP} $2
FunctionEnd

Function CreateImageButton
  Exch $0 ; y
  Exch 1
  Exch $1 ; x
  Exch 2
  Exch $2 ; bmp
  ${NSD_CreateButton} $1 $0 20% 7% ""
  Pop $3
  System::Call 'user32::SetWindowLongW(i $3, i -16, i ${BS_BITMAP}|${BS_FLAT}|${WS_TABSTOP})'
  System::Call 'user32::LoadImageW(i 0, w "$2", i ${IMAGE_BITMAP}, i 0, i 0, i ${LR_LOADFROMFILE}) i.r0'
  SendMessage $3 ${BM_SETIMAGE} ${IMAGE_BITMAP} $0
  Push $3
FunctionEnd

Function WelcomeCreate
  nsDialogs::Create 1018
  Pop $Dialog
  Call CreateBg
  Call HideDefaultButtons

  Push "78%"
  Push "70%"
  Push "$PLUGINSDIR\\btn_next.bmp"
  Call CreateImageButton
  Pop $NextBtn
  ${NSD_OnClick} $NextBtn OnNext

  Push "78%"
  Push "79%"
  Push "$PLUGINSDIR\\btn_cancel.bmp"
  Call CreateImageButton
  Pop $CancelBtn
  ${NSD_OnClick} $CancelBtn OnCancel

  nsDialogs::Show
FunctionEnd

Function DirectoryCreate
  nsDialogs::Create 1018
  Pop $Dialog
  Call CreateBg
  Call HideDefaultButtons

  ${NSD_CreateText} 8% 60% 58% 6% "$INSTDIR"
  Pop $DirInput
  ${NSD_CreateButton} 68% 60% 16% 6% "Browse"
  Pop $BrowseBtn
  ${NSD_OnClick} $BrowseBtn OnBrowse

  Push "8%"
  Push "80%"
  Push "$PLUGINSDIR\\btn_back.bmp"
  Call CreateImageButton
  Pop $BackBtn
  ${NSD_OnClick} $BackBtn OnBack

  Push "78%"
  Push "80%"
  Push "$PLUGINSDIR\\btn_next.bmp"
  Call CreateImageButton
  Pop $NextBtn
  ${NSD_OnClick} $NextBtn OnNext

  Push "78%"
  Push "89%"
  Push "$PLUGINSDIR\\btn_cancel.bmp"
  Call CreateImageButton
  Pop $CancelBtn
  ${NSD_OnClick} $CancelBtn OnCancel

  nsDialogs::Show
FunctionEnd

Function FinishCreate
  nsDialogs::Create 1018
  Pop $Dialog
  Call CreateBg
  Call HideDefaultButtons

  Push "78%"
  Push "80%"
  Push "$PLUGINSDIR\\btn_finish.bmp"
  Call CreateImageButton
  Pop $FinishBtn
  ${NSD_OnClick} $FinishBtn OnNext

  nsDialogs::Show
FunctionEnd

Function OnBrowse
  nsDialogs::SelectFolderDialog "Select install folder" "$INSTDIR"
  Pop $0
  ${If} $0 != ""
    StrCpy $INSTDIR $0
    ${NSD_SetText} $DirInput $INSTDIR
  ${EndIf}
FunctionEnd

Function OnNext
  SendMessage $HWNDPARENT ${WM_COMMAND} 1 0
FunctionEnd

Function OnBack
  SendMessage $HWNDPARENT ${WM_COMMAND} 3 0
FunctionEnd

Function OnCancel
  SendMessage $HWNDPARENT ${WM_COMMAND} 2 0
FunctionEnd

Function InstFilesShow
  Push $HWNDPARENT
  Call ApplyBackgroundToParent

  System::Call 'user32::FindWindowExW(i $HWNDPARENT, i 0, w "msctls_progress32", w "") i.r0'
  StrCpy $ProgressNative $0
  ${If} $ProgressNative != 0
    ShowWindow $ProgressNative 1
  ${EndIf}

  System::Call 'user32::CreateWindowExW(i 0, w "STATIC", w "", i 0x5000000E, i 220, i 350, i 380, i 26, i $HWNDPARENT, i 0, i 0, i 0) i.r1'
  System::Call 'user32::LoadImageW(i 0, w "$PLUGINSDIR\\bar_frame.bmp", i ${IMAGE_BITMAP}, i 0, i 0, i ${LR_LOADFROMFILE}) i.r2'
  SendMessage $1 ${BM_SETIMAGE} ${IMAGE_BITMAP} $2
  StrCpy $ProgressFrame $1

  ${If} $ProgressNative != 0
    System::Call 'user32::SetWindowPos(i $ProgressNative, i 0, i 230, i 356, i 360, i 14, i 0)'
    System::Call 'user32::GetWindowLongW(i $ProgressNative, i -16) i.r2'
    System::Call 'user32::SetWindowLongW(i $ProgressNative, i -16, i r2|${PBS_SMOOTH})'
    System::Call 'user32::SendMessageW(i $ProgressNative, i ${PBM_SETBKCOLOR}, i 0, i 0x00301E12)'
    System::Call 'user32::SendMessageW(i $ProgressNative, i ${PBM_SETBARCOLOR}, i 0, i 0x00F0B060)'
  ${EndIf}

  GetDlgItem $0 $HWNDPARENT 1
  Push $0
  Push "$PLUGINSDIR\\btn_finish.bmp"
  Call ApplyButtonBitmap

  GetDlgItem $0 $HWNDPARENT 2
  Push $0
  Push "$PLUGINSDIR\\btn_cancel.bmp"
  Call ApplyButtonBitmap

  GetDlgItem $0 $HWNDPARENT 3
  Push $0
  Push "$PLUGINSDIR\\btn_back.bmp"
  Call ApplyButtonBitmap

FunctionEnd

Page custom WelcomeCreate
Page custom DirectoryCreate
PageEx instfiles
  PageCallbacks "" InstFilesShow ""
PageExEnd
Page custom FinishCreate

Section "Install"
  SetOutPath "$INSTDIR"
  WriteRegStr HKCU "Software\\${APP_NAME}" "InstallDir" "$INSTDIR"
  File /r "${ROOT_DIR}\\dist\\jpackage\\ModernMusicBot\\*"
  File "${ROOT_DIR}\\ModernMusicBot.properties.example"
  File "${ROOT_DIR}\\README.md"
  File "${ROOT_DIR}\\README_INSTALLATION.md"
  File "${ROOT_DIR}\\LICENSE"
SectionEnd
