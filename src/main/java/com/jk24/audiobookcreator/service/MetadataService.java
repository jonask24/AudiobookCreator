package com.jk24.audiobookcreator.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import com.jk24.audiobookcreator.model.Audiobook;
import com.jk24.audiobookcreator.model.BookAudio;

/**
 * Service for handling audiobook metadata suggestions and persistence.
 * This class provides intelligent metadata suggestions based on previously
 * processed audiobooks and folder context.
 * <p>
 * The service maintains a history of audiobooks by folder, allowing it to
 * make context-aware suggestions for new audiobooks in the same series or folder.
 */
public class MetadataService {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(MetadataService.class.getName());
    
    // Store metadata for books by folder path
    private static final Map<String, java.util.List<Audiobook>> folderHistory = new HashMap<>();
    
    private final com.jk24.audiobookcreator.service.PreferencesService prefsService;
    
    /**
     * Creates a new metadata service connected to the specified preferences service.
     * The preferences service is used to store and retrieve persistent metadata.
     * 
     * @param prefsService the preferences service to use
     */
    public MetadataService(com.jk24.audiobookcreator.service.PreferencesService prefsService) {
        this.prefsService = prefsService;
    }
    
    /**
     * Suggests metadata for a new audiobook based on folder context and files.
     * The method uses a combination of strategies to make intelligent suggestions:
     * 1. First uses preferences for base values
     * 2. Then checks folder history for additional suggestions
     * 3. If this is the first book in a folder, uses folder name as series
     * 4. If title is empty, tries to use first filename
     * 
     * @param bookFolder the folder containing the audiobook files
     * @param files the audio files to include in the audiobook
     * @return a pre-populated audiobook with suggested metadata
     */
    public Audiobook suggestMetadata(File bookFolder, java.util.List<BookAudio> files) {
        Audiobook audiobook = new Audiobook();
        
        // Add the files to the audiobook
        files.forEach(audiobook::addAudioFile);
        
        // First try to use preferences for base values
        audiobook.setTitle(prefsService.getPreviousBookTitle());
        audiobook.setAuthor(prefsService.getPreviousBookAuthor());
        audiobook.setSeries(prefsService.getPreviousBookSeries());
        audiobook.setBookNumber(prefsService.getPreviousBookNumber() + 1);
        
        // Check folder history for additional suggestions
        if (bookFolder != null) {
            String folderPath = bookFolder.getAbsolutePath();
            
            if (folderHistory.containsKey(folderPath) && !folderHistory.get(folderPath).isEmpty()) {
                // Use folder history for suggestions
                java.util.List<Audiobook> previousBooks = folderHistory.get(folderPath);
                Audiobook lastBook = previousBooks.get(previousBooks.size() - 1);
                
                // If title is empty, try to use first filename
                if ((audiobook.getTitle() == null || audiobook.getTitle().isEmpty()) && !files.isEmpty()) {
                    audiobook.setTitle(files.get(0).getTitle());
                    LOGGER.info("Using first filename as title: " + audiobook.getTitle());
                }
                
                // Use last book's author and series if needed
                if (audiobook.getAuthor() == null || audiobook.getAuthor().isEmpty()) {
                    audiobook.setAuthor(lastBook.getAuthor());
                    LOGGER.info("Using previous author from folder history: " + audiobook.getAuthor());
                }
                
                if (audiobook.getSeries() == null || audiobook.getSeries().isEmpty()) {
                    audiobook.setSeries(lastBook.getSeries());
                    LOGGER.info("Using previous series from folder history: " + audiobook.getSeries());
                }
                
                // Always increment book number from the last one in this folder
                audiobook.setBookNumber(lastBook.getBookNumber() + 1);
                LOGGER.info("Using incremented book number: " + audiobook.getBookNumber());
            } else {
                // First book in this folder - use folder name as series if series is empty
                if (audiobook.getSeries() == null || audiobook.getSeries().isEmpty()) {
                    audiobook.setSeries(bookFolder.getName());
                    LOGGER.info("Using folder name as series: " + audiobook.getSeries());
                }
                
                // If title is empty, try to use first filename
                if ((audiobook.getTitle() == null || audiobook.getTitle().isEmpty()) && !files.isEmpty()) {
                    audiobook.setTitle(files.get(0).getTitle());
                    LOGGER.info("Using first filename as title: " + audiobook.getTitle());
                }
            }
        }
        
        return audiobook;
    }
    
    /**
     * Saves audiobook metadata to folder history and preferences.
     * This builds up the history database for future suggestions.
     * 
     * @param bookFolder the folder containing the audiobook
     * @param audiobook the audiobook to save metadata from
     */
    public void saveToFolderHistory(File bookFolder, Audiobook audiobook) {
        if (bookFolder == null) return;
        
        String folderPath = bookFolder.getAbsolutePath();
        
        if (!folderHistory.containsKey(folderPath)) {
            folderHistory.put(folderPath, new java.util.ArrayList<>());
        }
        
        folderHistory.get(folderPath).add(audiobook);
        LOGGER.info("Saved metadata for folder: " + folderPath);
        
        // Also save to preferences
        prefsService.saveBookMetadata(
                audiobook.getTitle(), 
                audiobook.getAuthor(), 
                audiobook.getSeries(), 
                audiobook.getBookNumber());
    }
}
