@@ ... @@
#!/bin/bash
# Build script for AudiobookCreator on Linux

echo "Step 1: Building JAR with Maven"
mvn clean package

echo "Step 2: Creating Linux DEB with jpackage"
jpackage --input target/ \
  --name "audiobookcreator" \
  --main-jar audiobookcreator-1.0-SNAPSHOT.jar \
  --main-class com.jk24.audiobookcreator.AudiobookCreatorLauncher \
  --type deb \
  --icon src/main/resources/icons/AudiobookIconLinux.png \
  --app-version 1.0.0 \
  --vendor "YourCompany" \
  --linux-shortcut \
  --linux-menu-group "Audio" \
  --dest target/dist

echo "Linux DEB package created in target/dist directory"