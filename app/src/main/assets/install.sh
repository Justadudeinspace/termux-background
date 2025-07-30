#!/data/data/com.termux/files/usr/bin/bash
echo "[*] Installing Termux Background..."
mkdir -p ~/.termux
if cp -f background.png ~/.termux/ && cp -f termux.properties ~/.termux/; then
  echo "[✓] Background image and properties applied."
else
  echo "[!] Failed to copy files. Check permissions."
fi
termux-reload-settings
