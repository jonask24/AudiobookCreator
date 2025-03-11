package com.jk24.audiobookcreator;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.io.File;

/**
 * Represents an audiobook that is being processed, with status tracking.
 * This class is designed for use with JavaFX UI binding, providing observable
 * properties that automatically update the UI when changed. It maintains:
 * - Progress information (0.0 to 1.0)
 * - Status information ("Pending", "Processing", "Completed", "Error")
 * - File information for retry operations
 */
public class ProcessingItem {
    private final Audiobook audiobook;
    private final StringProperty filename = new SimpleStringProperty();
    private final DoubleProperty progress = new SimpleDoubleProperty(0.0);
    private final StringProperty status = new SimpleStringProperty("Pending");
    
    // Store retry information
    private final ObjectProperty<File> outputFile = new SimpleObjectProperty<>();
    private final ObjectProperty<File> coverImage = new SimpleObjectProperty<>();
    
    /**
     * Creates a new processing item for the specified audiobook.
     * Initially sets the filename from the audiobook title and
     * status to "Pending".
     * 
     * @param audiobook the audiobook being processed
     */
    public ProcessingItem(Audiobook audiobook) {
        this.audiobook = audiobook;
        // We'll set the filename explicitly later, but initialize with a default
        this.filename.set(audiobook.getTitle() + ".m4b");
    }
    
    /**
     * Gets the audiobook being processed.
     * 
     * @return the audiobook
     */
    public Audiobook getAudiobook() {
        return audiobook;
    }
    
    /**
     * Gets the output filename.
     * 
     * @return the output filename
     */
    public String getFilename() {
        return filename.get();
    }
    
    /**
     * Gets the filename property for binding.
     * 
     * @return the observable filename property
     */
    public StringProperty filenameProperty() {
        return filename;
    }
    
    /**
     * Sets the output filename.
     * 
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename.set(filename);
    }
    
    /**
     * Gets the current progress value (0.0 to 1.0).
     * 
     * @return the progress value
     */
    public double getProgress() {
        return progress.get();
    }
    
    /**
     * Gets the progress property for binding.
     * 
     * @return the observable progress property
     */
    public DoubleProperty progressProperty() {
        return progress;
    }
    
    /**
     * Sets the progress value and updates the status based on the progress.
     * - 0 or less: "Pending"
     * - 0.01 to 0.99: "Processing"
     * - 1.0: "Completed"
     * 
     * @param progress the progress value to set (0.0 to 1.0)
     */
    public void setProgress(double progress) {
        this.progress.set(progress);
        if (progress <= 0) {
            setStatus("Pending");
        } else if (progress >= 1.0) {
            // Always explicitly set to "Completed" when progress is complete
            String currentStatus = getStatus();
            if (!"Completed".equals(currentStatus)) {
                setStatus("Completed");
            }
        } else {
            setStatus("Processing");
        }
    }
    
    /**
     * Gets the current status.
     * 
     * @return the status string
     */
    public String getStatus() {
        return status.get();
    }
    
    /**
     * Gets the status property for binding.
     * 
     * @return the observable status property
     */
    public StringProperty statusProperty() {
        return status;
    }
    
    /**
     * Sets the status directly.
     * This can be used to set custom statuses like "Error" that
     * aren't automatically set by progress changes.
     * 
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status.set(status);
    }
    
    /**
     * Gets the output file for retry operations.
     * 
     * @return the output file
     */
    public File getOutputFile() {
        return outputFile.get();
    }
    
    /**
     * Gets the output file property for binding.
     * 
     * @return the observable output file property
     */
    public ObjectProperty<File> outputFileProperty() {
        return outputFile;
    }
    
    /**
     * Sets the output file for retry operations.
     * 
     * @param outputFile the output file to set
     */
    public void setOutputFile(File outputFile) {
        this.outputFile.set(outputFile);
    }
    
    /**
     * Gets the cover image file for retry operations.
     * 
     * @return the cover image file
     */
    public File getCoverImage() {
        return coverImage.get();
    }
    
    /**
     * Gets the cover image property for binding.
     * 
     * @return the observable cover image property
     */
    public ObjectProperty<File> coverImageProperty() {
        return coverImage;
    }
    
    /**
     * Sets the cover image file for retry operations.
     * 
     * @param coverImage the cover image to set
     */
    public void setCoverImage(File coverImage) {
        this.coverImage.set(coverImage);
    }
    
    /**
     * Resets the item for retry after a processing error.
     * Sets progress back to 0 and status to "Pending".
     */
    public void resetForRetry() {
        setProgress(0.0);
        setStatus("Pending");
    }
}