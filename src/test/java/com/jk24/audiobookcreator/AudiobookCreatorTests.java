package com.jk24.audiobookcreator;

import com.jk24.audiobookcreator.model.Audiobook;
import com.jk24.audiobookcreator.model.BookAudio;
import com.jk24.audiobookcreator.processor.AudiobookProcessor;
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
}