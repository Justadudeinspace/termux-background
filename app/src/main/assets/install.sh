#!/data/data/com.termux/files/usr/bin/bash
# Termux Background Plugin Installer

echo "[*] Checking Termux Environment..."

if [ ! -d "$HOME/.termux" ]; then
    mkdir -p "$HOME/.termux"
fi

echo "[*] Copying background image placeholder..."
cp /data/data/com.termuxbackground/files/background.png "$HOME/.termux/background.png" 2>/dev/null || touch "$HOME/.termux/background.png"

echo "[*] Writing termux.properties..."
echo "background-image=background.png" > "$HOME/.termux/termux.properties"

echo "[*] Reloading Termux Settings..."
termux-reload-settings

echo "[✓] Termux Background installed successfully!"
exit 0
