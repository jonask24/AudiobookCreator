package com.jk24.audiobookcreator.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an audiobook with its metadata and collection of audio files.
 * This class is the central model object for the application, containing
 * all the information needed to process and generate an audiobook file.
 * <p>
 * Key features:
 * - Metadata storage (title, author, series, book number)
 * - Audio file collection management with reordering capabilities
 * - Immutable access to the audio file collection for thread safety
 */
public class Audiobook {
    private String title = "";
    private String author = "";
    private String series = "";
    private int bookNumber = 1;
    private final List<BookAudio> audioFiles = new ArrayList<>();
    
    /**
 * Creates a new empty audiobook with default values.
 * Initial book number is set to 1, and other fields are empty strings.
 */
public Audiobook() {
    // Default constructor with initial values
}

// Getters and setters for metadata fields
    
/**
 * Gets the title of the audiobook.
 * 
 * @return the audiobook title
 */
public String getTitle() {
    return title;
}
    
/**
 * Sets the title of the audiobook.
 * 
 * @param title the title to set
 */
public void setTitle(String title) {
    this.title = title;
}
    
/**
 * Gets the author of the audiobook.
 * 
 * @return the audiobook author
 */
public String getAuthor() {
    return author;
}
    
/**
 * Sets the author of the audiobook.
 * 
 * @param author the author to set
 */
public void setAuthor(String author) {
    this.author = author;
}
    
/**
 * Gets the series name of the audiobook.
 * 
 * @return the series name
 */
public String getSeries() {
    return series;
}
    
/**
 * Sets the series name of the audiobook.
 * 
 * @param series the series name to set
 */
public void setSeries(String series) {
    this.series = series;
}
    
/**
 * Gets the book number in the series.
 * 
 * @return the book number
 */
public int getBookNumber() {
    return bookNumber;
}
    
/**
 * Sets the book number in the series.
 * 
 * @param bookNumber the book number to set
 */
public void setBookNumber(int bookNumber) {
    this.bookNumber = bookNumber;
}
    
/**
 * Adds an audio file to the audiobook.
 * The file will be appended to the end of the current list.
 * 
 * @param bookAudio the audio file to add
 */
public void addAudioFile(BookAudio bookAudio) {
    audioFiles.add(bookAudio);
}
    
/**
 * Removes an audio file from the audiobook.
 * 
 * @param bookAudio the audio file to remove
 */
public void removeAudioFile(BookAudio bookAudio) {
    audioFiles.remove(bookAudio);
}
    
/**
 * Moves an audio file up in the sequence (earlier in playback).
 * If the file is already at the top, no change occurs.
 * 
 * @param bookAudio the audio file to move up
 */
public void moveAudioFileUp(BookAudio bookAudio) {
    int index = audioFiles.indexOf(bookAudio);
    if (index > 0) {
        Collections.swap(audioFiles, index, index - 1);
    }
}
    
/**
 * Moves an audio file down in the sequence (later in playback).
 * If the file is already at the bottom, no change occurs.
 * 
 * @param bookAudio the audio file to move down
 */
public void moveAudioFileDown(BookAudio bookAudio) {
    int index = audioFiles.indexOf(bookAudio);
    if (index >= 0 && index < audioFiles.size() - 1) {
        Collections.swap(audioFiles, index, index + 1);
    }
}
    
/**
 * Returns an unmodifiable view of the audio files.
 * This prevents external code from modifying the collection directly
 * while still providing access to the contents.
 * 
 * @return an unmodifiable list of audio files
 */
public List<BookAudio> getAudioFiles() {
    return Collections.unmodifiableList(audioFiles);
}
    
/**
 * Checks if the audiobook has any audio files.
 * 
 * @return true if the audiobook contains no audio files, false otherwise
 */
public boolean isEmpty() {
    return audioFiles.isEmpty();
}
    
/**
 * Removes all audio files from the audiobook.
 * This clears the audiobook content but preserves metadata.
 */
public void clearAudioFiles() {
    audioFiles.clear();
}
}