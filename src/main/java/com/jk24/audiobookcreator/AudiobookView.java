package com.jk24.audiobookcreator;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UI Component that represents an audiobook editor.
 * This component provides a complete interface for managing audiobook files
 * including:
 * <p>
 * - File list with add/remove/reorder capabilities
 * - Drag and drop support for adding files
 * - Automatic file sorting
 * - Cover image selection and display
 * - Metadata editing through a child component
 * <p>
 * The component automatically handles common tasks like searching for cover images
 * in audio file directories and updating the audiobook title from added files.
 */
public class AudiobookView extends VBox {
    private final Audiobook audiobook;
    private final ObservableList<BookAudio> observableAudioFiles;
    private final ListView<BookAudio> fileListView;
    private final Button addButton, removeButton, moveUpButton, moveDownButton, processButton;
    private final PreferencesService prefsService;
    private final MetadataService metadataService;
    private VBox buttonBox;
    private MetadataEditorView metadataEditor;
    private boolean autoSortFiles = true; // Default to automatic sorting
    private BorderPane imageContainer;
    private ImageView coverImageView;
    private Button prevImageButton;
    private Button nextImageButton;
    private Button addImageButton;
    private List<File> imageFiles = new ArrayList<>();
    private int currentImageIndex = 0;

    /**
     * Creates a new audiobook view for the specified audiobook.
     * 
     * @param audiobook the audiobook to edit
     * @param prefsService the preferences service for persisting settings
     * @param metadataService the metadata service for metadata suggestions
     */
    public AudiobookView(Audiobook audiobook, PreferencesService prefsService, MetadataService metadataService) {
        this.audiobook = audiobook;
        this.prefsService = prefsService;
        this.metadataService = metadataService;

        setPadding(new Insets(10));
        setSpacing(10);

        // Create observable list backed by the audiobook's files
        observableAudioFiles = FXCollections.observableArrayList(audiobook.getAudioFiles());

        // Create file list view
        fileListView = new ListView<>(observableAudioFiles);
        fileListView.setCellFactory(lv -> new AudioFileListCell());

        // Configure selection behavior
        MultipleSelectionModel<BookAudio> selectionModel = fileListView.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.SINGLE);

        // Setup drag and drop for the list
        setupDragAndDrop();

        // Create buttons for file management
        processButton = new Button("Process");
        processButton.getStyleClass().add("primary-button");

        // Create icon buttons
        addButton = createIconButton("M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z", "Add Files"); // Plus icon
        removeButton = createIconButton("M6,19C6,20.1 6.9,21 8,21H16C17.1,21 18,20.1 18,19V7H6V19M8,9H16V19H8V9M15.5,4L14.5,3H9.5L8.5,4H5V6H19V4H15.5Z", "Remove"); // Trash icon
        removeButton.getStyleClass().add("remove-button"); // Add special class for styling

        // Create arrow buttons with SVG icons
        moveUpButton = createArrowButton("M7,15L12,10L17,15H7Z", "Move Up"); // Triangle pointing up
        moveDownButton = createArrowButton("M7,10L12,15L17,10H7Z", "Move Down"); // Triangle pointing down

        // Button actions
        addButton.setOnAction(e -> {
            selectionModel.setSelectionMode(SelectionMode.SINGLE);
            addFiles();
        });

        removeButton.setOnAction(e -> {
            selectionModel.setSelectionMode(SelectionMode.MULTIPLE);
            removeSelectedFiles();
            selectionModel.setSelectionMode(SelectionMode.SINGLE);
        });

        moveUpButton.setOnAction(e -> {
            selectionModel.setSelectionMode(SelectionMode.SINGLE);
            moveFilesUp();
        });

        moveDownButton.setOnAction(e -> {
            selectionModel.setSelectionMode(SelectionMode.SINGLE);
            moveFilesDown();
        });

