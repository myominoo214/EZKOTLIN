#!/bin/bash

# Create a proper ICO file for the installer

echo "[INFO] Creating icon.ico file..."

# Try to use ImageMagick if available
if command -v convert >/dev/null 2>&1; then
    echo "[INFO] Using ImageMagick to convert SVG to ICO"
    convert icon.svg -resize 256x256 -background transparent icon.ico
    if [ $? -eq 0 ]; then
        echo "[INFO] Successfully created icon.ico using ImageMagick"
        exit 0
    fi
fi

# Try rsvg-convert + icotool
if command -v rsvg-convert >/dev/null 2>&1 && command -v icotool >/dev/null 2>&1; then
    echo "[INFO] Using rsvg-convert and icotool"
    rsvg-convert -w 256 -h 256 icon.svg > icon.png
    icotool -c -o icon.ico icon.png
    rm -f icon.png
    if [ $? -eq 0 ]; then
        echo "[INFO] Successfully created icon.ico using rsvg-convert and icotool"
        exit 0
    fi
fi

# Create a minimal but valid ICO file using Python
if command -v python3 >/dev/null 2>&1; then
    echo "[INFO] Creating ICO file using Python PIL"
    python3 << 'EOF'
import struct
from PIL import Image, ImageDraw
import io

# Create a simple 32x32 icon
img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Draw a simple "Y" shape in blue
draw.polygon([(8, 8), (16, 20), (24, 8), (20, 8), (16, 16), (12, 8)], fill=(0, 100, 200, 255))

# Save as ICO
img.save('icon.ico', format='ICO', sizes=[(32, 32)])
print("Created icon.ico using Python PIL")
EOF
    if [ $? -eq 0 ]; then
        echo "[INFO] Successfully created icon.ico using Python"
        exit 0
    fi
fi

# Fallback: Create a minimal valid ICO file manually
echo "[INFO] Creating minimal ICO file as fallback"
cat > icon.ico << 'EOF'
\x00\x00\x01\x00\x01\x00\x20\x20\x00\x00\x01\x00\x20\x00\xa8\x10\x00\x00\x16\x00\x00\x00\x28\x00\x00\x00\x20\x00\x00\x00\x40\x00\x00\x00\x01\x00\x20\x00\x00\x00\x00\x00\x00\x10\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00
EOF

# Add some basic icon data (32x32 transparent with a simple pattern)
for i in {1..1024}; do
    printf "\x00\x64\xc8\xff" >> icon.ico
done

# Add AND mask (all transparent)
for i in {1..128}; do
    printf "\x00" >> icon.ico
done

echo "[INFO] Created minimal ICO file"
echo "[INFO] Icon creation completed"