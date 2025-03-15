package com.jk24.audiobookcreator;

import com.jk24.audiobookcreator.model.Audiobook;
import com.jk24.audiobookcreator.model.BookAudio;
import com.jk24.audiobookcreator.processor.AudiobookProcessor;
import com.jk24.audiobookcreator.ui.ProcessingItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests to verify the package restructuring.
 */
public class AudiobookCreatorTests {
    
    private Audiobook audiobook;
    
    @BeforeEach
    public void setup() {
        audiobook = new Audiobook();
        audiobook.setTitle("Test Audiobook");
        audiobook.setAuthor("Test Author");
        audiobook.setSeries("Test Series");
        audiobook.setBookNumber(1);
    }
    
    @Test
    public void testBasicAudiobookProperties() {
        assertEquals("Test Audiobook", audiobook.getTitle());
        assertEquals("Test Author", audiobook.getAuthor());
        assertEquals("Test Series", audiobook.getSeries());
        assertEquals(1, audiobook.getBookNumber());
    }
    
    @Test
    public void testAudiobookIsEmptyByDefault() {
        assertTrue(audiobook.isEmpty());
        assertEquals(0, audiobook.getAudioFiles().size());
    }
    
    @Test
    public void testAudioProcessorCreation() {
        // Simply test that we can create an AudiobookProcessor instance
        AudiobookProcessor processor = new AudiobookProcessor();
        
        // Basic configuration
        processor.setMultithreadingEnabled(true);
        processor.setNumThreads(2);
        processor.setMetadataEnabled(true);
        
        // Verify we can access the processor without errors
        assertNotNull(processor);
    }
    
    @Test
    public void testBookAudioBasics() {
        File mockFile = new File("test.mp3");
        BookAudio bookAudio = new BookAudio(mockFile);
        
        assertEquals("test", bookAudio.getTitle());
        assertEquals(mockFile, bookAudio.getFile());
        
        bookAudio.setTitle("New Title");
        assertEquals("New Title", bookAudio.getTitle());
    }
    
    @Test
    public void testAddingAudioToBook() {
        File mockFile1 = new File("test1.mp3");
        File mockFile2 = new File("test2.mp3");
        
        BookAudio bookAudio1 = new BookAudio(mockFile1);
        BookAudio bookAudio2 = new BookAudio(mockFile2);
        
        audiobook.addAudioFile(bookAudio1);
        audiobook.addAudioFile(bookAudio2);
        
        assertFalse(audiobook.isEmpty());
        assertEquals(2, audiobook.getAudioFiles().size());
        
        // Test that we can't modify the returned list directly
        List<BookAudio> audioFiles = audiobook.getAudioFiles();
        assertThrows(UnsupportedOperationException.class, () -> {
            audioFiles.add(new BookAudio(new File("test3.mp3")));
        });
    }
    
    @Test
    public void testProgressUpdatesHandling() {
        // Create a test audiobook
        Audiobook testBook = new Audiobook();
        testBook.setTitle("Test Progress Book");
        
        // Test the progress values and status transitions with the revised ProcessingItem code
        // This tests the core of the progress handling fix
        ProcessingItem item = new ProcessingItem(testBook);
        
        // Test initial state
        assertEquals(0.0, item.getProgress(), 0.001);
        assertEquals("Pending", item.getStatus());
        
        // Test small progress update
        item.setProgress(0.01);
        assertEquals(0.01, item.getProgress(), 0.001);
        assertEquals("Processing", item.getStatus());
        
        // Test normal progress update
        item.setProgress(0.5);
        assertEquals(0.5, item.getProgress(), 0.001);
        assertEquals("Processing", item.getStatus());
        
        // Test completion
        item.setProgress(1.0);
        assertEquals(1.0, item.getProgress(), 0.001);
        assertEquals("Completed", item.getStatus());
        
        // Test that setting error status is preserved even with progress updates
        item.setStatus("Error");
        assertEquals("Error", item.getStatus());
        
        // Progress update shouldn't change error status
        item.setProgress(0.75);
        assertEquals(0.75, item.getProgress(), 0.001);
        assertEquals("Error", item.getStatus(), "Error status should be preserved");
        
        // Even completion shouldn't automatically change error status
        item.setProgress(1.0);
        assertEquals(1.0, item.getProgress(), 0.001);
        assertEquals("Error", item.getStatus(), "Error status should be preserved on completion");
        
        // Test reset for retry
        item.resetForRetry();
        assertEquals(0.0, item.getProgress(), 0.001);
        assertEquals("Pending", item.getStatus());
    }
    