        // Add Image button for adding custom images
        addImageButton = createIconButton("M21,19V5C21,3.9 20.1,3 19,3H5C3.9,3 3,3.9 3,5V19C3,20.1 3.9,21 5,21H19C20.1,21 19,20.1 19,19ZM8.5,13.5L11,16.5L14.5,12L19,18H5L8.5,13.5Z", "Add Image"); // Photo/image icon
        addImageButton.setOnAction(e -> addCustomImage());

        // Create button layout
        buttonBox = new VBox(10);
        buttonBox.getChildren().addAll(processButton, addButton, removeButton, moveUpButton, moveDownButton, addImageButton);

        // Create layout for list and buttons
        HBox listLayout = new HBox(10);
        listLayout.getChildren().addAll(fileListView, buttonBox);
        HBox.setHgrow(fileListView, Priority.ALWAYS);

        // Create image display area
        createImageDisplayArea();

        // Add components to the main layout
        metadataEditor = new MetadataEditorView(audiobook);
        
        // Create a simple HBox with minimal spacing
        HBox metadataAndImageContainer = new HBox(5); // Just 5 pixels of spacing
        metadataAndImageContainer.getChildren().addAll(metadataEditor, imageContainer);
        
        // Don't allow the image to grow
        HBox.setHgrow(imageContainer, Priority.NEVER);
        
        // Allow metadata to use only needed space
        HBox.setHgrow(metadataEditor, Priority.NEVER);
        
        // Add everything to the main layout
        getChildren().addAll(listLayout, metadataAndImageContainer);

