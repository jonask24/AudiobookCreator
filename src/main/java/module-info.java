module com.jk24.audiobookcreator {
   requires javafx.controls;
   requires javafx.fxml;
   requires java.desktop;
   requires java.prefs;
   requires jave.core;
   requires java.logging;
   requires jaudiotagger;
   requires org.slf4j;  
   opens com.jk24.audiobookcreator to javafx.fxml;
   opens com.jk24.audiobookcreator.ui to javafx.fxml;
   exports com.jk24.audiobookcreator;
   exports com.jk24.audiobookcreator.ui;
   exports com.jk24.audiobookcreator.model;
   exports com.jk24.audiobookcreator.service;
   exports com.jk24.audiobookcreator.processor;
}