package com.jk24.audiobookcreator.ui;

import com.jk24.audiobookcreator.model.Audiobook;
import com.jk24.audiobookcreator.processor.AudiobookProcessor;
import com.jk24.audiobookcreator.service.MetadataService;
import com.jk24.audiobookcreator.service.PreferencesService;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application class for the Audiobook Creator.
 * Provides the user interface and coordinates the application components.
 */
public class AudioMergerApp extends Application {

    // Services for application functionality
    private PreferencesService prefsService;      // Manages user preferences
    private MetadataService metadataService;      // Handles audiobook metadata
    private AudiobookProcessor audiobookProcessor; // Processes audio files
    
    // UI Components
    private Button bookFolderButton;              // Button to select book folder
    private AudiobookView currentAudiobookView;   // View for the current audiobook
    private BorderPane centerPanel;               // Container for audiobook view
    private CheckBox darkModeCheckbox;            // Theme selection checkbox
    private Spinner<Integer> threadCountSpinner;  // Thread count control
    private ProcessingPanel processingPanel;      // Panel showing processing status
    
    // Data
    private List<Audiobook> audiobooks = new ArrayList<>();
    
    // Theme resources
    private Scene scene;
    private static final String LIGHT_THEME_CSS = "/styles/light-theme.css";
    private static final String DARK_THEME_CSS = "/styles/dark-theme.css";

    // Logger for application events
    private static final Logger logger = LoggerFactory.getLogger(AudioMergerApp.class);

