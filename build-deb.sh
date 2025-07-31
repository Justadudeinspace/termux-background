#!/data/data/com.termux/files/usr/bin/bash

mkdir -p termux-background-deb/DEBIAN
cat > termux-background-deb/DEBIAN/control << EOM
Package: termux-background
Version: 1.0.2
Architecture: all
Maintainer: Justadudeinspace
Description: Termux plugin to apply background image.
EOM

mkdir -p termux-background-deb/data/data/com.termux/files/usr/bin
cp app/src/main/assets/background.png termux-background-deb/data/data/com.termux/files/usr/bin/

cat > termux-background-deb/data/data/com.termux/files/usr/bin/termux-background << EOL
#!/data/data/com.termux/files/usr/bin/bash
mkdir -p ~/.termux
cp -f ~/termux-background/background.png ~/.termux/background.png
grep -q background= ~/.termux/termux.properties || {
 echo "background=background.png" >> ~/.termux/termux.properties
 echo "background.opacity=0.8" >> ~/.termux/termux.properties
 echo "background.animation=scroll" >> ~/.termux/termux.properties
}
termux-reload-settings
EOL

chmod +x termux-background-deb/data/data/com.termux/files/usr/bin/termux-background
dpkg-deb -b termux-background-deb termux-background_1.0.2_all.deb
