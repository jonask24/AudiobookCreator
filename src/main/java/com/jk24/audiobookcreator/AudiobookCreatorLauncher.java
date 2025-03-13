package com.jk24.audiobookcreator;

/**
 * Launcher class for the Audiobook Creator application.
 * This class serves as the main entry point that will be used
 * by the packaging tools to create executables.
 */
public class AudiobookCreatorLauncher {

   public static void main(String[] args) {
        // Launch the JavaFX application
        javafx.application.Application.launch(AudioMergerApp.class, args);
   }
}