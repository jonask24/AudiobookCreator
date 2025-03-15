package com.jk24.audiobookcreator.ui;

import com.jk24.audiobookcreator.model.Audiobook;
import com.jk24.audiobookcreator.processor.AudiobookProcessor;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;

/**
 * Panel for displaying and tracking audiobook processing status.
 * This component shows a list of processing items with their current status
 * and provides UI controls for managing the processing queue.
 * <p>
 * Features:
 * - Live progress tracking for each audiobook
 * - Retry capability for failed items
 * - Queue management (clear completed)
 * - Expandable layout
 */
public class ProcessingPanel extends VBox {
    private final ObservableList<ProcessingItem> processingItems = FXCollections.observableArrayList();
    private final ListView<ProcessingItem> processingListView;
    private final Map<Audiobook, ProcessingItem> itemMap = new HashMap<>();
    private AudiobookProcessor processor;
    
    /**
     * Creates a new processing panel with a list view and controls.
     */
    public ProcessingPanel() {
        setPadding(new Insets(10));
        setSpacing(10);
        
        // Set preferred height but allow expansion
        setPrefHeight(408); // Default starting height
        // Remove max height restriction to allow expansion
        
        // Add a style class instead of hardcoded styles
        getStyleClass().add("processing-panel");
        
        // Title
        Label titleLabel = new Label("Processing Status");
        titleLabel.getStyleClass().add("panel-title");
        
        // List view
        processingListView = new ListView<>(processingItems);
        processingListView.setCellFactory(lv -> {
            ProcessingItemCell cell = new ProcessingItemCell();
            cell.setRetryHandler(event -> {
                ProcessingItem item = cell.getItem();
                if (item != null) {
                    retryProcessing(item);
                }
            });
            return cell;
        });
        processingListView.setPrefHeight(308); // Default height
        processingListView.setMinHeight(200); // Minimum reasonable height
        VBox.setVgrow(processingListView, Priority.ALWAYS); // Allow growing with container
        processingListView.getStyleClass().add("processing-list"); // Add a class for styling
        
        // Clear completed button
        Button clearCompletedButton = new Button("Clear Completed");
        clearCompletedButton.setOnAction(e -> clearCompletedItems());
        
        getChildren().addAll(titleLabel, processingListView, clearCompletedButton);
    }
    
    /**
     * Add an audiobook to the processing queue
     */
    public ProcessingItem addProcessingItem(Audiobook audiobook) {
        if (itemMap.containsKey(audiobook)) {
            return itemMap.get(audiobook);
        }
        
        ProcessingItem item = new ProcessingItem(audiobook);
        processingItems.add(item);
        itemMap.put(audiobook, item);
        return item;
    }
    
    /**
     * Remove an audiobook from the processing queue
     */
    public void removeProcessingItem(Audiobook audiobook) {
        ProcessingItem item = itemMap.remove(audiobook);
        if (item != null) {
            processingItems.remove(item);
        }
    }
    
    /**
     * Process an audiobook and track progress
     */
    public CompletableFuture<File> processAudiobook(AudiobookProcessor processor, Audiobook audiobook, File outputFile, File coverImage) {
        this.processor = processor; // Store processor for retries
        ProcessingItem item = addProcessingItem(audiobook);
        
        // Set the actual output filename and store retry information
        item.setFilename(outputFile.getName());
        item.setOutputFile(outputFile);
        item.setCoverImage(coverImage);
        
        // Set the initial progress and status
        Platform.runLater(() -> {
            item.setProgress(0.01); // Show a small progress to indicate it's starting
            item.setStatus("Starting");
        });
       
       // Process the audiobook and update progress
       return processor.processAudiobook(audiobook, outputFile, coverImage, progress -> {
           // Ensure the update happens on the JavaFX UI thread
           Platform.runLater(() -> {
               // Debug the progress updates
               System.out.println("Progress update for " + audiobook.getTitle() + ": " + progress);
                
                // Store a reference to the specific item we're updating
                final ProcessingItem specificItem = itemMap.get(audiobook);
                
                // Make sure we don't get "stuck" at values too close to each other
                // Use the stored reference to ensure we're working with the right item
                if (specificItem != null) {
                    double currentProgress = specificItem.getProgress();
                    
                    // Always update for completion and for significant changes
                    if (progress >= 0.999) {
                       // Explicitly set to 1.0 for completion
                       specificItem.setProgress(1.0);
                       System.out.println("Setting progress 1.0 for " + audiobook.getTitle() + " (should trigger status 'Completed')");
                        // Force a status update if needed
                        if (!"Completed".equals(specificItem.getStatus())) {
                            System.out.println("Status not updated automatically, forcing update to 'Completed'");
                            specificItem.setStatus("Completed");
                        }
                    } else if (progress < 0.02 || progress > 0.98 || Math.abs(progress - currentProgress) >= 0.01) {
                        // Normal update for significant changes
                        specificItem.setProgress(progress);
                    }
                }
           });
       }).exceptionally(ex -> {
           // Handle errors
           Platform.runLater(() -> {
                System.err.println("Error processing " + audiobook.getTitle() + ": " + ex.getMessage());
                ex.printStackTrace();
               item.setStatus("Error");
           });
           return null;
        });
    }
    
    /**
     * Retry processing an item that previously failed
     */
    private void retryProcessing(ProcessingItem item) {
        if (processor == null) {
            System.err.println("Cannot retry: processor not available");
            return;
        }
        
        // Reset the item status for retry
        item.resetForRetry();
        
        Audiobook audiobook = item.getAudiobook();
        File outputFile = item.getOutputFile();
        File coverImage = item.getCoverImage();
        
        System.out.println("Retrying processing for: " + audiobook.getTitle());
        
        // Process again with same parameters
        processor.processAudiobook(audiobook, outputFile, coverImage, progress -> {
            Platform.runLater(() -> {
                // Store a reference to the specific item we're updating
                final ProcessingItem specificItem = itemMap.get(audiobook);
                
                if (specificItem != null) {
                    double currentProgress = specificItem.getProgress();
                    
                    if (progress >= 0.999) {
                        specificItem.setProgress(1.0);
                        if (!"Completed".equals(specificItem.getStatus())) {
                            specificItem.setStatus("Completed");
                        }
                    } else if (progress < 0.02 || progress > 0.98 || Math.abs(progress - currentProgress) >= 0.01) {
                        specificItem.setProgress(progress);
                    }
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                System.err.println("Error retrying processing for " + audiobook.getTitle() + ": " + ex.getMessage());
                ex.printStackTrace();
                item.setStatus("Error");
            });
            return null;
        });
    }
    
    /**
     * Clear all completed items
     */
    public void clearCompletedItems() {
        processingItems.removeIf(item -> "Completed".equals(item.getStatus()));
        // Update the item map
        itemMap.entrySet().removeIf(entry -> !processingItems.contains(entry.getValue()));
    }
}