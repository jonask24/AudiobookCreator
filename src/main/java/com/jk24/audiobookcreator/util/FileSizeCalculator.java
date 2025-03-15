package com.jk24.audiobookcreator.util;

import com.jk24.audiobookcreator.model.BookAudio;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Utility class for calculating estimated output file sizes for audiobooks
 * based on different quality settings.
 */
public class FileSizeCalculator {

    // Constants for audio quality settings
    private static final int BIT_RATE_BEST = 128000; // 128 kbps
    private static final int BIT_RATE_BOOK = 64000; // 64 kbps
    private static final int SAMPLING_RATE_BEST = 44100; // 44.1 kHz
    private static final int SAMPLING_RATE_BOOK = 22050; // 22.05 kHz
    
    // Default overhead factor for M4B container format (conservative estimate)
    private static final double M4B_OVERHEAD_FACTOR = 1.05; // 5% overhead
    
    /**
     * Calculates the estimated output file size for the given audio files based on the selected quality.
     *
     * @param audioFiles List of audio files to calculate size for
     * @param isHighQuality Whether to use high quality or book optimized quality
     * @return Estimated size in bytes
     */
    public static long calculateEstimatedSize(List<BookAudio> audioFiles, boolean isHighQuality) {
        long totalInputSize = 0;
        long totalDuration = 0;
        
        // First, sum up the sizes of all input files
        for (BookAudio audio : audioFiles) {
            File file = audio.getFile();
            if (file.exists()) {
                totalInputSize += file.length();
                
                // If we had duration information, we could use it for more accurate calculation
                // Currently going with input file size as a proxy
            }
        }
        
        if (totalInputSize == 0) {
            return 0; // No valid files to calculate
        }
        
        // Calculate output size based on bit rate, which determines bytes per second
        int selectedBitRate = isHighQuality ? BIT_RATE_BEST : BIT_RATE_BOOK;
        double compressionRatio;
        
        // Estimate compression ratio based on quality setting
        if (isHighQuality) {
            // For high quality, approximately 1:1.2 ratio (less compression)
            compressionRatio = 0.8; 
        } else {
            // For book quality, approximately 1:2 ratio (more compression)
            compressionRatio = 0.4;
        }
        
        // Calculate estimated size
        long estimatedBytes = (long)(totalInputSize * compressionRatio * M4B_OVERHEAD_FACTOR);
        
        return estimatedBytes;
    }
    
    /**
     * Formats a byte size into a human-readable string (KB, MB, GB)
     *
     * @param bytes The size in bytes
     * @return A formatted string representing the size
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 0) {
            return "0 B";
        }
        
        DecimalFormat df = new DecimalFormat("#.##");
        
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return df.format(bytes / 1024.0) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return df.format(bytes / (1024.0 * 1024.0)) + " MB";
        } else {
            return df.format(bytes / (1024.0 * 1024.0 * 1024.0)) + " GB";
        }
    }
}