        // Search for images in the directory of the first audio file
        if (!observableAudioFiles.isEmpty()) {
            searchForImages(observableAudioFiles.get(0).getFile());
        }
    }

    /**
     * Sets up drag and drop functionality for the file list.
     * This allows users to add audio files by dragging them from the file system.
     */
    private void setupDragAndDrop() {
        fileListView.setOnDragOver(event -> {
            if (event.getGestureSource() != fileListView &&
                event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        fileListView.setOnDragDropped(event -> {
            List<File> files = event.getDragboard().getFiles();
            boolean wasEmpty = observableAudioFiles.isEmpty();
            for (File file : files) {
                if (isAudioFile(file)) {
                    BookAudio bookAudio = new BookAudio(file);
                    audiobook.addAudioFile(bookAudio);
                    observableAudioFiles.add(bookAudio);

                    // If this is the first file added, use its name as the audiobook title
                    if (wasEmpty && observableAudioFiles.size() == 1) {
                        updateAudiobookTitleFromFile(bookAudio);
                    }

                    // Search for images in the directory of the first audio file
                    if (observableAudioFiles.size() == 1) {
                        searchForImages(bookAudio.getFile());
                    }
                }
            }
            event.setDropCompleted(true);
            event.consume();

            // Sort the files by filename after adding them through drag and drop
            if (autoSortFiles) {
                sortFilesByName();
            }
        });
    }

    /**
     * Determines if a file is a supported audio file based on its extension.
     * 
     * @param file the file to check
     * @return true if it's a supported audio file, false otherwise
     */
    private boolean isAudioFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".aac") ||
               name.endsWith(".m4a") || name.endsWith(".flac") || name.endsWith(".ogg") ||
               name.endsWith(".m4b");
    }

    /**
     * Opens a file chooser to add audio files to the audiobook.
     * Automatically sorts the files after adding them if auto-sort is enabled.
     */
    private void addFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Audio Files");

        // Set initial directory to book folder if available
        File bookFolder = prefsService.getBookFolder();
        if (bookFolder != null && bookFolder.exists()) {
            fileChooser.setInitialDirectory(bookFolder);
        }

        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.aac", "*.m4a", "*.flac", "*.ogg", "*.m4b")
        );

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(getScene().getWindow());
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            boolean wasEmpty = observableAudioFiles.isEmpty();

            for (File file : selectedFiles) {
                BookAudio bookAudio = new BookAudio(file);
                audiobook.addAudioFile(bookAudio);
                observableAudioFiles.add(bookAudio);

                // If this is the first file added, use its name as the audiobook title
                if (wasEmpty && observableAudioFiles.size() == 1) {
                    updateAudiobookTitleFromFile(bookAudio);
                }
            }

            // Sort the files by filename after adding them
            if (autoSortFiles) {
                sortFilesByName();
            }

            // Search for images in the directory of the first audio file
            if (!observableAudioFiles.isEmpty()) {
                searchForImages(observableAudioFiles.get(0).getFile());
            }
        }
    }

    /**
     * Removes the selected files from the audiobook.
     * If no files are selected, does nothing.
     */
    private void removeSelectedFiles() {
       List<BookAudio> selectedItems = fileListView.getSelectionModel().getSelectedItems();

        // If nothing selected, or selection is empty, just return
        if (selectedItems == null || selectedItems.isEmpty()) {
            return;
        }

       for (BookAudio file : selectedItems) {
           audiobook.removeAudioFile(file);
       }
       observableAudioFiles.removeAll(selectedItems);

        // Clear selection after removing
        fileListView.getSelectionModel().clearSelection();
   }

    /**
     * Moves the selected file up in the list (earlier in playback).
     * If the selected file is already at the top, does nothing.
     */
    private void moveFilesUp() {
        BookAudio selectedItem = fileListView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            int index = observableAudioFiles.indexOf(selectedItem);
            if (index > 0) {
                audiobook.moveAudioFileUp(selectedItem);
                observableAudioFiles.remove(index);
                observableAudioFiles.add(index - 1, selectedItem);
                fileListView.getSelectionModel().select(index - 1);
                fileListView.scrollTo(index - 1);
            }
        }
    }

    /**
     * Moves the selected file down in the list (later in playback).
     * If the selected file is already at the bottom, does nothing.
     */
    private void moveFilesDown() {
        BookAudio selectedItem = fileListView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            int index = observableAudioFiles.indexOf(selectedItem);
            if (index < observableAudioFiles.size() - 1) {
                audiobook.moveAudioFileDown(selectedItem);
                observableAudioFiles.remove(index);
                observableAudioFiles.add(index + 1, selectedItem);
                fileListView.getSelectionModel().select(index + 1);
                fileListView.scrollTo(index + 1);
            }
        }
    }

    /**
     * Gets the audiobook being edited.
     * 
     * @return the audiobook
     */
    public Audiobook getAudiobook() {
        return audiobook;
    }

    /**
     * Sets the action for the process button.
     * This allows the parent component to define the processing behavior.
     * 
     * @param action the action to execute when the process button is clicked
     */
    public void setProcessButtonAction(EventHandler<javafx.event.ActionEvent> action) {
        processButton.setOnAction(action);
    }

    /**
     * Updates the audiobook title based on the first file's name.
     * This helps provide a sensible default title when files are added.
     * 
     * @param bookAudio the audio file to use for the title
     */
    private void updateAudiobookTitleFromFile(BookAudio bookAudio) {
        // Get the file's title (already has extension removed)
        String fileTitle = bookAudio.getTitle();

        // Clean up the title - remove track numbers if present
        String cleanTitle = cleanupTitle(fileTitle);

        // Set the audiobook title
        audiobook.setTitle(cleanTitle);

        // Update the metadata editor to show the new title
        if (metadataEditor != null) {
            metadataEditor.updateFromAudiobook();
        }

        System.out.println("Set audiobook title from file: " + cleanTitle);
    }

    /**
     * Cleans up a filename to make it a better title.
     * Removes track numbers, underscores, and extra whitespace.
     * 
     * @param filename the filename to clean
     * @return the cleaned title
     */
    private String cleanupTitle(String filename) {
        // Remove leading track numbers (e.g., "01 - Title" becomes "Title")
        String cleaned = filename.replaceAll("^\\d+\\s*[\\-\\_\\.\\s]+\\s*", "");

        // Replace underscores with spaces
        cleaned = cleaned.replace('_', ' ');

        // Remove any extra spaces
        cleaned = cleaned.trim().replaceAll("\\s+", " ");

        return cleaned;
    }

    /**
     * Custom ListCell implementation for displaying BookAudio files.
     * Shows the title of each audio file in the list.
     */
    private static class AudioFileListCell extends javafx.scene.control.ListCell<BookAudio> {
        @Override
        protected void updateItem(BookAudio bookAudio, boolean empty) {
            super.updateItem(bookAudio, empty);

            if (empty || bookAudio == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(bookAudio.getTitle());
            }
        }
    }

    /**
     * Sorts the audio files by filename.
     * Uses a natural order comparison that handles numbers properly,
     * so "1", "2", "10" are sorted correctly instead of "1", "10", "2".
     */
    private void sortFilesByName() {
       // Create a sorted list from the current files
       List<BookAudio> sortedFiles = observableAudioFiles.stream()
           .sorted((a, b) -> {
               // Use natural order comparison for filenames
               // This handles numbers better (1, 2, 10 instead of 1, 10, 2)
               String nameA = a.getFile().getName();
               String nameB = b.getFile().getName();

               // Extract any leading numbers from filenames for better numeric sorting
               String numberPatternA = extractLeadingNumber(nameA);
               String numberPatternB = extractLeadingNumber(nameB);

               if (!numberPatternA.isEmpty() && !numberPatternB.isEmpty()) {
                   try {
                       // If both have leading numbers, compare them numerically
                       int numA = Integer.parseInt(numberPatternA);
                       int numB = Integer.parseInt(numberPatternB);
                       return Integer.compare(numA, numB);
                   } catch (NumberFormatException e) {
                       // Fall back to string comparison if parsing fails
                   }
               }

               // Default to case-insensitive string comparison
               return nameA.compareToIgnoreCase(nameB);
           })
           .toList();

       // Clear and re-add in sorted order
       if (!sortedFiles.isEmpty()) {
           // First clear from the observable list
           observableAudioFiles.clear();

           // Then clear from the audiobook
           audiobook.clearAudioFiles();

           // Finally add them back in the sorted order
           for (BookAudio audio : sortedFiles) {
               audiobook.addAudioFile(audio);
               observableAudioFiles.add(audio);
           }

           System.out.println("Sorted " + sortedFiles.size() + " audio files by filename");
       }
    }

    /**
     * Extract any leading number from a filename for better sorting.
     * For example, "01_track.mp3" would extract "01".
     * 
     * @param filename the filename to process
     * @return the extracted leading number, or an empty string if none found
     */
    private String extractLeadingNumber(String filename) {
        // Match leading numbers at the beginning of the filename
        Pattern pattern = Pattern.compile("^(\\d+)");
        Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * Creates a button with an SVG arrow icon.
     * 
     * @param svgPath the SVG path data for the arrow
     * @param tooltipText the tooltip text for the button
     * @return the created button
     */
    private Button createArrowButton(String svgPath, String tooltipText) {
        return createIconButton(svgPath, tooltipText);
    }

    /**
     * Creates a button with an SVG icon.
     * The button has a fixed size and displays the icon centered.
     * 
     * @param svgPath the SVG path data for the icon
     * @param tooltipText the tooltip text for the button
     * @return the created button
     */
    private Button createIconButton(String svgPath, String tooltipText) {
       Button button = new Button();

       // Create SVG icon
       SVGPath arrow = new SVGPath();
       arrow.setContent(svgPath);
       arrow.getStyleClass().add("svg-icon");

       // Set the graphic and style the button
       button.setGraphic(arrow);
       button.setTooltip(new Tooltip(tooltipText));
       button.setPrefWidth(40);
       button.setMaxWidth(40);
       button.setMinWidth(40);
       button.setAlignment(Pos.CENTER);
       button.getStyleClass().add("icon-button");

       return button;
   }

    /**
     * Creates the image display area with navigation buttons.
     * This container will show cover images and provide controls
     * to navigate between them.
     */
    private void createImageDisplayArea() {
        imageContainer = new BorderPane();
        imageContainer.setPrefHeight(220);
        imageContainer.setPrefWidth(200);
        imageContainer.setMaxHeight(220);
        imageContainer.setMinWidth(200);
        imageContainer.getStyleClass().add("image-container");
        imageContainer.setVisible(false); // Initially hidden until images are found
        
        coverImageView = new ImageView();
        coverImageView.setPreserveRatio(true);
        coverImageView.setFitHeight(180);
        coverImageView.setFitWidth(180);
        coverImageView.getStyleClass().add("cover-image");
        
        // Create navigation buttons
        HBox navigationButtons = new HBox(10);
        navigationButtons.setAlignment(Pos.CENTER);
        
        prevImageButton = createIconButton("M15,6H3V18H15V6M10,9V15L5,12L10,9Z", "Previous Image"); // Left arrow
        nextImageButton = createIconButton("M9,6H21V18H9V6M14,9V15L19,12L14,9Z", "Next Image"); // Right arrow

        navigationButtons.getChildren().addAll(prevImageButton, nextImageButton);

        imageContainer.setCenter(coverImageView);
        imageContainer.setBottom(navigationButtons);

        // Set up button actions
        prevImageButton.setOnAction(e -> showPreviousImage());
        nextImageButton.setOnAction(e -> showNextImage());
        addImageButton.setOnAction(e -> addCustomImage());
    }
    
    /**
     * Searches for image files in the directory of the audio file.
     * This attempts to find cover images automatically when audio files are added.
     * Images with "cover" or "folder" in the name are prioritized.
     * 
     * @param audioFile the audio file whose directory should be searched
     */
    private void searchForImages(File audioFile) {
        imageFiles.clear();
        currentImageIndex = 0;
        
        if (audioFile == null || !audioFile.exists()) {
            return;
        }
        
        // Get the directory of the audio file
        File directory = audioFile.getParentFile();
        if (directory == null || !directory.isDirectory()) {
            return;
        }
        
        System.out.println("Searching for images in: " + directory.getAbsolutePath());
        
        // Look for image files
        File[] files = directory.listFiles((dir, name) -> {
            String lowercase = name.toLowerCase();
            return lowercase.endsWith(".jpg") || lowercase.endsWith(".jpeg") || 
                   lowercase.endsWith(".png") || lowercase.endsWith(".gif") ||
                   lowercase.endsWith(".bmp");
        });
        
        // Sort files to prioritize cover images
        if (files != null && files.length > 0) {
            // Convert to list for sorting
            List<File> fileList = Arrays.asList(files);
            
            // Sort so that likely cover images appear first
            Collections.sort(fileList, (a, b) -> {
                String nameA = a.getName().toLowerCase();
                String nameB = b.getName().toLowerCase();
                
                // Check for common cover image names
                boolean aCover = nameA.contains("cover") || nameA.contains("folder");
                boolean bCover = nameB.contains("cover") || nameB.contains("folder");
                
                if (aCover && !bCover) return -1;
                if (!aCover && bCover) return 1;
                
                // Secondary sort by name
                return nameA.compareTo(nameB);
            });
            
            imageFiles.addAll(fileList);
            System.out.println("Found " + imageFiles.size() + " image(s) in folder");
            
            // Debug: list found images
            for (int i = 0; i < imageFiles.size(); i++) {
                System.out.println("  Image " + (i+1) + ": " + imageFiles.get(i).getName());
            }
            
            // Show the first image
            showCurrentImage();
        } else {
            System.out.println("No images found in directory");
            imageContainer.setVisible(false);
        }
    }
    
    /**
     * Shows the current image in the image view.
     * Updates navigation buttons based on the current position in the image list.
     */
    private void showCurrentImage() {
        if (imageFiles.isEmpty()) {
            imageContainer.setVisible(false);
            return;
        }
        
        try {
            File imageFile = imageFiles.get(currentImageIndex);
            Image image = new Image(imageFile.toURI().toString());
            coverImageView.setImage(image);
            imageContainer.setVisible(true);
            
            // Update navigation buttons
            updateNavigationButtons();
            
            System.out.println("Displaying image " + (currentImageIndex + 1) + " of " + imageFiles.size() + 
                ": " + imageFile.getName());
        } catch (Exception e) {
            System.err.println("Error loading image: " + e.getMessage());
            imageContainer.setVisible(false);
        }
    }
    
    /**
     * Updates the state of the navigation buttons based on current position.
     * Disables the previous button at the first image and the next button at the last image.
     */
    private void updateNavigationButtons() {
        // Previous button only active if we're not at the first image
        prevImageButton.setDisable(imageFiles.isEmpty() || currentImageIndex == 0);
        
        // Next button only active if we're not at the last image
        nextImageButton.setDisable(imageFiles.isEmpty() || currentImageIndex >= imageFiles.size() - 1);
    }
    
    /**
     * Shows the previous image in the list.
     * If already at the first image, does nothing.
     */
    private void showPreviousImage() {
        if (currentImageIndex > 0) {
            currentImageIndex--;
            showCurrentImage();
        }
    }
    
    /**
     * Shows the next image in the list.
     * If already at the last image, does nothing.
     */
    private void showNextImage() {
        if (currentImageIndex < imageFiles.size() - 1) {
            currentImageIndex++;
            showCurrentImage();
        }
    }
    
    /**
     * Opens a file chooser to add a custom image.
     * If the selected image is already in the list, it's just shown rather than added again.
     */
    private void addCustomImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        
        // Set up file filters for image files
        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
            "Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp");
        fileChooser.getExtensionFilters().add(imageFilter);
        
        // Try to set initial directory to same folder as existing images or audio files
        if (!imageFiles.isEmpty()) {
            File currentImage = imageFiles.get(currentImageIndex);
            File parentDir = currentImage.getParentFile();
            if (parentDir != null && parentDir.exists()) {
                fileChooser.setInitialDirectory(parentDir);
            }
        } else if (!observableAudioFiles.isEmpty()) {
            File audioFile = observableAudioFiles.get(0).getFile();
            File parentDir = audioFile.getParentFile();
            if (parentDir != null && parentDir.exists()) {
                fileChooser.setInitialDirectory(parentDir);
            }
        }
        
        // Show file chooser and process selected file
        File selectedFile = fileChooser.showOpenDialog(getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Check if this image is already in our list
                boolean alreadyExists = false;
                for (File existingImage : imageFiles) {
                    if (existingImage.getAbsolutePath().equals(selectedFile.getAbsolutePath())) {
                        // Image already in list, just show it
                        currentImageIndex = imageFiles.indexOf(existingImage);
                        alreadyExists = true;
                        break;
                    }
                }
                
                // If it's a new image, add it to our list
                if (!alreadyExists) {
                    imageFiles.add(selectedFile);
                    currentImageIndex = imageFiles.size() - 1;
                }
                
                // Make sure the container is visible since we now have at least one image
                imageContainer.setVisible(true);
                
                // Show the selected image
                showCurrentImage();
                
                System.out.println("Added custom image: " + selectedFile.getName());
            } catch (Exception e) {
                System.err.println("Error adding custom image: " + e.getMessage());
                e.printStackTrace();
                
                // Show an error dialog
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Image Error");
                alert.setHeaderText("Error Loading Image");
                alert.setContentText("Could not load the selected image file: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    /**
     * Gets the currently selected cover image.
     * This is the image that will be used when processing the audiobook.
     * 
     * @return the current cover image file, or null if no images are available
     */
    public File getCurrentCoverImage() {
        if (!imageFiles.isEmpty() && currentImageIndex >= 0 && currentImageIndex < imageFiles.size()) {
            return imageFiles.get(currentImageIndex);
        }
        return null;
    }
}