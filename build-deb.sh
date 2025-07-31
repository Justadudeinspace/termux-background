#!/data/data/com.termux/files/usr/bin/bash

set -e
VERSION=1.0.1

echo "[+] Building Termux Background .deb v$VERSION..."

rm -rf termux-background-deb
mkdir -p termux-background-deb/DEBIAN
mkdir -p termux-background-deb/data/data/com.termux/files/usr/bin
mkdir -p termux-background-deb/data/data/com.termux/files/home/.termux

# Launcher script
cat > termux-background-deb/data/data/com.termux/files/usr/bin/termux-background << 'EOS'
#!/data/data/com.termux/files/usr/bin/bash

echo "[✓] Setting Termux background..."
mkdir -p ~/.termux
cp -f ~/termux-background/background.png ~/.termux/background.png

grep -q "background=" ~/.termux/termux.properties 2>/dev/null || {
  echo "background=background.png" >> ~/.termux/termux.properties
  echo "background.opacity=0.8" >> ~/.termux/termux.properties
  echo "background.animation=scroll" >> ~/.termux/termux.properties
}

termux-reload-settings
echo "[✓] Applied background and reloaded Termux."
EOS

chmod +x termux-background-deb/data/data/com.termux/files/usr/bin/termux-background

# Control file
cat > termux-background-deb/DEBIAN/control << EOM
Package: termux-background
Version: $VERSION
Architecture: all
Maintainer: Justadudeinspace
Description: Termux plugin to set terminal background images with opacity and animation.
EOM

dpkg-deb -b termux-background-deb termux-background_${VERSION}_all.deb
echo "[✓] .deb built: termux-background_${VERSION}_all.deb"
