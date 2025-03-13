package com.jk24.audiobookcreator;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

/**
 * Custom ListCell for displaying processing items with progress bars
 */
public class ProcessingItemCell extends ListCell<ProcessingItem> {
    private final HBox content;
    private final Label filename;
    private final ProgressBar progressBar;
    private final Label statusLabel;
    private final Button retryButton;
    private EventHandler<ActionEvent> retryHandler;
    
    public ProcessingItemCell() {
        // Create UI components
        content = new HBox(10); // 10px spacing
        content.setPadding(new Insets(8));
        content.getStyleClass().addAll("processing-item", "processing-item-border"); // Add border class
        
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
            if (retryHandler != null) {
                retryHandler.handle(event);
            }
        });
    }
    
    @Override
    protected void updateItem(ProcessingItem item, boolean empty) {
        super.updateItem(item, empty);
        
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            // Clear any existing listeners
            if (statusListener != null) {
                statusListener = null;
            }
        } else {
            // Update values
            filename.setText(item.getFilename());
            
            // Bind the progress bar to the progress property for live updates
            progressBar.progressProperty().unbind(); // Unbind from previous items first
            progressBar.progressProperty().bind(item.progressProperty());
            
            // Listen for status changes to update the bar color
            if (statusListener != null) {
                item.statusProperty().removeListener(statusListener);
            }
            statusListener = (observable, oldValue, newValue) -> {
                // Update the status label text when the status changes
                System.out.println("Status changed: " + oldValue + " -> " + newValue);
                statusLabel.setText(newValue);
                updateStatusStyle(newValue);
                
                // Show/hide retry button based on status
                retryButton.setVisible("Error".equals(newValue));
            };
            item.statusProperty().addListener(statusListener);
            
            // Also update now in case the status is already set
            statusLabel.setText(item.getStatus());
            System.out.println("Initial status for cell: " + item.getStatus());
            updateStatusStyle(item.getStatus());
            
            // Show retry button if status is Error
            retryButton.setVisible("Error".equals(item.getStatus()));
            
            setGraphic(content);
        }
    }
    
    private ChangeListener<String> statusListener;
    
    /**
     * Update the progress bar style based on the current status
     */
    private void updateStatusStyle(String status) {
           // Style based on status
            switch (status) {
               case "Completed":
                   statusLabel.setTextFill(Color.GREEN);
                   // Change the progress bar to green when complete
                   progressBar.getStyleClass().removeAll("blue-bar");
                   progressBar.getStyleClass().add("green-bar");
                   break;
               case "Processing":
                   statusLabel.setTextFill(Color.BLUE);
                   // Ensure the progress bar is blue during processing
                   progressBar.getStyleClass().removeAll("green-bar");
                   progressBar.getStyleClass().add("blue-bar");
                   break;
               case "Error":
                   statusLabel.setTextFill(Color.RED);
                   // Change the progress bar to red on error
                   progressBar.getStyleClass().removeAll("blue-bar", "green-bar");
                   progressBar.getStyleClass().add("red-bar");
                   break;
               default:
                   statusLabel.setTextFill(Color.GRAY);
                   // Reset to blue bar for other states
                   progressBar.getStyleClass().removeAll("green-bar", "red-bar");
                   progressBar.getStyleClass().add("blue-bar");
       }
   }
}