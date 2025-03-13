package com.jk24.audiobookcreator;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * Service for managing application preferences and persistent settings.
 * This class provides access to user preferences including:
 * - Visual preferences (theme)
 * - File locations (book folder, output folder)
 * - Processing options (threading)
 * - Previous audiobook metadata
 * <p>
 * Preferences are automatically persisted between application sessions
 * using the Java Preferences API.
 */
public class PreferencesService {
    // Preference keys
    private static final String BOOK_FOLDER_KEY = "bookFolderPath";
    private static final String OUTPUT_FOLDER_KEY = "outputFolderPath";
    private static final String THEME_KEY = "appTheme";
    private static final String PREVIOUS_BOOK_TITLE_KEY = "prevBookTitle";
    private static final String PREVIOUS_BOOK_AUTHOR_KEY = "prevBookAuthor";
    private static final String PREVIOUS_BOOK_SERIES_KEY = "prevBookSeries";
    private static final String PREVIOUS_BOOK_NUMBER_KEY = "prevBookNumber";
    private static final String MULTITHREADING_ENABLED_KEY = "multithreadingEnabled";
    private static final String THREAD_COUNT_KEY = "threadCount";
    
    // The underlying preferences store
    private final Preferences prefs;
    
    // Current values
    private File bookFolder;
    private File outputFolder;
    private String theme;
    private String previousBookTitle;
    private String previousBookAuthor;
    private String previousBookSeries;
    private int previousBookNumber;
    private boolean multithreadingEnabled;
    private int threadCount;
    
    /**
     * Creates a new preferences service and loads saved preferences.
     * Default values are provided for preferences that haven't been set.
     */
    public PreferencesService() {
        prefs = Preferences.userNodeForPackage(PreferencesService.class);
        loadPreferences();
    }
    
    /**
     * Loads all preferences from the persistent store.
     * This method validates file paths and sets reasonable defaults
     * for missing preferences.
     */
    private void loadPreferences() {
        // Load book folder
        String savedFolderPath = prefs.get(BOOK_FOLDER_KEY, null);
        if (savedFolderPath != null) {
            bookFolder = new File(savedFolderPath);
            if (!bookFolder.exists() || !bookFolder.isDirectory()) {
                bookFolder = null;
            }
        }
        
        // Load output folder
        String savedOutputPath = prefs.get(OUTPUT_FOLDER_KEY, null);
        if (savedOutputPath != null) {
            outputFolder = new File(savedOutputPath);
            if (!outputFolder.exists() || !outputFolder.isDirectory()) {
                outputFolder = null;
            }
        }
        
        // Load theme
        theme = prefs.get(THEME_KEY, "light");
        
        // Load previous book info
        previousBookTitle = prefs.get(PREVIOUS_BOOK_TITLE_KEY, "");
        previousBookAuthor = prefs.get(PREVIOUS_BOOK_AUTHOR_KEY, "");
        previousBookSeries = prefs.get(PREVIOUS_BOOK_SERIES_KEY, "");
        previousBookNumber = prefs.getInt(PREVIOUS_BOOK_NUMBER_KEY, 1);
       
        // Load multithreading preferences
        multithreadingEnabled = prefs.getBoolean(MULTITHREADING_ENABLED_KEY, true);
        threadCount = prefs.getInt(THREAD_COUNT_KEY, Runtime.getRuntime().availableProcessors());
    }
    
    /**
     * Gets the current book folder.
     * 
     * @return the book folder, or null if not set or invalid
     */
    public File getBookFolder() {
        return bookFolder;
    }
    
    /**
     * Sets and persists the book folder.
     * 
     * @param bookFolder the book folder to use
     */
    public void setBookFolder(File bookFolder) {
        this.bookFolder = bookFolder;
        if (bookFolder != null) {
            prefs.put(BOOK_FOLDER_KEY, bookFolder.getAbsolutePath());
        } else {
            prefs.remove(BOOK_FOLDER_KEY);
        }
    }
    
    /**
     * Gets the current output folder.
     * 
     * @return the output folder, or null if not set or invalid
     */
    public File getOutputFolder() {
        return outputFolder;
    }
    
