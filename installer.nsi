; YOTTA Ledger NSIS Installer Script
; This creates a Windows installer (.exe) that can be built on any platform

!define APPNAME "EZLedger"
!define COMPANYNAME "EZ Systems"
!define DESCRIPTION "Secure Financial Management Application"
!define VERSIONMAJOR 1
!define VERSIONMINOR 0
!define VERSIONBUILD 0
!define HELPURL "https://ez-systems.com/support"
!define UPDATEURL "https://ez-systems.com/updates"
!define ABOUTURL "https://ez-systems.com"
!define INSTALLSIZE 153600 ; Size in KB (150MB)

; Request application privileges for Windows Vista/7/8/10/11
RequestExecutionLevel admin

; Windows 10+ compatibility manifest
!define MUI_MANIFEST_DPI_AWARE
!define MUI_MANIFEST_SUPPORTEDOS

; Include Modern UI and Logic Library
!include "MUI2.nsh"
!include "LogicLib.nsh"
!include "WinVer.nsh"

; General
Name "${APPNAME}"
OutFile "EZLedger-Setup-${VERSIONMAJOR}.${VERSIONMINOR}.${VERSIONBUILD}.exe"
Unicode True

; Default installation folder (Windows 10+ compatible)
InstallDir "$PROGRAMFILES64\EZLedger"

; Get installation folder from registry if available
InstallDirRegKey HKCU "Software\${COMPANYNAME}\${APPNAME}" ""

; Interface Settings
!define MUI_ABORTWARNING
; Icon removed for compatibility
; !define MUI_ICON "icon.ico"
; !define MUI_UNICON "icon.ico"

; Pages
!insertmacro MUI_PAGE_LICENSE "secure-dist\README.txt"
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

; Languages
!insertmacro MUI_LANGUAGE "English"

; Windows version compatibility check
Function .onInit
    ; Check Windows version (Windows 10 = 10.0)
    ${IfNot} ${AtLeastWin10}
        MessageBox MB_YESNO|MB_ICONQUESTION "This application is designed for Windows 10 or later. Continue installation anyway?" IDYES continue
        Abort
        continue:
    ${EndIf}
FunctionEnd

; Version Information
VIProductVersion "${VERSIONMAJOR}.${VERSIONMINOR}.${VERSIONBUILD}.0"
VIAddVersionKey "ProductName" "${APPNAME}"
VIAddVersionKey "Comments" "${DESCRIPTION}"
VIAddVersionKey "CompanyName" "${COMPANYNAME}"
VIAddVersionKey "FileDescription" "${APPNAME} Installer"
VIAddVersionKey "FileVersion" "${VERSIONMAJOR}.${VERSIONMINOR}.${VERSIONBUILD}.0"
VIAddVersionKey "ProductVersion" "${VERSIONMAJOR}.${VERSIONMINOR}.${VERSIONBUILD}.0"
VIAddVersionKey "InternalName" "${APPNAME}"
VIAddVersionKey "LegalCopyright" "© ${COMPANYNAME}"
VIAddVersionKey "OriginalFilename" "YOTTA-Ledger-Setup.exe"

; Installer sections
Section "Core Application" SecCore
    SectionIn RO ; Read-only section
    
    ; Set output path to the installation directory
    SetOutPath $INSTDIR
    
    ; Copy main application files
    File "secure-dist\app.jar"
    File "secure-dist\EZLedger.bat"
    File "secure-dist\EZLedger.ps1"
    File "secure-dist\EZLedger.class"
    File "secure-dist\README.txt"
    File "secure-dist\install.bat"
    File "debug-launcher.bat"
    
    ; Copy JRE directory
    SetOutPath $INSTDIR\jre
    File /r "secure-dist\jre\*"
    
    ; Store installation folder
    WriteRegStr HKCU "Software\${COMPANYNAME}\${APPNAME}" "" $INSTDIR
    
    ; Create uninstaller
    WriteUninstaller "$INSTDIR\Uninstall.exe"
    
    ; Add to Add/Remove Programs
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayName" "${APPNAME}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "UninstallString" "$INSTDIR\Uninstall.exe"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "QuietUninstallString" "$INSTDIR\Uninstall.exe /S"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "InstallLocation" "$INSTDIR"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayIcon" "$INSTDIR\YOTTA-Ledger.bat"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "Publisher" "${COMPANYNAME}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "HelpLink" "${HELPURL}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "URLUpdateInfo" "${UPDATEURL}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "URLInfoAbout" "${ABOUTURL}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayVersion" "${VERSIONMAJOR}.${VERSIONMINOR}.${VERSIONBUILD}"
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "VersionMajor" ${VERSIONMAJOR}
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "VersionMinor" ${VERSIONMINOR}
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "NoModify" 1
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "NoRepair" 1
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "EstimatedSize" ${INSTALLSIZE}
    
SectionEnd

Section "Desktop Shortcut" SecDesktop
    CreateShortcut "$DESKTOP\${APPNAME}.lnk" "$INSTDIR\EZLedger.bat" "" "$INSTDIR\EZLedger.bat" 0
SectionEnd

Section "Start Menu Shortcuts" SecStartMenu
    CreateDirectory "$SMPROGRAMS\${APPNAME}"
    CreateShortcut "$SMPROGRAMS\${APPNAME}\${APPNAME}.lnk" "$INSTDIR\EZLedger.bat" "" "$INSTDIR\EZLedger.bat" 0
    CreateShortcut "$SMPROGRAMS\${APPNAME}\Uninstall.lnk" "$INSTDIR\Uninstall.exe" "" "$INSTDIR\Uninstall.exe" 0
SectionEnd

; Component descriptions
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
    !insertmacro MUI_DESCRIPTION_TEXT ${SecCore} "Core application files and embedded Java runtime"
    !insertmacro MUI_DESCRIPTION_TEXT ${SecDesktop} "Create a desktop shortcut for easy access"
    !insertmacro MUI_DESCRIPTION_TEXT ${SecStartMenu} "Create Start Menu shortcuts"
!insertmacro MUI_FUNCTION_DESCRIPTION_END

; Uninstaller section
Section "Uninstall"
    ; Remove registry keys
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"
    DeleteRegKey HKCU "Software\${COMPANYNAME}\${APPNAME}"
    
    ; Remove files and directories
    Delete "$INSTDIR\app.jar"
    Delete "$INSTDIR\EZLedger.bat"
    Delete "$INSTDIR\EZLedger.ps1"
    Delete "$INSTDIR\YOTTALedger.java"
    Delete "$INSTDIR\EZLedger.class"
    Delete "$INSTDIR\README.txt"
    Delete "$INSTDIR\install.bat"
    Delete "$INSTDIR\Uninstall.exe"
    
    ; Remove JRE directory
    RMDir /r "$INSTDIR\jre"
    
    ; Remove shortcuts
    Delete "$DESKTOP\${APPNAME}.lnk"
    Delete "$SMPROGRAMS\${APPNAME}\${APPNAME}.lnk"
    Delete "$SMPROGRAMS\${APPNAME}\Uninstall.lnk"
    RMDir "$SMPROGRAMS\${APPNAME}"
    
    ; Remove installation directory if empty
    RMDir "$INSTDIR"
    
SectionEnd