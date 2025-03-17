package com.jk24.audiobookcreator.ui;

import com.jk24.audiobookcreator.model.Audiobook;
import com.jk24.audiobookcreator.processor.AudiobookProcessor;
import com.jk24.audiobookcreator.service.PreferencesService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javafx.application.Platform;
import javafx.scene.control.ListView;

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
    private final ConcurrentHashMap<UUID, ProcessingItem> itemMap = new ConcurrentHashMap<>();
    private AudiobookProcessor processor;
    private PreferencesService preferencesService;

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
                ProcessingItem item = cell.getCurrentItem();
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
     * Add an audiobook to the processing queue with a default pending state.
     * This is mainly used for testing or for manual queue management.
     * 
     * @param audiobook the audiobook to add to the queue
     * @return the created processing item
     */
    public ProcessingItem addProcessingItem(Audiobook audiobook) {
        UUID uuid = UUID.randomUUID();
        ProcessingItem item = new ProcessingItem(audiobook, uuid);
        
        // Add to both collections
        processingItems.add(item);
        itemMap.put(uuid, item);
        
        return item;
    }
    
    /**
     * Remove an audiobook from the processing queue
     */
    public void removeProcessingItem(Audiobook audiobook) {
        // Find the item by audiobook
        itemMap.entrySet().removeIf(entry -> entry.getValue().getAudiobook().equals(audiobook));
        processingItems.removeIf(item -> item.getAudiobook().equals(audiobook));
    }

    /**
     * Process an audiobook and track progress
     */
    public CompletableFuture<File> processAudiobook(AudiobookProcessor processor, Audiobook audiobook, File outputFile, File coverImage, PreferencesService preferencesService) {
        this.processor = processor; // Store processor for retries
        this.preferencesService = preferencesService;

        // Get audio settings directly from the audiobook
        int bitRate = audiobook.getAudioBitRate();
        int samplingRate = audiobook.getAudioSamplingRate();
        String qualityName = audiobook.getAudioQuality();
        
        // Log the audio quality settings that will be used for this processing job
        System.out.println("----------------------------------------------------");
        System.out.println("[" + outputFile.getName() + "] Processing with audiobook-specific audio settings:");
        System.out.println("  - Quality preset: " + qualityName);
        System.out.println("  - Bit Rate: " + bitRate + " bps");
        System.out.println("  - Sampling Rate: " + samplingRate + " Hz");
        System.out.println("----------------------------------------------------");

        // Record the time when this processing started
        final long startTimeMs = System.currentTimeMillis();

        // Create a unique ID for this processing job
        final UUID processId = UUID.randomUUID();

        // Create a strongly referenced item and add it to the display list
        final ProcessingItem item = new ProcessingItem(audiobook, processId);

        // Store the filename to use in logs
        final String filenameForLogs = outputFile.getName();

        // Add the item to our collections on the UI thread
        Platform.runLater(() -> {
            // Initialize the item
            item.setFilename(outputFile.getName());
            item.setOutputFile(outputFile);
            item.setCoverImage(coverImage);
            item.setStatus("Starting");
            item.setProgress(0.01);

            // Add to collections
            processingItems.add(item);
            itemMap.put(processId, item);

            // Ensure this item is visible
            processingListView.scrollTo(item);
            processingListView.refresh();

            System.out.println("[" + filenameForLogs + "] Created ProcessingItem, initial progress 0.01");
        });

        // Special flags for this processing job
        final boolean[] isM4bOptimizationPath = new boolean[1];
        isM4bOptimizationPath[0] = false;
        final double[] lastProgressValue = new double[1];
        lastProgressValue[0] = 0.0;
        final AtomicLong lastUpdateMs = new AtomicLong(System.currentTimeMillis());

        // Direct progress handler with direct reference to this item
        java.util.function.Consumer<Double> progressHandler = progress -> {
            // Keep track of the book filename for debugging
            String itemName = filenameForLogs;

            // Detect M4B optimization path
            if (progress == 0.01 && lastProgressValue[0] == 0.0) {
                lastProgressValue[0] = progress;
            } else if (progress == 0.1 && lastProgressValue[0] == 0.01) {
                lastProgressValue[0] = progress;
            } else if (progress >= 0.4 && lastProgressValue[0] == 0.1) {
                isM4bOptimizationPath[0] = true;
                lastProgressValue[0] = progress;
            } else {
                lastProgressValue[0] = progress;
            }

            // Throttling logic
            long now = System.currentTimeMillis();
            long lastUpdate = lastUpdateMs.get();
            long updateThreshold = isM4bOptimizationPath[0] ? 50 : 
                (progress < 0.1 || progress > 0.9) ? 100 :
                (now - startTimeMs > 30000) ? 1000 : 250;

            // Skip if updated too recently
            if (now - lastUpdate < updateThreshold) {
                return;
            }

            // Update timestamp
            lastUpdateMs.set(now);

            // Apply the update on UI thread
            Platform.runLater(() -> {
                // CRITICAL FIX: Get the item directly from our map using the process ID
                // This ensures we always update the correct item even if UI cells get recycled
                ProcessingItem targetItem = itemMap.get(processId);

                if (targetItem != null) {
                    System.out.println("[" + itemName + "] Progress update: " + progress + 
                            (isM4bOptimizationPath[0] ? " (M4B optimization path)" : ""));

                    // Update the item's progress
                    boolean isHighProgress = isM4bOptimizationPath[0] ? progress >= 0.95 : progress >= 0.99;

                    if (isHighProgress) {
                        targetItem.setProgress(1.0);
                        String currentStatus = targetItem.getStatus();
                        if (!currentStatus.equals("Completed")) {
                            targetItem.setStatus("Completed");
                            System.out.println("[" + itemName + "] Status set to Completed");
                        }
                    } else {
                        targetItem.setProgress(progress);
                    }

                    // IMPORTANT: Force the ListView to refresh all cells
                    // This ensures all visible cells show the correct progress
                    processingListView.refresh();
                } else {
                    System.out.println("[" + itemName + "] WARNING: Item not found for progress update: " + progress);
                }
            });
        };

        // Process the audiobook with our handler - pass audiobook for its quality settings
        return processor.processAudiobook(audiobook, outputFile, coverImage, progressHandler, preferencesService)
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    System.err.println("[" + filenameForLogs + "] ERROR: " + ex.getMessage());

                    // Get the item directly from our map
                    ProcessingItem targetItem = itemMap.get(processId);
                    if (targetItem != null) {
                        targetItem.setStatus("Error");
                        processingListView.refresh();
                    }
                });
                return null;
            });
    }

    /**
     * Overloaded method that ensures preferencesService is properly initialized
     * Modified to make sure the call goes to the full method with safety checks
     */
    public CompletableFuture<File> processAudiobook(AudiobookProcessor processor, Audiobook audiobook, File outputFile, File coverImage) {
        // Use the instance variable preferencesService instead of passing null
        // This ensures audio quality settings will be properly applied
        return processAudiobook(processor, audiobook, outputFile, coverImage, this.preferencesService);
    }

    /**
     * Retry processing an item that previously failed
     */
    private void retryProcessing(ProcessingItem item) {
        if (processor == null) {
            System.err.println("Cannot retry: processor not available");
            return;
        }

        // Get references to the necessary data
        final Audiobook audiobook = item.getAudiobook();
        final UUID processId = item.getUuid(); // Get the UUID for item identification
        final File outputFile = item.getOutputFile();
        final File coverImage = item.getCoverImage();
        final String filenameForLogs = outputFile.getName();

        // Reset the item status for retry
        Platform.runLater(() -> {
            item.resetForRetry();
            processingListView.refresh();
            System.out.println("[" + filenameForLogs + "] Retry - reset item status");
        });

        System.out.println("[" + filenameForLogs + "] Retrying processing");

        // Get audio settings directly from the audiobook for consistency
        int bitRate = audiobook.getAudioBitRate();
        int samplingRate = audiobook.getAudioSamplingRate();
        String qualityName = audiobook.getAudioQuality();

        // Log the audio quality settings that will be used for this retry
        System.out.println("----------------------------------------------------");
        System.out.println("[" + filenameForLogs + "] Retrying with audiobook-specific audio settings:");
        System.out.println("  - Quality preset: " + qualityName);
        System.out.println("  - Bit Rate: " + bitRate + " bps");
        System.out.println("  - Sampling Rate: " + samplingRate + " Hz");
        System.out.println("----------------------------------------------------");

        // Tracking state for this retry operation
        final long startTimeMs = System.currentTimeMillis();
        final AtomicLong lastUpdateMs = new AtomicLong(System.currentTimeMillis());

        // Create a direct progress handler that uses the item's UUID to identify it
        java.util.function.Consumer<Double> progressHandler = progress -> {
            // Throttling logic
            long now = System.currentTimeMillis();
            long lastUpdate = lastUpdateMs.get();
            long updateThreshold = (progress < 0.1 || progress > 0.9) ? 100 :
                (now - startTimeMs > 30000) ? 1000 : 250;

            // Skip update if not enough time has passed
            if (now - lastUpdate < updateThreshold) {
                return;
            }

            // Update timestamp
            lastUpdateMs.set(now);

            Platform.runLater(() -> {
                // CRITICAL FIX: Look up the item directly using its UUID
                // This ensures we always update the correct item
                ProcessingItem targetItem = itemMap.get(processId);

                if (targetItem != null) {
                    System.out.println("[" + filenameForLogs + "] Retry progress: " + progress);

                    // Update the item's progress
                    if (progress >= 0.95) {
                        targetItem.setProgress(1.0);
                        if (!targetItem.getStatus().equals("Completed")) {
                            targetItem.setStatus("Completed");
                            System.out.println("[" + filenameForLogs + "] Status set to Completed");
                        }
                    } else {
                        targetItem.setProgress(progress);
                    }

                    // Force refresh
                    processingListView.refresh();
                } else {
                    System.out.println("[" + filenameForLogs + "] WARNING: Item not found for retry progress update");
                }
            });
        };

        // Process with our handler - using audiobook's quality settings
        processor.processAudiobook(audiobook, outputFile, coverImage, progressHandler, preferencesService)
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    System.err.println("[" + filenameForLogs + "] ERROR during retry: " + ex.getMessage());

                    // Get the item using its UUID
                    ProcessingItem targetItem = itemMap.get(processId);
                    if (targetItem != null) {
                        targetItem.setStatus("Error");
                        processingListView.refresh();
                    }
                });
                return null;
            });
    }

    /**
     * Clear all completed items
     */
    public void clearCompletedItems() {
        // First get all items to remove
        List<ProcessingItem> itemsToRemove = processingItems.stream()
            .filter(item -> "Completed".equals(item.getStatus()))
            .collect(java.util.stream.Collectors.toList());
            
        // Remove from the observable list
        processingItems.removeAll(itemsToRemove);
        
        // Remove from the item map - use UUID for safe removal
        for (ProcessingItem item : itemsToRemove) {
            itemMap.remove(item.getUuid());
        }
        
        // Refresh all visible cells after removing items
        refreshVisibleCells();
    }
    
    /**
     * Force a refresh of all visible cells to ensure they display the correct state.
     * This solves the issue where cells may show incorrect progress information.
     */
    private void refreshVisibleCells() {
        // Force ListView to refresh all cells - this is the most reliable approach
        processingListView.refresh();
    }

    /**
     * Sets the preferences service to use for audio settings.
     */
    public void setPreferencesService(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }
}