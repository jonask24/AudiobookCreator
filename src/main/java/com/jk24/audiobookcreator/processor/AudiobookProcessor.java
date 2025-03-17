package com.jk24.audiobookcreator.processor;

import com.jk24.audiobookcreator.model.Audiobook;
import com.jk24.audiobookcreator.model.BookAudio;
import com.jk24.audiobookcreator.service.PreferencesService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.progress.EncoderProgressListener;
import ws.schild.jave.info.MultimediaInfo;

/**
 * Handles the audiobook processing pipeline including audio file merging and 
 * metadata application. This class handles the transformation of multiple audio 
 * files into a single M4B audiobook file.
 * 
 * Key features:
 * - Parallel file processing with configurable thread count
 * - Progress monitoring through callback interface
 * - Metadata and cover image integration
 * - Smart handling of existing M4B files
 */
public class AudiobookProcessor {
    // Thread management for parallel processing
    private ExecutorService executorService;
    private boolean multithreadingEnabled = true;
    private int numThreads = Runtime.getRuntime().availableProcessors();
    private int audioBitRate = 128000;  // Default: Best quality
    private int audioSamplingRate = 44100;  // Default: Best quality
    
    // Feature toggles
    private boolean metadataEnabled = true;
    
    /**
     * Creates a new processor instance with default settings.
     * By default, multithreading is enabled with thread count equal to
     * available processors.
     */
    public AudiobookProcessor() {
        initializeExecutor();
    }
    
    /**
     * Creates a new processor instance with settings from PreferencesService.
     * This constructor configures multithreading and audio quality settings
     * based on the provided preferences.
     * 
     * @param preferencesService the preferences service to use for configuration
     */
    public AudiobookProcessor(PreferencesService preferencesService) {
        if (preferencesService != null) {
            // Configure multithreading
            this.multithreadingEnabled = preferencesService.isMultithreadingEnabled();
            this.numThreads = preferencesService.getThreadCount();
            
            // Configure audio quality settings
            this.audioBitRate = preferencesService.getAudioBitRate();
            this.audioSamplingRate = preferencesService.getAudioSamplingRate();
            
            System.out.println("AudiobookProcessor initialized with preferences:");
            System.out.println("- Multithreading: " + this.multithreadingEnabled);
            System.out.println("- Thread count: " + this.numThreads);
            System.out.println("- Audio bit rate: " + this.audioBitRate + " bps");
            System.out.println("- Audio sampling rate: " + this.audioSamplingRate + " Hz");
        }
        
        initializeExecutor();
    }
    
