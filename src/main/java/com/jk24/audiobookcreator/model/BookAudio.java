package com.jk24.audiobookcreator.model;

import java.io.File;
import java.time.Duration;

/**
 * Represents an audio file in the system with its metadata.
 * This class wraps a physical audio file and provides access to its
 * properties including duration, title and artist information.
 * <p>
 * The title is initially extracted from the filename, but can be
 * overridden with metadata information.
 */
public class BookAudio {
    private final File file;
    private Duration duration;
    private String title;
    private String artist;
    
    /**
     * Creates a new BookAudio instance for the specified file.
     * The title is automatically extracted from the filename.
     * 
     * @param file the audio file to represent
     */
    public BookAudio(File file) {
        this.file = file;
        this.title = extractTitleFromFilename(file.getName());
    }
    
    /**
     * Gets the underlying audio file.
     * 
     * @return the file object
     */
    public File getFile() {
        return file;
    }
    
    /**
     * Gets the title of the audio file.
     * 
     * @return the title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Sets the title of the audio file.
     * 
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * Gets the artist of the audio file.
     * 
     * @return the artist
     */
    public String getArtist() {
        return artist;
    }
    
    /**
     * Sets the artist of the audio file.
     * 
     * @param artist the artist to set
     */
    public void setArtist(String artist) {
        this.artist = artist;
    }
    
    /**
     * Gets the duration of the audio file.
     * 
     * @return the duration
     */
    public Duration getDuration() {
        return duration;
    }
    
    /**
     * Sets the duration of the audio file.
     * 
     * @param duration the duration to set
     */
    public void setDuration(Duration duration) {
        this.duration = duration;
    }
    
    /**
     * Returns a string representation of this audio file.
     * Currently returns the filename for display in lists.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        return file.getName();
    }
    
    /**
     * Extracts a title from the given filename by removing the file extension.
     * For example, "Chapter 1.mp3" becomes "Chapter 1".
     * 
     * @param filename the filename to process
     * @return the extracted title
     */
    private String extractTitleFromFilename(String filename) {
        // Remove file extension if present
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(0, lastDotIndex);
        }
        return filename;
    }
}
