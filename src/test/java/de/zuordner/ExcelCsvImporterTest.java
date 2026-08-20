package de.zuordner;

import de.zuordner.importengine.ExcelCsvImporter;
import de.zuordner.importengine.TableData;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ExcelCsvImporterTest {

    @Test
    public void testReadGermanSemicolonCsv() throws Exception {
        ExcelCsvImporter importer = new ExcelCsvImporter();
        File csvFile = new File("Test123.csv");
        assertTrue(csvFile.exists(), "Test123.csv file should exist");

        try (FileInputStream fis = new FileInputStream(csvFile)) {
            TableData tableData = importer.readTablePreview(fis, "Test123.csv", "CSV Data", 0, 0);

            assertNotNull(tableData);
            assertEquals(9, tableData.getHeaders().size(), "Headers count should be 9");
            assertEquals("Vorname", tableData.getHeaders().get(0));
            assertEquals("Nachname", tableData.getHeaders().get(1));
            assertEquals("Alter", tableData.getHeaders().get(2));
            assertEquals("Geschlecht", tableData.getHeaders().get(3));

            assertEquals(30, tableData.getRows().size(), "Row count should be 30 participants");

            // Verify first participant: Marie Schäfer, 12, Weiblich
            assertEquals("Marie", tableData.getRows().get(0).get(0));
            assertEquals("Schäfer", tableData.getRows().get(0).get(1));
            assertEquals("12", tableData.getRows().get(0).get(2));
            assertEquals("Weiblich", tableData.getRows().get(0).get(3));
        }
    }
}