    /**
     * Sets and persists the output folder.
     * 
     * @param outputFolder the output folder to use
     */
    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
        if (outputFolder != null) {
            prefs.put(OUTPUT_FOLDER_KEY, outputFolder.getAbsolutePath());
        } else {
            prefs.remove(OUTPUT_FOLDER_KEY);
        }
    }
    
    /**
     * Gets the current theme name.
     * 
     * @return the theme name ("light" or "dark")
     */
    public String getTheme() {
        return theme;
    }
    
    /**
     * Sets and persists the theme.
     * 
     * @param theme the theme name to use
     */
    public void setTheme(String theme) {
        this.theme = theme;
        prefs.put(THEME_KEY, theme);
    }
    
    /**
     * Convenience method to check if dark theme is active.
     * 
     * @return true if dark theme is selected, false otherwise
     */
    public boolean isDarkTheme() {
        return "dark".equals(theme);
    }
    
    /**
     * Gets the previous book title from preferences.
     * 
     * @return the previous book title
     */
    public String getPreviousBookTitle() {
        return previousBookTitle;
    }
    
    /**
     * Sets and persists the previous book title.
     * 
     * @param title the title to save
     */
    public void setPreviousBookTitle(String title) {
        this.previousBookTitle = title;
        prefs.put(PREVIOUS_BOOK_TITLE_KEY, title);
    }
    
    /**
     * Gets the previous book author from preferences.
     * 
     * @return the previous book author
     */
    public String getPreviousBookAuthor() {
        return previousBookAuthor;
    }
    
    /**
     * Sets and persists the previous book author.
     * 
     * @param author the author to save
     */
    public void setPreviousBookAuthor(String author) {
        this.previousBookAuthor = author;
        prefs.put(PREVIOUS_BOOK_AUTHOR_KEY, author);
    }
    
    /**
     * Gets the previous book series from preferences.
     * 
     * @return the previous book series
     */
    public String getPreviousBookSeries() {
        return previousBookSeries;
    }
    
    /**
     * Sets and persists the previous book series.
     * 
     * @param series the series to save
     */
    public void setPreviousBookSeries(String series) {
        this.previousBookSeries = series;
        prefs.put(PREVIOUS_BOOK_SERIES_KEY, series);
    }
    
    /**
     * Gets the previous book number from preferences.
     * 
     * @return the previous book number
     */
    public int getPreviousBookNumber() {
        return previousBookNumber;
    }
    
    /**
     * Sets and persists the previous book number.
     * 
     * @param number the book number to save
     */
    public void setPreviousBookNumber(int number) {
        this.previousBookNumber = number;
        prefs.putInt(PREVIOUS_BOOK_NUMBER_KEY, number);
    }
    
    /**
     * Convenience method to save all book metadata at once.
     * 
     * @param title the book title
     * @param author the book author
     * @param series the book series
     * @param number the book number
     */
    public void saveBookMetadata(String title, String author, String series, int number) {
        setPreviousBookTitle(title);
        setPreviousBookAuthor(author);
        setPreviousBookSeries(series);
        setPreviousBookNumber(number);
    }
    
    /**
     * Checks if multithreading is enabled.
     * 
     * @return true if multithreading is enabled, false otherwise
     */
    public boolean isMultithreadingEnabled() {
        return multithreadingEnabled;
    }
    
    /**
     * Sets and persists the multithreading enabled setting.
     * 
     * @param enabled true to enable multithreading, false to disable
     */
    public void setMultithreadingEnabled(boolean enabled) {
        this.multithreadingEnabled = enabled;
        prefs.putBoolean(MULTITHREADING_ENABLED_KEY, enabled);
    }
    
    /**
     * Gets the thread count for multithreaded processing.
     * 
     * @return the number of threads to use
     */
    public int getThreadCount() {
        return threadCount;
    }
    
    /**
     * Sets and persists the thread count.
     * 
     * @param count the number of threads to use
     */
    public void setThreadCount(int count) {
        this.threadCount = count;
        prefs.putInt(THREAD_COUNT_KEY, count);
    }
}