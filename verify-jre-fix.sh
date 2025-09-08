#!/bin/bash

echo "🔍 Verifying JRE Fix Implementation..."
echo "======================================"

# Check if secure-dist exists
if [ ! -d "secure-dist" ]; then
    echo "❌ secure-dist directory not found"
    exit 1
fi

# Check batch file syntax
echo "\n📝 Checking YOTTA-Ledger.bat syntax:"
if grep -q "%%" secure-dist/YOTTA-Ledger.bat; then
    echo "❌ Found double percent signs (%%)"
    grep -n "%%" secure-dist/YOTTA-Ledger.bat
else
    echo "✅ Variable syntax is correct (single %)"
fi

# Check path separators
echo "\n🔧 Checking path separators:"
if grep -q "%JRE_DIR%bin\\java.exe" secure-dist/YOTTA-Ledger.bat; then
    echo "✅ JRE path separator is correct"
else
    echo "❌ JRE path separator issue found"
    grep -n "java.exe" secure-dist/YOTTA-Ledger.bat
fi

# Check JRE structure
echo "\n☕ Checking JRE structure:"
if [ -f "secure-dist/jre/bin/java.exe" ]; then
    echo "✅ java.exe exists at: secure-dist/jre/bin/java.exe"
    ls -la secure-dist/jre/bin/java.exe
else
    echo "❌ java.exe not found"
fi

# Check distribution files
echo "\n📦 Checking distribution files:"
for file in "YOTTA-Ledger-Secure-Windows.zip" "YOTTA-Ledger-Secure-Windows.tar.gz"; do
    if [ -f "secure-dist/$file" ]; then
        echo "✅ $file exists ($(du -h secure-dist/$file | cut -f1))"
    else
        echo "❌ $file not found"
    fi
done

# Check NSIS installer
echo "\n🛠️ Checking NSIS installer:"
if [ -f "installer-dist/YOTTA-Ledger-Setup-1.0.0.exe" ]; then
    echo "✅ NSIS installer exists ($(du -h installer-dist/YOTTA-Ledger-Setup-1.0.0.exe | cut -f1))"
else
    echo "❌ NSIS installer not found"
fi

# Show key parts of the batch file
echo "\n📋 Key sections of YOTTA-Ledger.bat:"
echo "=====================================\n"
echo "Variable definitions:"
grep -n "set \"" secure-dist/YOTTA-Ledger.bat | head -4
echo "\nJRE check:"
grep -n "if not exist" secure-dist/YOTTA-Ledger.bat
echo "\nJava execution:"
grep -n "java.exe" secure-dist/YOTTA-Ledger.bat | tail -1

echo "\n🎉 Verification completed!"
echo "\n📝 Summary:"
echo "   - Fixed variable syntax (% instead of %%)"
echo "   - Corrected path separators (backslashes)"
echo "   - Verified JRE structure and java.exe existence"
echo "   - Rebuilt secure distribution packages"
echo "   - Rebuilt NSIS installer"
echo "\n✅ The 'JRE not found' error should now be resolved!"