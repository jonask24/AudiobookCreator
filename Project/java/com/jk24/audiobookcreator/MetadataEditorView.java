package com.jk24.audiobookcreator;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 * UI component for editing audiobook metadata
 */
public class MetadataEditorView extends GridPane {
    private final TextField titleField;
    private final TextField authorField;
    private final TextField seriesField;
    private final TextField bookNumberField;
    private final Audiobook audiobook;
    
    /**
     * Creates a new metadata editor view for the specified audiobook.
     * Initial field values are set from the audiobook's current metadata.
     * 
     * @param audiobook the audiobook to edit metadata for
     */
    public MetadataEditorView(Audiobook audiobook) {
        this.audiobook = audiobook;
        
        // Reduce the horizontal gap between label and field
        setHgap(5);
        setVgap(10);
        setPadding(new Insets(10));
        
        // Reduce padding to minimize extra space
        setPadding(new Insets(5));
        
        // Create form fields
        titleField = new TextField(audiobook.getTitle());
        authorField = new TextField(audiobook.getAuthor());
        seriesField = new TextField(audiobook.getSeries());
        bookNumberField = new TextField(String.valueOf(audiobook.getBookNumber()));
        
        // Set preferred widths to minimize unnecessary space
        titleField.setPrefWidth(150);
        authorField.setPrefWidth(150);
        seriesField.setPrefWidth(150);
        bookNumberField.setPrefWidth(150);
        
        // Add form controls
        add(new Label("Title"), 0, 0);
        add(titleField, 1, 0);
        add(new Label("Author"), 0, 1);
        add(authorField, 1, 1);
        add(new Label("Series"), 0, 2);
        add(seriesField, 1, 2);
        add(new Label("Book #"), 0, 3);
        add(bookNumberField, 1, 3);
        
        // Bind field values to audiobook properties
        titleField.textProperty().addListener((obs, oldVal, newVal) -> 
            audiobook.setTitle(newVal));
            
        authorField.textProperty().addListener((obs, oldVal, newVal) -> 
            audiobook.setAuthor(newVal));
            
        seriesField.textProperty().addListener((obs, oldVal, newVal) -> 
            audiobook.setSeries(newVal));
            
        bookNumberField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                int bookNum = Integer.parseInt(newVal);
                audiobook.setBookNumber(bookNum);
            } catch (NumberFormatException e) {
                // Ignore invalid input
            }
        });
    }
    
    /**
     * Updates all form fields from the current audiobook data.
     * This is useful when the audiobook's metadata has been changed
     * externally, such as after auto-fill operations.
     */
    public void updateFromAudiobook() {
        titleField.setText(audiobook.getTitle());
        authorField.setText(audiobook.getAuthor());
        seriesField.setText(audiobook.getSeries());
        bookNumberField.setText(String.valueOf(audiobook.getBookNumber()));
    }
}