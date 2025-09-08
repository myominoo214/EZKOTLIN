# YOTTA Ledger Windows Installer Troubleshooting Guide

## "Java Runtime Environment not found" Error

If you're experiencing this error after installing YOTTA Ledger, follow these troubleshooting steps:

### Step 1: Run Debug Launcher

1. Navigate to your YOTTA Ledger installation directory (usually `C:\Program Files\YOTTA Ledger\`)
2. Double-click `debug-launcher.bat`
3. This will show detailed information about what files are found/missing
4. Take a screenshot or note the output

### Step 2: Check Installation Directory

Verify these files exist in your installation directory:
- `app.jar` (should be ~87MB)
- `YOTTA-Ledger.bat`
- `EZLedger.class`
- `jre/` folder with Java runtime
- `jre/bin/java.exe`

### Step 3: Manual Path Check

1. Open Command Prompt as Administrator
2. Navigate to installation directory: `cd "C:\Program Files\YOTTA Ledger"`
3. Check JRE: `dir jre\bin\java.exe`
4. If found, try running: `jre\bin\java.exe -version`

### Step 4: Alternative Launch Methods

#### Method A: PowerShell Script
Try running `YOTTA-Ledger.ps1` instead of the batch file:
1. Right-click `YOTTA-Ledger.ps1`
2. Select "Run with PowerShell"

#### Method B: Direct Java Launch
Open Command Prompt in installation directory and run:
```cmd
jre\bin\java.exe -Xms256m -Xmx1024m -XX:+UseG1GC -XX:+DisableAttachMechanism -Djava.security.manager=default -Djava.security.policy=all.policy -Dfile.encoding=UTF-8 -Dcom.sun.management.jmxremote=false -Djava.awt.headless=false -cp "app.jar;." YOTTALedger
```

### Step 5: Common Issues and Solutions

#### Issue: Antivirus Blocking
- **Solution**: Add installation directory to antivirus exclusions
- **Files to exclude**: `app.jar`, `jre/bin/java.exe`, `YOTTA-Ledger.bat`

#### Issue: Insufficient Permissions
- **Solution**: Run as Administrator
- Right-click `YOTTA-Ledger.bat` → "Run as administrator"

#### Issue: Corrupted Installation
- **Solution**: Reinstall the application
- Uninstall via Control Panel
- Delete remaining files in installation directory
- Run installer again

#### Issue: Windows Path Length Limits
- **Solution**: Install to shorter path
- Try installing to `C:\YOTTA\` instead of `C:\Program Files\YOTTA Ledger\`

### Step 6: System Requirements Check

- **OS**: Windows 10/11 (64-bit)
- **RAM**: Minimum 4GB, Recommended 8GB
- **Disk Space**: 200MB free space
- **Permissions**: Administrator rights for installation

### Step 7: Advanced Debugging

If the issue persists:

1. **Check Windows Event Viewer**:
   - Open Event Viewer
   - Navigate to Windows Logs → Application
   - Look for YOTTA Ledger related errors

2. **Enable Batch File Debugging**:
   - Edit `YOTTA-Ledger.bat`
   - Add `echo on` at the top
   - Add `pause` before `exit` commands
   - Run and observe output

3. **Check File Associations**:
   - Ensure `.jar` files are associated with Java
   - Check if `java.exe` is in system PATH

### Getting Help

If none of these solutions work:

1. Run `debug-launcher.bat` and save the output
2. Check Windows Event Viewer for errors
3. Note your Windows version and installation path
4. Contact support with this information

### Silent Installation Troubleshooting

For silent installations (`YOTTA-Ledger-Setup-1.0.0.exe /S`):

- Check installation log in `%TEMP%`
- Ensure running with Administrator privileges
- Verify target directory is writable
- Check for conflicting software

---

**Note**: The embedded JRE should eliminate most Java-related issues. If you're still experiencing problems, it's likely related to file permissions, antivirus software, or installation corruption.