package com.jk24.audiobookcreator.ui;

import com.jk24.audiobookcreator.model.Audiobook;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

/**
 * Custom ListCell for displaying processing items with progress bars.
 * This cell uses a manual update approach to avoid binding issues when cells are recycled.
 */
public class ProcessingItemCell extends ListCell<ProcessingItem> {
    // UI components
    private final HBox content;
    private final Label filename;
    private final ProgressBar progressBar;
    private final Label statusLabel;
    private final Button retryButton;
    
    // Items needed to track cell state
    private EventHandler<ActionEvent> retryHandler;
    private ProcessingItem currentItem; // The currently displayed item
    private String cellId = "cell-" + System.nanoTime(); // Unique ID for debugging
    
    // Properties for the currently displayed content
    private final SimpleDoubleProperty cellProgress = new SimpleDoubleProperty(0); 
    
    public ProcessingItemCell() {
        // Create UI components
        content = new HBox(10); // 10px spacing
        content.setPadding(new Insets(8));
        content.getStyleClass().addAll("processing-item", "processing-item-border");
        
        VBox fileInfoBox = new VBox(3);
        filename = new Label();
        filename.getStyleClass().add("item-filename");
        statusLabel = new Label();
        statusLabel.getStyleClass().add("item-status");
        fileInfoBox.getChildren().addAll(filename, statusLabel);
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(180); // Make progress bar wider
        progressBar.setPrefHeight(16); // Set consistent height
        progressBar.getStyleClass().add("blue-bar"); // Add a special style class
        
        // Important: Bind the progress bar to our cell's progress property
        progressBar.progressProperty().bind(cellProgress);
        
        // Create retry button
        retryButton = new Button("Retry");
        retryButton.setVisible(false); // Initially hidden
        retryButton.getStyleClass().add("retry-button");
        
        // Add components to the cell
        content.getChildren().addAll(fileInfoBox, progressBar, retryButton);
        HBox.setHgrow(fileInfoBox, Priority.ALWAYS);
    }
    
    public void setRetryHandler(EventHandler<ActionEvent> handler) {
        this.retryHandler = handler;
        retryButton.setOnAction(event -> {
            if (retryHandler != null && currentItem != null) {
                retryHandler.handle(event);
            }
        });
    }
    
    @Override
    protected void updateItem(ProcessingItem item, boolean empty) {
        // Remember what we were showing before
        ProcessingItem oldItem = currentItem;
        
        // Call the parent first
        super.updateItem(item, empty);
        
        // Clear everything if empty
        if (empty || item == null) {
            currentItem = null;
            setText(null);
            setGraphic(null);
            // Reset our progress property when not showing an item
            cellProgress.set(0);
            return;
        }
        
        // Set the current item
        currentItem = item;
        
        // We are explicitly NOT binding to item properties because of ListView cell recycling issues
        // Instead, we'll manually update values and use our own cell-specific properties
        
        // Update UI values directly - these don't change after initialization
        filename.setText(item.getFilename());
        
        // Update progress and status - ALWAYS update to current values
        updateCellState(item);
        
        // Make retry button visible if needed
        retryButton.setVisible("Error".equals(item.getStatus()));
        
        // Set the cell content
        setGraphic(content);
        
        // Log when this cell is assigned to a new item
        if (oldItem != item) {
            System.out.println("Cell " + cellId + " now showing: " + item.getFilename());
        }
    }
    
    /**
     * Updates the cell state from the item.
     * This method is called from updateItem and can be called from outside to refresh.
     */
    public void updateCellState(ProcessingItem item) {
        if (item != null && item == currentItem) {
            // Always get current progress from the item - this is critical
            double progress = item.getProgress();
            cellProgress.set(progress);
            
            // Update status
            String status = item.getStatus();
            statusLabel.setText(status);
            updateStatusStyle(status);
            
            // Debug to confirm updates are occurring
            System.out.println("Cell " + cellId + " updated: " + item.getFilename() + 
                " Progress: " + progress + ", Status: " + status);
        }
    }
    
    /**
     * Gets the current item being displayed by this cell
     */
    public ProcessingItem getCurrentItem() {
        return currentItem;
    }
    
    /**
     * Update the progress bar style based on the current status
     */
    private void updateStatusStyle(String status) {
        // Style based on status
        switch (status) {
            case "Completed":
                statusLabel.setTextFill(Color.GREEN);
                progressBar.getStyleClass().removeAll("blue-bar", "red-bar");
                progressBar.getStyleClass().add("green-bar");
                break;
            case "Processing":
                statusLabel.setTextFill(Color.BLUE);
                progressBar.getStyleClass().removeAll("green-bar", "red-bar");
                progressBar.getStyleClass().add("blue-bar");
                break;
            case "Error":
                statusLabel.setTextFill(Color.RED);
                progressBar.getStyleClass().removeAll("blue-bar", "green-bar");
                progressBar.getStyleClass().add("red-bar");
                break;
            default:
                statusLabel.setTextFill(Color.GRAY);
                progressBar.getStyleClass().removeAll("green-bar", "red-bar");
                progressBar.getStyleClass().add("blue-bar");
        }
    }
}