package com.example.chatgptapp.service;

import com.example.chatgptapp.dto.VectorData;
import com.example.chatgptapp.util.TextChunker; // Assuming TextChunker is used statically
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataIngestionServiceTests {

    @Mock
    private OpenAIService openAIService;

    @Mock
    private VectorStoreService vectorStoreService;

    @InjectMocks
    private DataIngestionService dataIngestionService;

    @Captor
    private ArgumentCaptor<VectorData> vectorDataArgumentCaptor;

    private final int testChunkSize = 50;
    private final int testChunkOverlap = 10;
    private final List<Double> dummyEmbedding = Arrays.asList(0.1, 0.2, 0.3);

    @BeforeEach
    void setUp() {
        // Set chunker parameters using ReflectionTestUtils
        ReflectionTestUtils.setField(dataIngestionService, "chunkSize", testChunkSize);
        ReflectionTestUtils.setField(dataIngestionService, "chunkOverlap", testChunkOverlap);
    }

    @Test
    void testIngestText_SuccessfulIngestion() {
        String documentId = "doc1";
        String text = "This is a sample text long enough to be split into multiple chunks for testing purposes.";
        // Expected chunks based on chunkSize=50, chunkOverlap=10
        // Chunk 1: "This is a sample text long enough to be split into" (0-49)
        // Chunk 2: " enough to be split into multiple chunks for tes" (40-89)
        // Chunk 3: " multiple chunks for testing purposes." (80 - end)
        List<String> expectedChunks = Arrays.asList(
                text.substring(0, 50),
                text.substring(40, 90),
                text.substring(80)
        );
        
        // Mock TextChunker.chunkText if it's complex, or rely on its own tests if simple.
        // For this example, let's assume TextChunker is tested separately and works as expected.
        // We can also mock it if we want to isolate DataIngestionService more.
        
        when(openAIService.getEmbeddings(anyString())).thenReturn(dummyEmbedding);

        dataIngestionService.ingestText(text, documentId);

        verify(openAIService, times(expectedChunks.size())).getEmbeddings(anyString());
        verify(vectorStoreService, times(expectedChunks.size())).store(vectorDataArgumentCaptor.capture());

        List<VectorData> capturedVectorData = vectorDataArgumentCaptor.getAllValues();
        assertEquals(expectedChunks.size(), capturedVectorData.size());

        for (int i = 0; i < expectedChunks.size(); i++) {
            VectorData vd = capturedVectorData.get(i);
            assertEquals(documentId + "_chunk_" + String.format("%04d", i), vd.getId());
            assertEquals(expectedChunks.get(i), vd.getContent());
            assertEquals(dummyEmbedding, vd.getEmbedding());
        }
    }
    
    @Test
    void testIngestText_WithStaticMockForTextChunker() {
         String documentId = "docMock";
        String text = "This is a sample text for static mock test.";
        List<String> mockChunks = Arrays.asList("This is a sample", " text for static", " mock test.");

        // Mocking the static TextChunker.chunkText method
        try (MockedStatic<TextChunker> mockedTextChunker = Mockito.mockStatic(TextChunker.class)) {
            mockedTextChunker.when(() -> TextChunker.chunkText(eq(text), eq(testChunkSize), eq(testChunkOverlap)))
                             .thenReturn(mockChunks);

            when(openAIService.getEmbeddings(anyString())).thenReturn(dummyEmbedding);

            dataIngestionService.ingestText(text, documentId);

            verify(openAIService, times(mockChunks.size())).getEmbeddings(anyString());
            verify(vectorStoreService, times(mockChunks.size())).store(vectorDataArgumentCaptor.capture());

            List<VectorData> capturedVectorData = vectorDataArgumentCaptor.getAllValues();
            assertEquals(mockChunks.size(), capturedVectorData.size());
            for (int i = 0; i < mockChunks.size(); i++) {
                assertEquals(mockChunks.get(i), capturedVectorData.get(i).getContent());
            }
        }
    }


    @Test
    void testIngestText_EmbeddingFailureForOneChunk() {
        String documentId = "doc2";
        String text = "Chunk one. Chunk two. Chunk three is very long for this example.";
        // Chunk1: "Chunk one. Chunk two. Chunk three is very long fo"
        // Chunk2: "hree is very long for this example."
        List<String> chunks = TextChunker.chunkText(text, testChunkSize, testChunkOverlap); // Should be 2 chunks

        when(openAIService.getEmbeddings(chunks.get(0))).thenReturn(dummyEmbedding);
        when(openAIService.getEmbeddings(chunks.get(1))).thenReturn(null); // Embedding fails for the second chunk

        dataIngestionService.ingestText(text, documentId);

        verify(openAIService, times(chunks.size())).getEmbeddings(anyString());
        // VectorStoreService.store should only be called for the chunk with successful embedding
        verify(vectorStoreService, times(1)).store(vectorDataArgumentCaptor.capture());
        
        VectorData captured = vectorDataArgumentCaptor.getValue();
        assertEquals(documentId + "_chunk_0000", captured.getId());
        assertEquals(chunks.get(0), captured.getContent());
    }

    @Test
    void testIngestText_NullOrEmptyText() {
        dataIngestionService.ingestText(null, "doc_null");
        dataIngestionService.ingestText("", "doc_empty");
        dataIngestionService.ingestText("   ", "doc_blank");


        verify(openAIService, never()).getEmbeddings(anyString());
        verify(vectorStoreService, never()).store(any(VectorData.class));
    }
    
    @Test
    void testIngestTexts_ListOfTextsAndIds() {
        List<String> texts = Arrays.asList("First document text.", "Second document text, longer.");
        List<String> docIds = Arrays.asList("id_A", "id_B");

        // Mock chunking behavior for simplicity, assuming TextChunker is well-tested
        List<String> chunksDocA = List.of("First document text."); // Shorter than chunkSize
        List<String> chunksDocB = List.of("Second document text, longer."); // Shorter than chunkSize

        when(openAIService.getEmbeddings(texts.get(0))).thenReturn(dummyEmbedding);
        when(openAIService.getEmbeddings(texts.get(1))).thenReturn(dummyEmbedding);
        
        // For this test, we'll rely on TextChunker's actual behavior for simplicity
        // as the texts are shorter than chunk size.

        dataIngestionService.ingestTexts(texts, docIds);

        verify(openAIService, times(2)).getEmbeddings(anyString());
        verify(vectorStoreService, times(2)).store(vectorDataArgumentCaptor.capture());
        
        List<VectorData> captured = vectorDataArgumentCaptor.getAllValues();
        assertEquals("id_A_chunk_0000", captured.get(0).getId());
        assertEquals(texts.get(0), captured.get(0).getContent());
        assertEquals("id_B_chunk_0000", captured.get(1).getId());
        assertEquals(texts.get(1), captured.get(1).getContent());
    }

    @Test
    void testIngestTexts_ListOfTexts_AutoGeneratedIds() {
        List<String> texts = Arrays.asList("Auto ID doc 1.", "Auto ID doc 2.");
        
        when(openAIService.getEmbeddings(anyString())).thenReturn(dummyEmbedding);

        dataIngestionService.ingestTexts(texts);

        verify(openAIService, times(texts.size())).getEmbeddings(anyString());
        verify(vectorStoreService, times(texts.size())).store(vectorDataArgumentCaptor.capture());

        List<VectorData> captured = vectorDataArgumentCaptor.getAllValues();
        assertTrue(captured.get(0).getId().startsWith("doc-0000-"));
        assertEquals(texts.get(0), captured.get(0).getContent());
        assertTrue(captured.get(1).getId().startsWith("doc-0001-"));
        assertEquals(texts.get(1), captured.get(1).getContent());
    }

    @Test
    void testIngestTexts_MismatchedLists() {
        List<String> texts = Arrays.asList("Text 1");
        List<String> docIds = Arrays.asList("id1", "id2"); // Mismatch

        dataIngestionService.ingestTexts(texts, docIds);
        verify(openAIService, never()).getEmbeddings(anyString());
        verify(vectorStoreService, never()).store(any(VectorData.class));
    }
    
    @Test
    void testIngestTexts_NullDocumentIdInList() {
        List<String> texts = Arrays.asList("Text with null ID");
        List<String> docIds = Collections.singletonList(null);
        
        when(openAIService.getEmbeddings(texts.get(0))).thenReturn(dummyEmbedding);
        
        dataIngestionService.ingestTexts(texts, docIds);
        
        verify(vectorStoreService, times(1)).store(vectorDataArgumentCaptor.capture());
        VectorData captured = vectorDataArgumentCaptor.getValue();
        assertTrue(captured.getId().startsWith("doc-")); // Auto-generated
        assertTrue(captured.getId().endsWith("_chunk_0000"));
        assertEquals(texts.get(0), captured.getContent());
    }
}
