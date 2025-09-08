# YOTTA Ledger Windows Installation Troubleshooting Guide

## "Java Runtime Environment not found" Error - Complete Solution Guide

If you're experiencing the "Java Runtime Environment not found" error after installing YOTTA Ledger, follow these comprehensive troubleshooting steps:

### Step 1: Run the Debug Launcher (Recommended First Step)

1. Navigate to your YOTTA Ledger installation directory (usually `C:\Program Files\YOTTA Ledger` or `C:\Program Files (x86)\YOTTA Ledger`)
2. Double-click on `debug-launcher.bat`
3. Review the detailed diagnostic information displayed
4. Take a screenshot of the output for reference

### Step 2: Verify Installation Integrity

**Check Required Files:**
Your installation directory should contain these files:
- `YOTTA-Ledger.bat` (main launcher)
- `debug-launcher.bat` (diagnostic tool)
- `app.jar` (application file, ~87MB)
- `EZLedger.class` (wrapper class)
- `jre\` folder (Java Runtime Environment)
- `jre\bin\java.exe` (Java executable)

**Quick Verification:**
1. Open Command Prompt as Administrator
2. Navigate to installation directory: `cd "C:\Program Files\YOTTA Ledger"`
3. Run: `dir /s` to list all files
4. Verify `jre\bin\java.exe` exists and is not 0 bytes

### Step 3: Test Java Runtime Manually

1. Open Command Prompt as Administrator
2. Navigate to installation directory
3. Run: `"jre\bin\java.exe" -version`
4. You should see Java version information
5. If this fails, the JRE is corrupted - proceed to Step 6 (Reinstall)

### Step 4: Check Windows Path Issues

**Path Length Limitation:**
- Windows has a 260-character path limit
- If your installation path is very long, try installing to `C:\YOTTA\` instead

**Special Characters:**
- Avoid installation paths with spaces, unicode characters, or special symbols
- Recommended path: `C:\YOTTA-Ledger\`

### Step 5: Security and Permissions

**Antivirus Exclusions:**
1. Add the entire YOTTA Ledger installation folder to your antivirus exclusions
2. Common antivirus programs that may block:
   - Windows Defender
   - Norton
   - McAfee
   - Avast
   - Kaspersky

**Run as Administrator:**
1. Right-click on `YOTTA-Ledger.bat`
2. Select "Run as administrator"
3. If this works, create a shortcut with admin privileges

**Windows SmartScreen:**
1. If Windows blocks the installer, click "More info"
2. Click "Run anyway"
3. This is normal for new applications

### Step 6: Complete Reinstallation

**Clean Uninstall:**
1. Go to Settings > Apps > Apps & features
2. Find "YOTTA Ledger" and click Uninstall
3. Delete any remaining files in the installation directory
4. Clear temporary files: `%TEMP%\YOTTA*`

**Fresh Installation:**
1. Download the latest `YOTTA-Ledger-Setup-1.0.0.exe`
2. Right-click and "Run as administrator"
3. Choose a simple installation path like `C:\YOTTA-Ledger\`
4. Disable antivirus temporarily during installation
5. Test immediately after installation

### Step 7: Alternative Launch Methods

**PowerShell Method:**
1. Open PowerShell as Administrator
2. Navigate to installation directory
3. Run: `& ".\jre\bin\java.exe" -cp "app.jar;." YOTTALedger`

**Direct Java Launch:**
1. Open Command Prompt as Administrator
2. Navigate to installation directory
3. Run: `"jre\bin\java.exe" -Xms256m -Xmx1024m -cp "app.jar;." YOTTALedger`

### Step 8: System Requirements Verification

**Minimum Requirements:**
- Windows 10 or later (64-bit)
- 2GB RAM minimum, 4GB recommended
- 500MB free disk space
- Administrator privileges for installation

**Architecture Check:**
1. Open Command Prompt
2. Run: `echo %PROCESSOR_ARCHITECTURE%`
3. Should show `AMD64` for 64-bit systems

### Step 9: Advanced Diagnostics

**Windows Event Viewer:**
1. Press Win+R, type `eventvwr.msc`
2. Navigate to Windows Logs > Application
3. Look for Java or YOTTA-related errors
4. Note error codes and descriptions

**Process Monitor:**
1. Download Process Monitor from Microsoft Sysinternals
2. Run while attempting to start YOTTA Ledger
3. Filter by process name "java" or "YOTTA"
4. Look for file access denied or path not found errors

### Step 10: Silent Installation Troubleshooting

**For IT Administrators:**
```cmd
# Silent install
YOTTA-Ledger-Setup-1.0.0.exe /S /D=C:\YOTTA-Ledger

# Verify installation
if exist "C:\YOTTA-Ledger\jre\bin\java.exe" (
    echo Installation successful
) else (
    echo Installation failed
)
```

### Common Error Codes and Solutions

| Error Code | Description | Solution |
|------------|-------------|----------|
| Exit Code 1 | JRE not found | Follow Steps 1-3 |
| Exit Code 2 | App files missing | Reinstall (Step 6) |
| Exit Code 3 | Permission denied | Run as admin (Step 5) |
| Exit Code 9009 | Command not found | Check PATH issues (Step 4) |

### Getting Additional Help

**Before Contacting Support:**
1. Run `debug-launcher.bat` and save the output
2. Note your Windows version: `winver`
3. Check available disk space
4. List installed antivirus software
5. Try installation on a different user account

**Support Information:**
- Include the debug-launcher.bat output
- Specify exact error messages
- Mention your Windows version and architecture
- List any security software installed

### Quick Fix Checklist

- [ ] Ran debug-launcher.bat
- [ ] Verified all files exist
- [ ] Tested Java runtime manually
- [ ] Added antivirus exclusions
- [ ] Tried running as administrator
- [ ] Checked installation path length
- [ ] Attempted clean reinstallation
- [ ] Verified system requirements

---

**Note:** This installer includes an embedded Java Runtime Environment (JRE) and should not require any external Java installation. If you continue to experience issues after following this guide, the problem may be related to Windows security policies or system configuration that requires IT administrator assistance.