    @Test
    public void testMultipleBookProgressTracking() throws Exception {
        // Test that multiple books can be processed simultaneously without UI issues
        final int numBooks = 3; // Process 3 books simultaneously
        List<Audiobook> testBooks = new ArrayList<>();
        
        // Create test books
        for (int i = 0; i < numBooks; i++) {
            Audiobook book = new Audiobook();
            book.setTitle("Test Book " + (i+1));
            book.setAuthor("Test Author");
            book.setBookNumber(i+1);
            testBooks.add(book);
        }
        
        // Create mock processor that will simulate progress
        MockAudiobookProcessor mockProcessor = new MockAudiobookProcessor();
        
        // Add books to process
        List<CompletableFuture<File>> futures = new ArrayList<>();
        CountDownLatch allBooksCompleted = new CountDownLatch(numBooks);
        
        for (int i = 0; i < numBooks; i++) {
            Audiobook book = testBooks.get(i);
            File outputFile = new File("test_output_" + i + ".m4b");
            
            // Process book and add future to our list
            CompletableFuture<File> future = mockProcessor.processAudiobook(book, outputFile, null, progress -> {});
            futures.add(future);
            
            // Add completion handler
            future.thenRun(allBooksCompleted::countDown);
        }
        
        // Wait for all books to complete processing or timeout
        boolean allCompleted = allBooksCompleted.await(10, TimeUnit.SECONDS);
        
        // Verify all books completed successfully
        assertTrue(allCompleted, "All books should have completed processing");
        
        // Verify each book has the correct final status
        for (int i = 0; i < numBooks; i++) {
            int index = i; // Final copy for lambda
            AtomicBoolean statusVerified = new AtomicBoolean(false);
            
            for (ProcessingItem item : mockProcessor.getProcessingItems()) {
                if (item.getAudiobook().getTitle().equals("Test Book " + (index + 1))) {
                    assertEquals("Completed", item.getStatus(), 
                            "Book " + (index + 1) + " should have 'Completed' status");
                    assertEquals(1.0, item.getProgress(), 0.001,
                            "Book " + (index + 1) + " should have 100% progress");
                    statusVerified.set(true);
                    break;
                }
            }
            
            assertTrue(statusVerified.get(), "Status for book " + (i + 1) + " should have been verified");
        }
    }
    
    /**
     * Mock AudiobookProcessor for testing progress updates without actual file processing.
     */
    private static class MockAudiobookProcessor extends AudiobookProcessor {
        private final List<ProcessingItem> items = new ArrayList<>();
        
        public List<ProcessingItem> getProcessingItems() {
            return items;
        }
        
        @Override
        public CompletableFuture<File> processAudiobook(
                Audiobook audiobook, 
                File outputFile, 
                File coverImage, 
                java.util.function.Consumer<Double> progressCallback) {
            
            // Create and track a processing item
            ProcessingItem item = new ProcessingItem(audiobook);
            items.add(item);
            
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // Simulate processing with progress updates
                    for (int i = 1; i <= 20; i++) {
                        double progress = i / 20.0;
                        progressCallback.accept(progress);
                        Thread.sleep(100); // Short delay between updates
                    }
                    return outputFile;
                } catch (Exception e) {
                    throw new RuntimeException("Mock processing failed", e);
                }
            });
        }
    }
}