    /**
     * Initializes or reinitializes the executor service based on current settings.
     * This ensures the execution thread pool reflects the current multithreading
     * preferences.
     */
    private void initializeExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        if (multithreadingEnabled) {
            executorService = Executors.newFixedThreadPool(numThreads);
        } else {
            executorService = Executors.newSingleThreadExecutor();
        }
    }
    
    /**
     * Enables or disables multithreaded audio processing.
     * 
     * @param enabled true to use multiple threads, false to use only one thread
     */
    public void setMultithreadingEnabled(boolean enabled) {
        if (this.multithreadingEnabled != enabled) {
            this.multithreadingEnabled = enabled;
            initializeExecutor();
        }
    }
    
    /**
     * Sets the number of threads to use when multithreading is enabled.
     * If the new value is different and greater than zero, the executor
     * service will be reinitialized.
     * 
     * @param numThreads the number of threads to use for parallel processing
     */
    public void setNumThreads(int numThreads) {
        if (this.numThreads != numThreads && numThreads > 0) {
            this.numThreads = numThreads;
            initializeExecutor();
        }
    }
    
    /**
     * Sets the callback function that will receive progress updates.
     * The callback receives values ranging from 0.0 (not started) to 
     * 1.0 (complete).
     * 
     * @param progressCallback function that accepts progress values
     * @deprecated This method is deprecated. Pass callbacks directly to processAudiobook instead.
     */
    @Deprecated
    public void setProgressCallback(Consumer<Double> progressCallback) {
        // Deprecated - do nothing
    }
    
    /**
     * Enables or disables metadata processing when creating audiobooks.
     * When enabled, title, author, series, and cover art are included.
     * 
     * @param enabled true to enable metadata, false to disable it
     */
    public void setMetadataEnabled(boolean enabled) {
        this.metadataEnabled = enabled;
    }
    
    /**
     * Sets the audio bit rate to use for encoding.
     * 
     * @param bitRate the bit rate in bits per second (e.g., 64000, 128000)
     */
    public void setAudioBitRate(int bitRate) {
        System.out.println("----------------------------------------------------");
        System.out.println("Changing audio bit rate from " + this.audioBitRate + " to " + bitRate + " bps");
        System.out.println("----------------------------------------------------");
        this.audioBitRate = bitRate;
       
       // Force a log entry with the current settings after change
       System.out.println("Current Audio Settings After Change:");
       System.out.println("Bit Rate: " + this.audioBitRate + " bps");
       System.out.println("Sampling Rate: " + this.audioSamplingRate + " Hz");
    }
    
    /**
     * Gets the current audio bit rate setting.
     * 
     * @return the bit rate in bits per second
     */
    public int getAudioBitRate() {
        return audioBitRate;
    }
    
    /**
     * Sets the audio sampling rate to use for encoding.
     * 
     * @param samplingRate the sampling rate in Hz (e.g., 22050, 44100)
     */
    public void setAudioSamplingRate(int samplingRate) {
        System.out.println("----------------------------------------------------");
        System.out.println("Changing audio sampling rate from " + this.audioSamplingRate + " to " + samplingRate + " Hz");
        System.out.println("----------------------------------------------------");
        this.audioSamplingRate = samplingRate;
       
       // Force a log entry with the current settings after change
       System.out.println("Current Audio Settings After Change:");
       System.out.println("Bit Rate: " + this.audioBitRate + " bps"); 
       System.out.println("Sampling Rate: " + this.audioSamplingRate + " Hz");
    }
    
    /**
     * Gets the current audio sampling rate setting.
     * 
     * @return the sampling rate in Hz
     */
    public int getAudioSamplingRate() {
        return audioSamplingRate;
    }
    
    /**
     * Processes an audiobook asynchronously with the specified output file and cover image.
     * This method merges all audio files in the audiobook, converts them to M4B format,
     * and applies metadata including the cover image if provided.
     * 
     * @param audiobook the audiobook containing audio files and metadata
     * @param outputFile the destination file for the processed audiobook
     * @param coverImage optional cover image (can be null)
     * @param progressCallback callback function to receive progress updates
     * @param preferencesService optional preferences service (can be null)
     * @return a CompletableFuture that completes with the output file when processing is done
     */
    public CompletableFuture<File> processAudiobook(
            Audiobook audiobook, 
            File outputFile, 
            File coverImage,
            Consumer<Double> progressCallback,
            PreferencesService preferencesService) {
        
        // Get audio quality settings directly from the audiobook
        final int audioBitRateToUse = audiobook.getAudioBitRate();
        final int audioSamplingRateToUse = audiobook.getAudioSamplingRate();
        
        // Store the settings to use for this specific processing job
        System.out.println("Using audiobook-specific audio settings for processing:");
        System.out.println("  - Quality preset: " + audiobook.getAudioQuality());
        System.out.println("  - Bit Rate: " + audioBitRateToUse + " bps");
        System.out.println("  - Sampling Rate: " + audioSamplingRateToUse + " Hz");
        
        // Initialize progress notification
        if (progressCallback != null) {
            progressCallback.accept(0.0); // Start at 0%
        }
        
        // Create a final reference to the callback for use in the lambda
        final Consumer<Double> finalProgressCallback = progressCallback;
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Extract file objects from the audiobook
                List<File> files = audiobook.getAudioFiles().stream()
                        .map(BookAudio::getFile)
                        .toList();
                
                // Report actual start of processing
                if (finalProgressCallback != null) {
                    finalProgressCallback.accept(0.01); // Started processing
                }
                
                // Process the files using the audiobook's quality settings directly
                // No need to modify shared class variables anymore
                mergeFiles(files, outputFile, audiobook, coverImage, finalProgressCallback, 
                         audioBitRateToUse, audioSamplingRateToUse);
                
                // Ensure we send a final 100% progress update
                if (finalProgressCallback != null) {
                    finalProgressCallback.accept(1.0);  // Completed
                    try {
                        Thread.sleep(100);
                        finalProgressCallback.accept(1.0);  // Send second completion signal
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                return outputFile;
            } catch (Exception e) {
                throw new RuntimeException("Failed to process audiobook: " + e.getMessage(), e);
            }
        }, executorService);
    }
    
    /**
     * Processes an audiobook asynchronously with the specified output file.
     * This is a convenience method that calls the main method with null for coverImage.
     * 
     * @param audiobook the audiobook containing audio files and metadata
     * @param outputFile the destination file for the processed audiobook
     * @param progressCallback callback function to receive progress updates
     * @return a CompletableFuture that completes with the output file when processing is done
     */
    public CompletableFuture<File> processAudiobook(
            Audiobook audiobook, 
            File outputFile, 
            Consumer<Double> progressCallback) {
        // Call the full method with null for coverImage
        return processAudiobook(audiobook, outputFile, null, progressCallback, null);
    }
    
    /**
     * Overload method for backward compatibility
     */
    public CompletableFuture<File> processAudiobook(
            Audiobook audiobook, 
            File outputFile, 
            File coverImage,
            Consumer<Double> progressCallback) {
        
        return processAudiobook(audiobook, outputFile, coverImage, progressCallback, null);
    }
    
    /**
     * Processes multiple audiobooks in parallel, creating an M4B file for each.
     * Each audiobook will be processed with its own quality settings.
     * 
     * @param audiobooks list of audiobooks to process
     * @param outputDirectory destination directory for output files
     * @param progressCallback callback for progress updates
     * @return list of CompletableFutures for each processing operation
     */
    public List<CompletableFuture<File>> processAudiobooks(
            List<Audiobook> audiobooks,
            File outputDirectory,
            Consumer<Double> progressCallback) {
        
        return audiobooks.stream()
                .map(audiobook -> {
                    String filename = sanitizeFilename(audiobook.getTitle()) + ".m4b";
                    File outputFile = new File(outputDirectory, filename);
                    // Each audiobook is processed with its own quality settings
                    return processAudiobook(audiobook, outputFile, progressCallback);
                })
                .toList();
    }
    
    /**
     * Sanitizes a string for use as a filename by replacing invalid characters.
     * This ensures the resulting filename is valid across different operating systems.
     * 
     * @param input The input string to sanitize
     * @return A sanitized filename string
     */
    public String sanitizeFilename(String input) {
        if (input == null || input.isEmpty()) {
            return "audiobook";
        }
        
        // Remove characters that are typically not allowed in filenames
        // Replace all characters illegal in filenames across operating systems with hyphens
        // This ensures consistent filename sanitization for Windows, macOS, and Linux
        return input.replace('/', '-')
                .replace('\\', '-')
                .replace(':', '-')
                .replace('*', '-')
                .replace('?', '-')
                .replace('"', '-')
                .replace('<', '-')
                .replace('>', '-')
                .replace('|', '-');
    }
    
    /**
     * Shuts down the executor service, releasing thread resources.
     * Call this method when the processor is no longer needed to prevent
     * resource leaks.
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    // === Core processing methods ===
    
    /**
     * Core method to merge audio files into an M4B file with metadata.
     * This method handles the complete process of converting and merging
     * audio files, with special handling for the case of a single M4B input.
     * 
     * @param inputFiles list of audio files to merge
     * @param outputFile destination M4B file
     * @param audiobook the audiobook containing metadata
     * @param coverImage optional cover image file
     * @param progressCallback callback for progress updates
     * @param bitRate explicit bit rate to use (e.g. 64000 or 128000)
     * @param samplingRate explicit sampling rate to use (e.g. 22050 or 44100)
     * @throws IOException if an I/O error occurs
     * @throws EncoderException if there's an error during encoding
     */
    public void mergeFiles(List<File> inputFiles, File outputFile, Audiobook audiobook, File coverImage, Consumer<Double> progressCallback, int bitRate, int samplingRate) throws IOException, EncoderException {
        if (inputFiles.isEmpty()) {
            throw new IllegalArgumentException("No input files provided");
        }
        
        // Output the audio settings that will be used for this merge operation
        System.out.println("----------------------------------------------------");
        System.out.println("mergeFiles using the following audio settings:");
        System.out.println("- Bit Rate: " + bitRate + " bps");
        System.out.println("- Sampling Rate: " + samplingRate + " Hz");
        System.out.println("----------------------------------------------------");

        // Create a temporary directory to store intermediate files
        File tempDir = Files.createTempDirectory("audio_merger").toFile();
        tempDir.deleteOnExit();
        
        try {
            // Step 1: Convert all files to the same format (with multithreading if enabled)
            List<File> convertedFiles;
            if (multithreadingEnabled && numThreads > 1) {
                convertedFiles = convertFilesParallel(inputFiles, tempDir, progressCallback, bitRate, samplingRate);
            } else {
                convertedFiles = convertFiles(inputFiles, tempDir, progressCallback, bitRate, samplingRate);
            }
            
            // Step 2: Concatenate all files
            File mergedRawFile = new File(tempDir, "merged.tmp");
            concatenateFiles(convertedFiles, mergedRawFile, progressCallback);
            
            // Step 3: Convert to M4B (possibly with temporary name if metadata is enabled)
            File finalOutputFile;
            if (metadataEnabled) {
                File tempOutputFile = new File(tempDir, "pre_metadata.m4b");
                System.out.println("Converting to M4B with settings: " + bitRate + "bps, " + samplingRate + "Hz");
                // Always use explicit bit rate and sampling rate
                convertToM4B(mergedRawFile, tempOutputFile, progressCallback, bitRate, samplingRate);
                finalOutputFile = applyMetadata(tempOutputFile, outputFile, audiobook, coverImage, progressCallback);
            } else {
                System.out.println("Converting to M4B with settings: " + bitRate + "bps, " + samplingRate + "Hz");
                // Always use explicit bit rate and sampling rate
                convertToM4B(mergedRawFile, outputFile, progressCallback, bitRate, samplingRate);
                finalOutputFile = outputFile;
            }
            
            // Ensure file exists with expected content
            if (!finalOutputFile.exists() || finalOutputFile.length() == 0) {
                throw new IOException("Failed to create output file or file is empty");
            }
            
        } finally {
            // Clean up temp files
            for (File file : tempDir.listFiles()) {
                file.delete();
            }
            tempDir.delete();
        }
    }
    
    /**
     * Handles a single M4B file by updating its metadata and copying it to the output location.
     * 
     * @deprecated This method is no longer used - all files including M4B files now go through 
     * the full conversion process. This avoids inconsistencies and ensures all outputs have the 
     * same encoding parameters.
     * 
     * @param inputFile The source M4B file
     * @param outputFile The destination file
     * @param audiobook The audiobook metadata
     * @param coverImage Optional cover image
     * @param progressCallback Callback for progress updates
     * @throws IOException if an I/O error occurs
     */
    @Deprecated
    private void handleSingleM4bFile(File inputFile, File outputFile, Audiobook audiobook, File coverImage, Consumer<Double> progressCallback) throws IOException {
        try {
            // Make a full sequence of progress updates to simulate regular processing stages
            // This ensures UI progress bars update smoothly even with this optimized path
            if (progressCallback != null) {
                progressCallback.accept(0.01); // Initial
                Thread.sleep(50); // Small delay to ensure UI updates
                progressCallback.accept(0.1); // Started
            }
            
            // Log that we're optimizing by just updating metadata
            System.out.println("Input is already an M4B file. Optimizing by just updating metadata.");
            System.out.println("Source: " + inputFile.getAbsolutePath());
            System.out.println("Destination: " + outputFile.getAbsolutePath());
            
            // Copy the file first if the source and destination are different
            if (!inputFile.getAbsolutePath().equals(outputFile.getAbsolutePath())) {
                // Simulate progress during file copy
                if (progressCallback != null) {
                    // Generate intermediate progress updates during file copy
                    for (double progress = 0.1; progress <= 0.4; progress += 0.1) {
                        progressCallback.accept(progress);
                        Thread.sleep(50); // Small delay to ensure UI updates
                    }
                }
                
                Files.copy(inputFile.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Copied M4B file to destination");
            }
            
            if (progressCallback != null) {
                progressCallback.accept(0.5); // File copied
                Thread.sleep(50);
            }
            
            // Apply metadata if enabled
            if (metadataEnabled) {
                // Simulate more progress during metadata operations
                if (progressCallback != null) {
                    progressCallback.accept(0.6);
                    Thread.sleep(50);
                }
                
                System.out.println("Updating metadata for M4B file:");
                System.out.println("- Title: \"" + audiobook.getTitle() + "\"");
                System.out.println("- Artist: \"" + audiobook.getAuthor() + "\"");
                System.out.println("- Album: \"" + audiobook.getSeries() + "\"");
                System.out.println("- Track: " + audiobook.getBookNumber());
                
                if (coverImage != null && coverImage.exists()) {
                    System.out.println("- Cover art: \"" + coverImage.getName() + "\"");
                }
                
                if (progressCallback != null) {
                    progressCallback.accept(0.7);
                    Thread.sleep(50);
                }
                
                // Use the fully qualified name for AudioFile from JAudioTagger
                org.jaudiotagger.audio.AudioFile audioFile = AudioFileIO.read(outputFile);
                Tag tag = audioFile.getTag();
                if (tag == null) {
                    tag = audioFile.createDefaultTag();
                    audioFile.setTag(tag);
                }
                
                // Don't apply empty metadata fields
                if (audiobook.getTitle() != null && !audiobook.getTitle().isEmpty()) {
                    tag.setField(FieldKey.TITLE, audiobook.getTitle());
                }
                
                if (audiobook.getAuthor() != null && !audiobook.getAuthor().isEmpty()) {
                    tag.setField(FieldKey.ARTIST, audiobook.getAuthor());
                }
                
                if (audiobook.getSeries() != null && !audiobook.getSeries().isEmpty()) {
                    tag.setField(FieldKey.ALBUM, audiobook.getSeries());
                }
                
                if (audiobook.getBookNumber() > 0) {
                    tag.setField(FieldKey.TRACK, String.valueOf(audiobook.getBookNumber()));
                }
                
                if (progressCallback != null) {
                    progressCallback.accept(0.8);
                    Thread.sleep(50);
                }
                
                // Handle cover art - special logic to preserve existing art if none provided
                if (coverImage != null && coverImage.exists()) {
                    try {
                        org.jaudiotagger.tag.images.Artwork artwork = 
                            org.jaudiotagger.tag.images.ArtworkFactory.createArtworkFromFile(coverImage);
                        tag.setField(artwork);
                        System.out.println("Updated cover art in the M4B file");
                    } catch (Exception e) {
                        System.err.println("Failed to update cover art: " + e.getMessage());
                    }
                } else {
                    System.out.println("No new cover art provided - preserving existing artwork if any");
                }
                
                if (progressCallback != null) {
                    progressCallback.accept(0.9);
                    Thread.sleep(50);
                }
                
                audioFile.commit(); // Save changes to the file
                System.out.println("Metadata update completed");
            } else {
                // Even if we don't apply metadata, still simulate progress
                if (progressCallback != null) {
                    for (double progress = 0.6; progress <= 0.9; progress += 0.1) {
                        progressCallback.accept(progress);
                        Thread.sleep(50);
                    }
                    
                    System.out.println("Metadata updates disabled - preserving original metadata");
                }
            }
            
            // Add one more delay before final progress to ensure UI catches up
            Thread.sleep(100);
            
            // Always send the final progress report
            if (progressCallback != null) {
                progressCallback.accept(1.0);  // Completed
                Thread.sleep(100);
                progressCallback.accept(1.0);  // Send a second completion signal to be sure
            }
            
        } catch (InterruptedException e) {
            // Handle thread interruption
            Thread.currentThread().interrupt();
            throw new IOException("M4B processing was interrupted");
        } catch (Exception e) {
            System.err.println("Error handling single M4B file: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to process single M4B file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Apply metadata to the file including title, author, series, and cover image.
     * 
     * @param inputFile the input file to get metadata
     * @param outputFile the output file to save with metadata
     * @param audiobook the audiobook containing metadata
     * @param coverImage optional cover image file
     * @param progressCallback callback for progress updates
     * @return the output file with metadata applied
     */
    private File applyMetadata(File inputFile, File outputFile, Audiobook audiobook, File coverImage, Consumer<Double> progressCallback) {
        try {
            // Apply actual metadata to the M4B file
            System.out.println("Adding metadata to M4B file:");
            System.out.println("- Title: \"" + audiobook.getTitle() + "\"");
            System.out.println("- Artist: \"" + audiobook.getAuthor() + "\"");
            System.out.println("- Album: \"" + audiobook.getSeries() + "\"");
            System.out.println("- Track: " + audiobook.getBookNumber());
            
            if (coverImage != null && coverImage.exists()) {
                System.out.println("- Cover art: \"" + coverImage.getName() + "\"");
            }
          
            // Use the fully qualified name for AudioFile from JAudioTagger
            org.jaudiotagger.audio.AudioFile audioFile = AudioFileIO.read(inputFile);
            Tag tag = audioFile.getTag();
            if (tag == null) {
                tag = audioFile.createDefaultTag();
                audioFile.setTag(tag);
            }
            
            // Don't apply empty metadata fields
            if (audiobook.getTitle() != null && !audiobook.getTitle().isEmpty()) {
                tag.setField(FieldKey.TITLE, audiobook.getTitle());
            }
            
            if (audiobook.getAuthor() != null && !audiobook.getAuthor().isEmpty()) {
                tag.setField(FieldKey.ARTIST, audiobook.getAuthor());
            }
            
            if (audiobook.getSeries() != null && !audiobook.getSeries().isEmpty()) {
                tag.setField(FieldKey.ALBUM, audiobook.getSeries());
            }
            
            if (audiobook.getBookNumber() > 0) {
                tag.setField(FieldKey.TRACK, String.valueOf(audiobook.getBookNumber()));
            }
            
            // Add cover art if available
            if (coverImage != null && coverImage.exists()) {
                try {
                    org.jaudiotagger.tag.images.Artwork artwork = 
                        org.jaudiotagger.tag.images.ArtworkFactory.createArtworkFromFile(coverImage);
                    tag.setField(artwork);
                    System.out.println("Added cover art to the M4B file");
                } catch (Exception e) {
                    System.err.println("Failed to add cover art: " + e.getMessage());
                }
            }
          
            audioFile.commit(); // Save changes to the file
           
            // Copy to final destination
            Files.copy(inputFile.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
           
            // Make sure we signal completion
            if (progressCallback != null) {
                progressCallback.accept(1.0); // 100% progress
                
                // Add an additional completion notification after a delay
                try {
                    Thread.sleep(100);
                    progressCallback.accept(1.0); // Send second completion signal
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
           
            return outputFile;
        } catch (Exception e) {
            System.err.println("Warning: Failed to apply metadata: " + e.getMessage());
            // Fallback if metadata application fails
            try {
                if (!outputFile.exists() || outputFile.length() == 0) {
                    Files.copy(inputFile.toPath(), outputFile.toPath());
                }
            } catch (IOException copyError) {
                System.err.println("Error copying file after metadata failure: " + copyError.getMessage());
            }
            return outputFile;
        }
    }
    
    /**
     * Sequential conversion of files to a common format.
     * This method processes one file at a time and is used when multithreading is disabled.
     * 
     * @param inputFiles list of files to convert
     * @param tempDir temporary directory for converted files
     * @param progressCallback callback for progress updates
     * @param bitRate explicit bit rate to use (e.g. 64000 or 128000)
     * @param samplingRate explicit sampling rate to use (e.g. 22050 or 44100)
     * @return list of converted files
     * @throws EncoderException if encoding fails
     */
    private List<File> convertFiles(List<File> inputFiles, File tempDir, Consumer<Double> progressCallback, final int bitRate, final int samplingRate) throws EncoderException {
        List<File> convertedFiles = new ArrayList<>();
        int totalFiles = inputFiles.size();
        
        for (int i = 0; i < inputFiles.size(); i++) {
            File inputFile = inputFiles.get(i);
            File outputFile = new File(tempDir, i + "_converted.mp3");
            final int fileIndex = i;
            
           // Debug log to verify settings are being applied
           System.out.println("----------------------------------------------------");
           System.out.println("Converting file " + (i+1) + "/" + inputFiles.size() + " to MP3 with quality settings:");
           System.out.println("Bit Rate: " + bitRate + " bps");
           System.out.println("Sampling Rate: " + samplingRate + " Hz");
           System.out.println("----------------------------------------------------");
           
            AudioAttributes audioAttributes = new AudioAttributes();
            audioAttributes.setCodec("libmp3lame");
            audioAttributes.setBitRate(bitRate);
            audioAttributes.setChannels(2);
            audioAttributes.setSamplingRate(samplingRate);
            
            EncodingAttributes encodingAttributes = new EncodingAttributes();
            encodingAttributes.setOutputFormat("mp3");
            encodingAttributes.setAudioAttributes(audioAttributes);
            
            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(inputFile), outputFile, encodingAttributes, 
                new EncoderProgressListener() {
                    @Override
                    public void sourceInfo(MultimediaInfo info) {}
                   
                   @Override
                   public void progress(int permil) {
                        if (progressCallback != null) {
                            // Calculate overall progress: current file progress + completed files
                            double fileProgress = permil / 1000.0;
                            double overallProgress = (fileIndex + fileProgress) / totalFiles * 0.7; // 70% of total progress
                            progressCallback.accept(overallProgress);
                        }
                    }
                    
                    @Override
                    public void message(String s) {}
                });
            
            convertedFiles.add(outputFile);
        }
        
        return convertedFiles;
    }
    
    /**
     * Parallel conversion of files using multiple threads.
     * This method distributes file conversion tasks across multiple threads for faster processing.
     * 
     * @param inputFiles list of files to convert
     * @param tempDir temporary directory for converted files
     * @param progressCallback callback for progress updates
     * @param bitRate explicit bit rate to use (e.g. 64000 or 128000)
     * @param samplingRate explicit sampling rate to use (e.g. 22050 or 44100)
     * @return list of converted files in the original order
     * @throws EncoderException if encoding fails
     */
    private List<File> convertFilesParallel(List<File> inputFiles, File tempDir, Consumer<Double> progressCallback, final int bitRate, final int samplingRate) throws EncoderException {
        int totalFiles = inputFiles.size();
        
        // Pre-create the output files list to maintain the correct order
        List<File> convertedFiles = new ArrayList<>(totalFiles);
        for (int i = 0; i < totalFiles; i++) {
            convertedFiles.add(new File(tempDir, i + "_converted.mp3"));
        }
        
        // For tracking progress
        AtomicInteger completedFiles = new AtomicInteger(0);
        AtomicReference<Double> currentFileProgress = new AtomicReference<>(0.0);
        
        // Determine thread count (use fewer threads if we have fewer files)
        int threadCount = Math.min(numThreads, totalFiles);
        ExecutorService localExecutor = Executors.newFixedThreadPool(threadCount);
        
        // Use a countdown latch to wait for all conversions to complete
        CountDownLatch latch = new CountDownLatch(totalFiles);
        
        // Start progress monitoring thread if callback is provided
        Thread progressThread = null;
        if (progressCallback != null) {
            progressThread = new Thread(() -> {
                while (latch.getCount() > 0) {
                    try {
                        double fileProgress = currentFileProgress.get();
                        int completed = completedFiles.get();
                        double overallProgress = (completed + fileProgress) / totalFiles * 0.7; // 70% of total progress
                        progressCallback.accept(overallProgress);
                        Thread.sleep(100); // Update 10 times per second
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            progressThread.setDaemon(true);
            progressThread.start();
        }
        
        try {
            // Submit conversion tasks
            for (int i = 0; i < totalFiles; i++) {
                final int fileIndex = i;
                localExecutor.submit(() -> {
                    try {
                        File inputFile = inputFiles.get(fileIndex);
                        File outputFile = convertedFiles.get(fileIndex);
                        
                       // Debug log to verify settings are being applied
                       System.out.println("----------------------------------------------------");
                       System.out.println("Converting file " + (fileIndex+1) + "/" + totalFiles + " in parallel with quality settings:");
                       System.out.println("Bit Rate: " + bitRate + " bps");
                       System.out.println("Sampling Rate: " + samplingRate + " Hz");
                       System.out.println("----------------------------------------------------");
                       
                        AudioAttributes audioAttributes = new AudioAttributes();
                        audioAttributes.setCodec("libmp3lame");
                        audioAttributes.setBitRate(bitRate);
                        audioAttributes.setChannels(2);
                        audioAttributes.setSamplingRate(samplingRate);
                        
                        EncodingAttributes encodingAttributes = new EncodingAttributes();
                        encodingAttributes.setOutputFormat("mp3");
                        encodingAttributes.setAudioAttributes(audioAttributes);
                        
                        Encoder encoder = new Encoder();
                        encoder.encode(new MultimediaObject(inputFile), outputFile, encodingAttributes,
                            new EncoderProgressListener() {
                                @Override
                                public void sourceInfo(MultimediaInfo info) {}
                                
                                @Override
                                public void progress(int permil) {
                                    // Only update the current file progress if it's the actively tracked file
                                    if (completedFiles.get() == fileIndex) {
                                        currentFileProgress.set(permil / 1000.0);
                                    }
                                }
                                
                                @Override
                                public void message(String s) {}
                            });
                            
                        // Mark this conversion as complete
                        completedFiles.incrementAndGet();
                    } catch (Exception e) {
                        System.err.println("Error converting file " + fileIndex + ": " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            // Wait for all files to be converted
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("File conversion interrupted");
            }
            
            // Signal progress update at end of conversion phase
            if (progressCallback != null) {
                progressCallback.accept(0.7);  // 70% complete after conversion
            }
        } finally {
            localExecutor.shutdown();
            if (progressThread != null) {
                progressThread.interrupt();
            }
        }
        
        return convertedFiles;
    }
    
    /**
     * Concatenates multiple audio files into a single file.
     * This method efficiently transfers file data using FileChannels.
     * 
     * @param inputFiles list of files to concatenate
     * @param outputFile destination file
     * @param progressCallback callback for progress updates
     * @throws IOException if an I/O error occurs
     */
    private void concatenateFiles(List<File> inputFiles, File outputFile, Consumer<Double> progressCallback) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             FileChannel outputChannel = fos.getChannel()) {
            
            for (int i = 0; i < inputFiles.size(); i++) {
                File inputFile = inputFiles.get(i);
                try (FileInputStream fis = new FileInputStream(inputFile);
                     FileChannel inputChannel = fis.getChannel()) {
                    
                    outputChannel.transferFrom(inputChannel, outputChannel.size(), inputChannel.size());
                }
                
                if (progressCallback != null) {
                    double progress = 0.7 + ((i + 1.0) / inputFiles.size() * 0.1); // 70-80% progress
                    progressCallback.accept(progress);
                }
            }
        }
    }
    
    /**
     * Converts an audio file to M4B format.
     * This method is a backward compatibility wrapper and should not be used in new code.
     * Use the version with explicit quality parameters instead.
     * 
     * @deprecated Use convertToM4B with explicit quality parameters instead
     * @param inputFile input audio file
     * @param outputFile output M4B file
     * @param progressCallback callback for progress updates
     * @throws EncoderException if encoding fails
     */
    @Deprecated
    private void convertToM4B(File inputFile, File outputFile, Consumer<Double> progressCallback) throws EncoderException {
        // WARNING: This method uses shared class variables and is not thread-safe
        // It should only be used by legacy code or in cases where thread safety is not a concern
        System.out.println("----------------------------------------------------");
        System.out.println("WARNING: Using non-thread-safe convertToM4B method");
        System.out.println("Converting to M4B using current processor settings:");
        System.out.println("- Bit Rate: " + audioBitRate + " bps");
        System.out.println("- Sampling Rate: " + audioSamplingRate + " Hz");
        System.out.println("----------------------------------------------------");
        
        // Forward to the full version of the method with current quality settings
        convertToM4B(inputFile, outputFile, progressCallback, audioBitRate, audioSamplingRate);
    }
   
   /**
    * Converts an audio file to M4B format with explicit quality settings.
    * This uses the JAVE library to convert to the iPod format, which produces an M4B file
    * compatible with most audiobook players.
    * 
    * @param inputFile input audio file
    * @param outputFile output M4B file
    * @param progressCallback callback for progress updates
    * @param bitRate explicit bit rate to use (e.g. 64000 or 128000)
    * @param samplingRate explicit sampling rate to use (e.g. 22050 or 44100)
    * @throws EncoderException if encoding fails
    */
   private void convertToM4B(File inputFile, File outputFile, Consumer<Double> progressCallback, 
                           int bitRate, int samplingRate) throws EncoderException {
        // Debug log to verify settings are being applied
        System.out.println("----------------------------------------------------");
        System.out.println("Converting to M4B with quality settings:");
        System.out.println("Bit Rate: " + bitRate + " bps");
        System.out.println("Sampling Rate: " + samplingRate + " Hz");
        System.out.println("----------------------------------------------------");
        
        // Configure audio attributes with explicit quality settings
        AudioAttributes audioAttributes = new AudioAttributes();
        audioAttributes.setCodec("aac");
        audioAttributes.setBitRate(bitRate);
        audioAttributes.setChannels(2);
        audioAttributes.setSamplingRate(samplingRate);
        
        // Configure encoding attributes
        EncodingAttributes encodingAttributes = new EncodingAttributes();
        encodingAttributes.setOutputFormat("ipod");
        encodingAttributes.setAudioAttributes(audioAttributes);
        
        // Report starting this phase if callback exists
        if (progressCallback != null) {
            progressCallback.accept(0.8); // Start at 80%
        }
        
        // Create encoder and start conversion
        Encoder encoder = new Encoder();
        encoder.encode(new MultimediaObject(inputFile), outputFile, encodingAttributes,
            new EncoderProgressListener() {
                @Override
                public void sourceInfo(MultimediaInfo info) {}
                
                @Override
                public void progress(int permil) {
                    if (progressCallback != null) {
                        // Only use 80-90% of the progress bar if we're adding metadata later
                        double progressRange = metadataEnabled ? 0.1 : 0.2;
                        double progress = Math.min(0.8 + (permil / 1000.0 * progressRange), 0.9);
                        progressCallback.accept(progress);
                    }
                }
                
                @Override
                public void message(String s) {
                    // Log any encoder messages for debugging
                    System.out.println("Encoder message: " + s);
                }
            });
    }
}

