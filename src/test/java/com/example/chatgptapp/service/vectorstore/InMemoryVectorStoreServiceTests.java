package com.example.chatgptapp.service.vectorstore;

import com.example.chatgptapp.dto.VectorData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryVectorStoreServiceTests {

    private InMemoryVectorStoreService vectorStoreService;

    private VectorData item1, item2, item3, item4_diff_dim;

    @BeforeEach
    void setUp() {
        vectorStoreService = new InMemoryVectorStoreService();
        // Sample data
        item1 = new VectorData("id1", "Content related to apples and bananas", Arrays.asList(0.1, 0.2, 0.7));
        item2 = new VectorData("id2", "Content about oranges and grapes", Arrays.asList(0.3, 0.4, 0.3));
        item3 = new VectorData("id3", "More content about apples, slightly different", Arrays.asList(0.15, 0.25, 0.6));
        item4_diff_dim = new VectorData("id4_diff", "Content with different dimension embedding", Arrays.asList(0.5, 0.5));

        vectorStoreService.store(item1);
        vectorStoreService.store(item2);
        vectorStoreService.store(item3);
        vectorStoreService.store(item4_diff_dim);
    }

    @Test
    void testStore_AddsItem() {
        VectorData newItem = new VectorData("id_new", "New content", Arrays.asList(0.6, 0.6, 0.6));
        vectorStoreService.store(newItem);
        // To verify, we'll use findSimilar with a vector identical to newItem's embedding
        List<VectorData> similar = vectorStoreService.findSimilar(Arrays.asList(0.6, 0.6, 0.6), 1);
        assertFalse(similar.isEmpty());
        assertEquals("id_new", similar.get(0).getId());
    }
    
    @Test
    void testStore_UpdatesExistingItem() {
        VectorData updatedItem1 = new VectorData("id1", "Updated content for apples", Arrays.asList(0.11, 0.22, 0.77));
        vectorStoreService.store(updatedItem1);
        
        List<VectorData> similar = vectorStoreService.findSimilar(Arrays.asList(0.11, 0.22, 0.77), 1);
        assertEquals("Updated content for apples", similar.get(0).getContent());
    }


    @Test
    void testFindSimilar_EmptyStore() {
        vectorStoreService.clear();
        List<VectorData> similar = vectorStoreService.findSimilar(Arrays.asList(0.1, 0.2, 0.3), 1);
        assertTrue(similar.isEmpty());
    }

    @Test
    void testFindSimilar_StoreWithOneItem_FindsIt() {
        vectorStoreService.clear();
        VectorData singleItem = new VectorData("single", "Single item test", Arrays.asList(0.5, 0.5, 0.5));
        vectorStoreService.store(singleItem);
        List<VectorData> similar = vectorStoreService.findSimilar(Arrays.asList(0.5, 0.5, 0.5), 1);
        assertEquals(1, similar.size());
        assertEquals("single", similar.get(0).getId());
    }

    @Test
    void testFindSimilar_TopK_LessThanAvailable() {
        List<Double> queryEmbedding = Arrays.asList(0.12, 0.22, 0.65); // Closest to item1 and item3
        List<VectorData> similar = vectorStoreService.findSimilar(queryEmbedding, 1);
        assertEquals(1, similar.size());
        // Expect item1 or item3 (item3 is slightly closer to this query)
        assertEquals("id3", similar.get(0).getId()); 
    }

    @Test
    void testFindSimilar_TopK_MoreThanAvailableExcludingDifferentDim() {
        List<Double> queryEmbedding = Arrays.asList(0.1, 0.2, 0.3);
        List<VectorData> similar = vectorStoreService.findSimilar(queryEmbedding, 5); // Request 5, but only 3 have matching dimensions
        assertEquals(3, similar.size()); // item4_diff_dim should be skipped
    }

    @Test
    void testFindSimilar_SimilarityRanking() {
        List<Double> queryEmbedding = Arrays.asList(0.12, 0.22, 0.65); // Query embedding
        // Expected order of similarity (cosine similarity, higher is better):
        // Query: [0.12, 0.22, 0.65]
        // Item3: [0.15, 0.25, 0.6] -> High similarity
        // Item1: [0.1, 0.2, 0.7]   -> High similarity, but slightly less than item3 for this query
        // Item2: [0.3, 0.4, 0.3]   -> Lower similarity
        
        List<VectorData> similar = vectorStoreService.findSimilar(queryEmbedding, 3);
        assertEquals(3, similar.size());
        assertEquals("id3", similar.get(0).getId(), "Item3 should be most similar");
        assertEquals("id1", similar.get(1).getId(), "Item1 should be second most similar");
        assertEquals("id2", similar.get(2).getId(), "Item2 should be least similar of the three");
    }
    
    @Test
    void testFindSimilar_NoSimilarItems() {
        // Query vector very different from stored items
        List<Double> queryEmbedding = Arrays.asList(0.9, 0.9, 0.1);
        List<VectorData> similar = vectorStoreService.findSimilar(queryEmbedding, 1);
        // It will still return the "closest" even if not very similar, as long as dimensions match.
        // The test here is more about ensuring it doesn't crash and returns something.
        assertFalse(similar.isEmpty()); 
    }

    @Test
    void testFindSimilar_QueryVectorDifferentDimension() {
        List<Double> queryEmbedding = Arrays.asList(0.1, 0.2); // 2 dimensions
        // item1, item2, item3 are 3 dimensions. item4_diff_dim is 2 dimensions.
        List<VectorData> similar = vectorStoreService.findSimilar(queryEmbedding, 3);
        assertEquals(1, similar.size()); // Only item4_diff_dim should match dimension
        assertEquals("id4_diff", similar.get(0).getId());
    }
    
    @Test
    void testFindSimilar_QueryVectorNullOrEmpty() {
        assertTrue(vectorStoreService.findSimilar(null, 1).isEmpty());
        assertTrue(vectorStoreService.findSimilar(List.of(), 1).isEmpty());
    }

    @Test
    void testFindSimilar_TopKZeroOrNegative() {
        List<Double> queryEmbedding = Arrays.asList(0.1, 0.2, 0.7);
        assertTrue(vectorStoreService.findSimilar(queryEmbedding, 0).isEmpty());
        assertTrue(vectorStoreService.findSimilar(queryEmbedding, -1).isEmpty());
    }

    @Test
    void testClear() {
        vectorStoreService.clear();
        List<VectorData> similar = vectorStoreService.findSimilar(Arrays.asList(0.1, 0.2, 0.3), 1);
        assertTrue(similar.isEmpty());
        // Try storing again after clear
        vectorStoreService.store(item1);
        similar = vectorStoreService.findSimilar(item1.getEmbedding(), 1);
        assertEquals(1, similar.size());
    }

    @Test
    void testStore_NullVectorData() {
        // Count items before
        long countBefore = vectorStoreService.findSimilar(Arrays.asList(0.1,0.1,0.1), 10).size();
        vectorStoreService.store(null);
        long countAfter = vectorStoreService.findSimilar(Arrays.asList(0.1,0.1,0.1), 10).size();
        assertEquals(countBefore, countAfter); // No change
    }

    @Test
    void testStore_VectorDataWithNullId() {
        long countBefore = vectorStoreService.findSimilar(Arrays.asList(0.1,0.1,0.1), 10).size();
        vectorStoreService.store(new VectorData(null, "content", Arrays.asList(0.1,0.1,0.1)));
        long countAfter = vectorStoreService.findSimilar(Arrays.asList(0.1,0.1,0.1), 10).size();
        assertEquals(countBefore, countAfter);
    }
    
    @Test
    void testStore_VectorDataWithNullEmbedding() {
        long countBefore = vectorStoreService.findSimilar(Arrays.asList(0.1,0.1,0.1), 10).size();
        vectorStoreService.store(new VectorData("id_null_emb", "content", null));
        long countAfter = vectorStoreService.findSimilar(Arrays.asList(0.1,0.1,0.1), 10).size();
        assertEquals(countBefore, countAfter);
    }
    
    @Test
    void testFindSimilar_ItemWithNullEmbeddingInStore() {
        vectorStoreService.store(new VectorData("id_null_emb_in_store", "content", null));
        List<Double> queryEmbedding = Arrays.asList(0.1, 0.2, 0.7);
        // This should not throw an error and simply skip the item with null embedding
        List<VectorData> similar = vectorStoreService.findSimilar(queryEmbedding, 5);
        // Should return the 3 valid items
        assertEquals(3, similar.stream().filter(vd -> vd.getEmbedding() != null).count());
    }
}
