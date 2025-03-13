module com.jk24.audiobookcreator {
   requires javafx.controls;
   requires javafx.fxml;
   requires java.desktop;
   requires java.prefs;
   requires jave.core;
   requires java.logging;
   requires jaudiotagger;
   requires org.slf4j;  // Add SLF4J API
   // The JAVE library doesn't use the module system, so we use classpath instead
   
   opens com.jk24.audiobookcreator to javafx.fxml;
   exports com.jk24.audiobookcreator;
}