#!/bin/bash

# Test script to verify Windows batch file syntax
echo "Testing YOTTA-Ledger.bat syntax..."
echo

# Check for common batch file syntax errors
echo "Checking for variable syntax issues:"
grep -n "%%" secure-dist/YOTTA-Ledger.bat && echo "ERROR: Found double %% in batch file!" || echo "✓ Variable syntax looks correct"

echo
echo "Checking for path separator issues:"
grep -n "jrebinjava" secure-dist/YOTTA-Ledger.bat && echo "ERROR: Missing path separators!" || echo "✓ Path separators look correct"

echo
echo "Checking for proper java.exe path:"
grep -n "java.exe" secure-dist/YOTTA-Ledger.bat && echo "✓ Found java.exe references" || echo "ERROR: No java.exe references found!"

echo
echo "Batch file content preview:"
echo "========================="
head -20 secure-dist/YOTTA-Ledger.bat
echo "========================="
echo
echo "JRE structure verification:"
ls -la secure-dist/jre/bin/java.exe 2>/dev/null && echo "✓ java.exe exists" || echo "ERROR: java.exe not found!"

echo
echo "Test completed!"