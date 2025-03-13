#!/bin/bash

# Exit immediately if any command fails
set -e

# Package metadata configuration
PACKAGE_NAME="audiobook-creator"
PACKAGE_VERSION="1.0.0"
PACKAGE_ARCHITECTURE="all"
PACKAGE_MAINTAINER="Your Name <your.email@example.com>"
PACKAGE_DESCRIPTION="Audiobook Creator Application"
# Define dependencies - requires Java runtime and JavaFX
PACKAGE_DEPENDS="default-jre | openjdk-11-jre | openjdk-17-jre, openjfx"

# Create the standard Debian package directory structure
echo "Creating package directory structure..."
mkdir -p "${PACKAGE_NAME}_${PACKAGE_VERSION}_${PACKAGE_ARCHITECTURE}/DEBIAN"
mkdir -p "${PACKAGE_NAME}_${PACKAGE_VERSION}_${PACKAGE_ARCHITECTURE}/usr/share/applications"
mkdir -p "${PACKAGE_NAME}_${PACKAGE_VERSION}_${PACKAGE_ARCHITECTURE}/usr/share/icons/hicolor/256x256/apps"
mkdir -p "${PACKAGE_NAME}_${PACKAGE_VERSION}_${PACKAGE_ARCHITECTURE}/usr/share/${PACKAGE_NAME}"
mkdir -p "${PACKAGE_NAME}_${PACKAGE_VERSION}_${PACKAGE_ARCHITECTURE}/usr/bin"

# Generate the DEBIAN control file with package metadata
echo "Creating control file..."
cat > "${PACKAGE_NAME}_${PACKAGE_VERSION}_${PACKAGE_ARCHITECTURE}/DEBIAN/control" << EOF
Package: ${PACKAGE_NAME}
Version: ${PACKAGE_VERSION}
Architecture: ${PACKAGE_ARCHITECTURE}
Maintainer: ${PACKAGE_MAINTAINER}
Depends: ${PACKAGE_DEPENDS}
Section: utils
Priority: optional
Description: ${PACKAGE_DESCRIPTION}
 A tool for creating audiobooks from various sources.
 This package provides a GUI application for audiobook creation.
EOF

# Create a .desktop file for desktop integration
echo "Creating desktop file..."
cat > "${PACKAGE_NAME}_${PACKAGE_VERSION}_${PACKAGE_ARCHITECTURE}/usr/share/applications/${PACKAGE_NAME}.desktop" << EOF
[Desktop Entry]
Name=Audiobook Creator
Comment=Create audiobooks from various sources
Exec=/usr/bin/audiobook-creator
Icon=/usr/share/icons/hicolor/256x256/apps/audiobook-creator.png
Terminal=false
Type=Application
Categories=Utility;AudioVideo;
EOF

# Create a launcher script that sets up Java modules and runs the application
echo "Creating launcher script..."
cat > "${PACKAGE_NAME}_${PACKAGE_VERSION}_${PACKAGE_ARCHITECTURE}/usr/bin/audiobook-creator" << EOF
#!/bin/bash
exec java --module-path /usr/share/openjfx/lib --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.web -jar /usr/share/${PACKAGE_NAME}/AudiobookCreator.jar "\$@"
EOF

# Set the launcher script to be executable
chmod +x "${PACKAGE_NAME}_${PACKAGE_VERSION}_${PACKAGE_ARCHITECTURE}/usr/bin/audiobook-creator"

# Copy application files to package directories
echo "Copying application files..."
# Verify that required files exist before continuing
if [ ! -f "AudiobookCreator.jar" ]; then
  echo "Error: AudiobookCreator.jar not found in current directory!"
  exit 1
fi

# Verify icon file exists
if [ ! -f "AudiobookIconLinux.png" ]; then
  echo "Error: AudiobookIconLinux.png not found in current directory!"
  exit 1
fi

# Set JAR file to be executable and copy to package directory
cp "AudiobookCreator.jar" "${PACKAGE_NAME}_${PACKAGE_VERSION}_${PACKAGE_ARCHITECTURE}/usr/share/${PACKAGE_NAME}/"
chmod +x "${PACKAGE_NAME}_${PACKAGE_VERSION}_${PACKAGE_ARCHITECTURE}/usr/share/${PACKAGE_NAME}/AudiobookCreator.jar"
cp "AudiobookIconLinux.png" "${PACKAGE_NAME}_${PACKAGE_VERSION}_${PACKAGE_ARCHITECTURE}/usr/share/icons/hicolor/256x256/apps/audiobook-creator.png"

# Create post-installation script to update desktop database
mkdir -p "${PACKAGE_NAME}_${PACKAGE_VERSION}_${PACKAGE_ARCHITECTURE}/DEBIAN"
cat > "${PACKAGE_NAME}_${PACKAGE_VERSION}_${PACKAGE_ARCHITECTURE}/DEBIAN/postinst" << EOF
#!/bin/bash
update-desktop-database -q || true
EOF
chmod 755 "${PACKAGE_NAME}_${PACKAGE_VERSION}_${PACKAGE_ARCHITECTURE}/DEBIAN/postinst"

# Build the final .deb package
echo "Building .deb package..."
dpkg-deb --build "${PACKAGE_NAME}_${PACKAGE_VERSION}_${PACKAGE_ARCHITECTURE}"