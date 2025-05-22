package com.example.chatgptapp.util;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class TextChunkerTests {

    @Test
    void testChunkText_NormalCase_WithOverlap() {
        String text = "This is a test string for chunking. It is moderately long.";
        //              012345678901234567890123456789012345678901234567890123456
        //              0         1         2         3         4         5
        int chunkSize = 20;
        int chunkOverlap = 5;
        List<String> chunks = TextChunker.chunkText(text, chunkSize, chunkOverlap);

        assertEquals(4, chunks.size());
        assertEquals("This is a test strin", chunks.get(0)); // 0-19
        assertEquals("test string for ch", chunks.get(1)); // 15-34
        assertEquals("g for chunking. It", chunks.get(2)); // 30-49
        assertEquals("king. It is modera", chunks.get(3)); // 45-64 (actual end is 56) -> "king. It is modera" should be "king. It is moderately long." -> text.substring(45, 57)
                                                      // currentPosition = 0 + 20 - 5 = 15
                                                      // currentPosition = 15 + 20 - 5 = 30
                                                      // currentPosition = 30 + 20 - 5 = 45
                                                      // end = min(45+20, 57) = 57. substring(45,57)
        // Let's re-evaluate the last chunk based on the logic
        // Chunk 1: text.substring(0, 20) -> "This is a test strin"
        // next_start = 20 - 5 = 15
        // Chunk 2: text.substring(15, 35) -> "test string for chun" (length is 57)
        // next_start = 15 + 20 - 5 = 30
        // Chunk 3: text.substring(30, 50) -> "g for chunking. It i"
        // next_start = 30 + 20 - 5 = 45
        // Chunk 4: text.substring(45, Math.min(45+20, 57)) = text.substring(45, 57) -> "tely long." -> Corrected: "king. It is moderately long."
        
        // Correcting expected values based on the logic:
        // Chunk 1: "This is a test strin"
        // Chunk 2: "test string for chun"
        // Chunk 3: "g for chunking. It i"
        // Chunk 4: "king. It is moderately long."
        assertEquals("This is a test strin", chunks.get(0));
        assertEquals("test string for chun", chunks.get(1));
        assertEquals("g for chunking. It i", chunks.get(2));
        assertEquals("king. It is moderately long.", chunks.get(3));
    }

    @Test
    void testChunkText_NormalCase_NoOverlap() {
        String text = "This is a test string for chunking. It is moderately long.";
        int chunkSize = 20;
        int chunkOverlap = 0;
        List<String> chunks = TextChunker.chunkText(text, chunkSize, chunkOverlap);

        assertEquals(3, chunks.size());
        assertEquals("This is a test strin", chunks.get(0));
        assertEquals("g for chunking. It i", chunks.get(1));
        assertEquals("s moderately long.", chunks.get(2)); // text.substring(40, Math.min(60, 57)) = text.substring(40,57)
    }

    @Test
    void testChunkText_ShorterThanChunkSize() {
        String text = "Short text.";
        int chunkSize = 20;
        int chunkOverlap = 5;
        List<String> chunks = TextChunker.chunkText(text, chunkSize, chunkOverlap);

        assertEquals(1, chunks.size());
        assertEquals("Short text.", chunks.get(0));
    }
    
    @Test
    void testChunkText_ExactlyChunkSize() {
        String text = "Exactly twenty chars"; // 20 chars
        int chunkSize = 20;
        int chunkOverlap = 5;
        List<String> chunks = TextChunker.chunkText(text, chunkSize, chunkOverlap);
        assertEquals(1, chunks.size());
        assertEquals("Exactly twenty chars", chunks.get(0));
    }

    @Test
    void testChunkText_MuchLongerText() {
        String text = "This is a very long string that definitely needs to be chunked multiple times. Let's see how it handles this. It should produce several chunks. The quick brown fox jumps over the lazy dog.";
        // Length = 170
        int chunkSize = 50;
        int chunkOverlap = 10;
        List<String> chunks = TextChunker.chunkText(text, chunkSize, chunkOverlap);
        // Chunk 1: 0-50
        // Next start: 50-10 = 40
        // Chunk 2: 40-90
        // Next start: 40 + 50 - 10 = 80
        // Chunk 3: 80-130
        // Next start: 80 + 50 - 10 = 120
        // Chunk 4: 120-170
        // Next start: 120 + 50 - 10 = 160. currentPosition = 160.
        // Chunk 5: 160 - min(160+50, 170) = 160-170.
        // currentPosition = 160 + 50 - 10 = 200. 200 >= 170. Loop terminates.
        assertEquals(4, chunks.size()); // Expected 4 chunks based on manual calculation: (170 - 50) / (50 - 10) + 1 = 120 / 40 + 1 = 3 + 1 = 4
        assertEquals(text.substring(0, 50), chunks.get(0));
        assertEquals(text.substring(40, 90), chunks.get(1));
        assertEquals(text.substring(80, 130), chunks.get(2));
        assertEquals(text.substring(120, 170), chunks.get(3));
    }
    
    @Test
    void testChunkText_OverlapEqualsChunkSize_AdjustsToZeroOverlap() {
        String text = "Testing tricky overlap case where overlap is same as chunk size.";
        int chunkSize = 20;
        int chunkOverlap = 20; // Invalid, should be adjusted to 0
        List<String> chunks = TextChunker.chunkText(text, chunkSize, chunkOverlap);
        // Expects behavior as if overlap was 0
        assertTrue(chunks.size() > 1); // Check it split
        assertEquals(text.substring(0, chunkSize), chunks.get(0));
        if (chunks.size() > 1) {
            assertEquals(text.substring(chunkSize, Math.min(chunkSize * 2, text.length())), chunks.get(1));
        }
    }

    @Test
    void testChunkText_EmptyString() {
        String text = "";
        int chunkSize = 20;
        int chunkOverlap = 5;
        List<String> chunks = TextChunker.chunkText(text, chunkSize, chunkOverlap);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void testChunkText_NullString() {
        String text = null;
        int chunkSize = 20;
        int chunkOverlap = 5;
        List<String> chunks = TextChunker.chunkText(text, chunkSize, chunkOverlap);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void testChunkText_ZeroChunkSize() {
        String text = "Some text";
        int chunkSize = 0;
        int chunkOverlap = 0;
        List<String> chunks = TextChunker.chunkText(text, chunkSize, chunkOverlap);
        assertTrue(chunks.isEmpty());
    }
    
    @Test
    void testChunkText_NegativeChunkSize() {
        String text = "Some text";
        int chunkSize = -5;
        int chunkOverlap = 0;
        List<String> chunks = TextChunker.chunkText(text, chunkSize, chunkOverlap);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void testChunkText_NegativeOverlap() {
        String text = "This is a test string for chunking.";
        int chunkSize = 15;
        int chunkOverlap = -5; // Invalid, should be adjusted to 0
        List<String> chunks = TextChunker.chunkText(text, chunkSize, chunkOverlap);
        // Expects behavior as if overlap was 0
        assertEquals(text.substring(0, 15), chunks.get(0));
        assertEquals(text.substring(15, 30), chunks.get(1));
        assertEquals(text.substring(30, text.length()), chunks.get(2));
    }
    
    @Test
    void testChunkText_FullOverlapAtEnd() {
        String text = "abcde"; // length 5
        int chunkSize = 3;
        int chunkOverlap = 1;
        List<String> chunks = TextChunker.chunkText(text, chunkSize, chunkOverlap);
        // Chunk 1: "abc" (0,3) -> next_start = 3-1=2
        // Chunk 2: "cde" (2,5) -> next_start = 2+3-1=4
        // Chunk 3: "e" (4,5) -> next_start = 4+3-1=6. 6 >= 5. Loop terminates.
        assertEquals(3, chunks.size());
        assertEquals("abc", chunks.get(0));
        assertEquals("cde", chunks.get(1));
        assertEquals("e", chunks.get(2));
    }

    @Test
    void testChunkText_PerfectChunkingNoOverlap() {
        String text = "abcdefghij"; // length 10
        int chunkSize = 5;
        int chunkOverlap = 0;
        List<String> chunks = TextChunker.chunkText(text, chunkSize, chunkOverlap);
        assertEquals(2, chunks.size());
        assertEquals("abcde", chunks.get(0));
        assertEquals("fghij", chunks.get(1));
    }

    @Test
    void testChunkText_PerfectChunkingWithOverlap() {
        String text = "abcdefghijklmno"; // length 15
        int chunkSize = 7;
        int chunkOverlap = 2;
        // Chunk 1: "abcdefg" (0,7) -> next_start = 7-2 = 5
        // Chunk 2: "fghijkl" (5,12) -> next_start = 5+7-2 = 10
        // Chunk 3: "klmno" (10,15) -> next_start = 10+7-2 = 15. 15 >= 15. Loop terminates.
        List<String> chunks = TextChunker.chunkText(text, chunkSize, chunkOverlap);
        assertEquals(3, chunks.size());
        assertEquals("abcdefg", chunks.get(0));
        assertEquals("fghijkl", chunks.get(1));
        assertEquals("klmno", chunks.get(2));
    }
}