    /**
     * JavaFX entry point - initializes the application UI
     */
    @Override
    public void start(Stage primaryStage) {
        // Log application start
        logger.info("Starting AudiobookCreator application");
        
        primaryStage.setTitle("Audiobook Creator");

        // Initialize application services
        prefsService = new PreferencesService();
        metadataService = new MetadataService(prefsService);
        audiobookProcessor = new AudiobookProcessor();
        
        // Configure processor with saved preferences
        int threadCount = prefsService.getThreadCount();
        boolean useMultithreading = threadCount > 1;
        audiobookProcessor.setMultithreadingEnabled(useMultithreading);
        prefsService.setMultithreadingEnabled(useMultithreading);
        audiobookProcessor.setNumThreads(threadCount);

        // Create main layout container
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));

        // Create top toolbar with controls
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        bookFolderButton = new Button("Select Book Folder");
        darkModeCheckbox = new CheckBox("Dark Mode");
        darkModeCheckbox.setStyle("-fx-font-size: 14px;");
        darkModeCheckbox.setTranslateY(3); // Align vertically

        // Create thread count control with label
        Label threadCountLabel = new Label("Threads");
        threadCountLabel.setStyle("-fx-font-size: 14px;");
        threadCountLabel.setTranslateY(5); // Align vertically
        threadCountSpinner = new Spinner<>(1, 10, prefsService.getThreadCount());
        threadCountSpinner.setPrefWidth(70);

        // Add components to toolbar with separators for visual grouping
        toolbar.getChildren().addAll(
            bookFolderButton, 
            new Separator(javafx.geometry.Orientation.VERTICAL),
            darkModeCheckbox,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            threadCountLabel, 
            threadCountSpinner
        );
        
        HBox.setHgrow(toolbar, Priority.ALWAYS);
        mainLayout.setTop(toolbar);

        // Center content holds the audiobook editor
        centerPanel = new BorderPane();
        
        // Right side panel shows processing status
        processingPanel = new ProcessingPanel();
        processingPanel.setMinHeight(408);
        
        // Create layout to hold center content and processing panel
        HBox contentLayout = new HBox(10);
        contentLayout.getChildren().addAll(centerPanel, processingPanel);
        
        // Configure sizing constraints
        centerPanel.setPrefWidth(500);
        centerPanel.setMaxWidth(500);
        centerPanel.setMinWidth(500);
        HBox.setHgrow(centerPanel, Priority.NEVER); // Prevent center panel from growing
        
        // Make processing panel expand when window resizes
        processingPanel.setPrefWidth(300);  // Starting width
        HBox.setHgrow(processingPanel, Priority.ALWAYS);
        
        mainLayout.setCenter(contentLayout);

        // Set up event handlers for UI components
        bookFolderButton.setOnAction(event -> selectBookFolder());
        
        // Initialize button text if there's a saved folder
        File savedFolder = prefsService.getBookFolder();
        if (savedFolder != null && savedFolder.exists()) {
            bookFolderButton.setText(savedFolder.getName());
        }
        
        // Set up theme toggle with saved preference
        darkModeCheckbox.setSelected(prefsService.isDarkTheme());
        darkModeCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            String newTheme = newValue ? "dark" : "light";
            prefsService.setTheme(newTheme);
            applyTheme(newTheme);
        });
        darkModeCheckbox.setStyle("-fx-font-size: 14px;");
        
        // Set up thread count control with saved preference
        threadCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, prefsService.getThreadCount()));
        threadCountSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            prefsService.setThreadCount(newValue);
            audiobookProcessor.setNumThreads(newValue);
            // Enable/disable multithreading based on thread count
            boolean isMultithreading = newValue > 1;
            prefsService.setMultithreadingEnabled(isMultithreading);
            audiobookProcessor.setMultithreadingEnabled(isMultithreading);
        });

        // Initialize with an empty audiobook
        createNewAudiobook();

        // Create and show the application window
        scene = new Scene(mainLayout, 800, 600);
        applyTheme(prefsService.getTheme());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Handles the selection of the book folder.
     * Updates UI and preferences when a folder is selected.
     */
    private void selectBookFolder() {
       DirectoryChooser directoryChooser = new DirectoryChooser();
       directoryChooser.setTitle("Select Book Folder");
        
        // Start in the previously selected folder if available
        File currentFolder = prefsService.getBookFolder();
        if (currentFolder != null && currentFolder.exists()) {
            directoryChooser.setInitialDirectory(currentFolder);
        }
        
       File selectedFolder = directoryChooser.showDialog(null);
       if (selectedFolder != null) {
           prefsService.setBookFolder(selectedFolder);
            
            // Update button text to show selected folder
            String folderName = selectedFolder.getName();
            bookFolderButton.setText(folderName);
            
           audiobooks.clear();
           // Future enhancement: Load audiobooks from the selected folder
       }
   }

    /**
     * Creates a new audiobook with prompt to save current one if needed.
     */
    private void createNewAudiobook() {
        // Check if there's an unsaved current audiobook
        if (currentAudiobookView != null) {
            Audiobook current = currentAudiobookView.getAudiobook();
            if (!current.isEmpty()) {
                // Ask if user wants to save the current audiobook
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Unsaved Audiobook");
                alert.setHeaderText("Current audiobook has files");
                alert.setContentText("Do you want to continue without saving?");
                
                ButtonType continueButton = new ButtonType("Continue");
                ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                
                alert.getButtonTypes().setAll(continueButton, cancelButton);
                
                alert.showAndWait().ifPresent(type -> {
                    if (type != continueButton) {
                        return; // Cancel was clicked, don't create a new audiobook
                    }
                });
            }
        }
        
        // Create a new audiobook
        Audiobook audiobook = new Audiobook();
        
        // Preserve metadata from previous audiobook if available
        if (currentAudiobookView != null) {
            Audiobook previousBook = currentAudiobookView.getAudiobook();
            audiobook.setAuthor(previousBook.getAuthor());
            audiobook.setSeries(previousBook.getSeries());
            audiobook.setBookNumber(previousBook.getBookNumber() + 1); // Increment book number
        }
        
        // Create view for the new audiobook
        AudiobookView audiobookView = new AudiobookView(audiobook, prefsService, metadataService);
        audiobookView.setProcessButtonAction(event -> processCurrentAudiobook());
        centerPanel.setCenter(audiobookView);
        currentAudiobookView = audiobookView;
    }
    
    /**
     * Creates a new audiobook without asking for confirmation.
     * Used after successful processing to prepare for the next book.
     */
    private void createNewAudiobookWithoutPrompt() {
        // Create a new audiobook
        Audiobook audiobook = new Audiobook();
        
        // Preserve metadata from previous audiobook
        if (currentAudiobookView != null) {
            Audiobook previousBook = currentAudiobookView.getAudiobook();
            audiobook.setAuthor(previousBook.getAuthor());
            audiobook.setSeries(previousBook.getSeries());
            audiobook.setBookNumber(previousBook.getBookNumber() + 1); // Increment book number
            
            logger.info("Preserved metadata from previous audiobook: Author='{}', Series='{}', Book #={}", 
                    previousBook.getAuthor(), previousBook.getSeries(), audiobook.getBookNumber());
        }
        
        // Create view for the new audiobook
        AudiobookView audiobookView = new AudiobookView(audiobook, prefsService, metadataService);
        audiobookView.setProcessButtonAction(event -> processCurrentAudiobook());
        centerPanel.setCenter(audiobookView);
        currentAudiobookView = audiobookView;
    }

    /**
     * Initiates the processing of the current audiobook.
     * Validates the audiobook before processing starts.
     */
    private void processCurrentAudiobook() {
        if (currentAudiobookView == null) {
            logger.warn("Process action called with no audiobook");
            showAlert("No Audiobook", "There is no audiobook to process.", Alert.AlertType.WARNING);
            return;
        }
        
        Audiobook audiobook = currentAudiobookView.getAudiobook();
        if (audiobook.isEmpty()) {
            logger.warn("Process action called with empty audiobook");
            showAlert("Empty Audiobook", "The current audiobook has no files to process.", Alert.AlertType.WARNING);
            return;
        }
        
        logger.info("Processing audiobook: {}", audiobook.getTitle());
        
        // Format filename with book number and title
        String formattedFilename = String.format("%02d %s.m4b", 
                audiobook.getBookNumber(), audiobook.getTitle());
        
        // Determine output location based on preferences
        File bookFolder = prefsService.getBookFolder();
        if (bookFolder != null && bookFolder.exists()) {
            // Create output file in the book folder
            File outputFile = new File(bookFolder, formattedFilename);
            processAudiobookFile(audiobook, outputFile);
        } else {
            // Use last output folder if book folder is not available
            File outputFolder = prefsService.getOutputFolder();
            if (outputFolder != null && outputFolder.exists()) {
                File outputFile = new File(outputFolder, formattedFilename);
                processAudiobookFile(audiobook, outputFile);
            } else {
                // No valid output location found
                logger.warn("No valid output location available");
                showAlert("No Output Location", 
                    "Please select a book folder first by clicking 'Select Book Folder'.", 
                    Alert.AlertType.WARNING);
            }
        }
    }

    /**
     * Processes an audiobook with the specified output file.
     * Adds the audiobook to the processing queue and prepares for the next book.
     *
     * @param audiobook The audiobook to process
     * @param outputFile The destination file for the processed audiobook
     */
    private void processAudiobookFile(Audiobook audiobook, File outputFile) {
        // Save the output directory for next time
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && parentDir.exists()) {
            prefsService.setOutputFolder(parentDir);
        }
        
        logger.info("Processing audiobook to: {}", outputFile.getAbsolutePath());
        
        // Get cover image from current view
        File coverImage = currentAudiobookView.getCurrentCoverImage();
        
        // Add to processing queue and start processing
        processingPanel.processAudiobook(audiobookProcessor, audiobook, outputFile, coverImage)
            .exceptionally(ex -> {
                logger.error("Error processing audiobook", ex);
                return null;
            });
            
        // Create a new audiobook for the next book
        createNewAudiobookWithoutPrompt();
        
        logger.info("Started processing audiobook and prepared UI for next book");
    }
    
    /**
     * Helper method to show alerts to the user.
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Applies the specified theme to the application.
     * @param themeName "light" or "dark"
     */
    private void applyTheme(String themeName) {
       ObservableList<String> stylesheets = scene.getStylesheets();
       stylesheets.clear();

       try {
           if ("dark".equals(themeName)) {
               String cssPath = getClass().getResource(DARK_THEME_CSS).toExternalForm();
               stylesheets.add(cssPath);
               System.out.println("Applied dark theme: " + cssPath);
           } else {
               String cssPath = getClass().getResource(LIGHT_THEME_CSS).toExternalForm();
               stylesheets.add(cssPath);
               System.out.println("Applied light theme: " + cssPath);
           }
       } catch (Exception e) {
           System.err.println("Error applying theme: " + e.getMessage());
           e.printStackTrace();
       }
    }
}