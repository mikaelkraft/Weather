package com.weatherapp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic test for HistoryManager that verifies adding an entry updates the in-memory list.
 * Note: HistoryManager writes to a file in the working directory; this test is intentionally small
 * and only validates in-memory behavior.
 */
public class HistoryManagerTest {
    @Test
    public void testAddEntry() {
        try {
            java.nio.file.Path tmp = java.nio.file.Files.createTempFile("history-test", ".json");
            tmp.toFile().deleteOnExit();
            HistoryManager hm = new HistoryManager(tmp);
            int before = hm.getHistory().size();
            hm.addEntry("UnitTestCity", 123456789L);
            assertEquals(before + 1, hm.getHistory().size());
            assertEquals("UnitTestCity", hm.getHistory().get(0).getCity());
        } catch (Exception ex) {
            fail("Exception during test: " + ex.getMessage());
        }
    }
}
