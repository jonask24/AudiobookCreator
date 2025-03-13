@@ ... @@
@echo off
REM Build script for AudiobookCreator

echo Step 1: Building JAR with Maven
call mvn clean package

echo Step 2: Creating Windows EXE with jpackage
jpackage --input target/ ^
  --name "AudiobookCreator" ^
  --main-jar audiobookcreator-1.0-SNAPSHOT.jar ^
  --main-class com.jk24.audiobookcreator.AudiobookCreatorLauncher ^
  --type exe ^
  --icon src/main/resources/icons/audiobookiconWin.ico ^
  --app-version 1.0.0 ^
  --vendor "YourCompany" ^
  --win-dir-chooser ^
  --win-shortcut ^
  --win-menu ^
  --dest target/dist

echo Windows executable created in target/dist directory