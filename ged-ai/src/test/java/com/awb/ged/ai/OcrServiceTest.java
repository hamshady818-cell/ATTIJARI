package com.awb.ged.ai;

import net.sourceforge.tess4j.TesseractException;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class OcrServiceTest {

    @Test
    void extraitTexteDepuisUneImage() throws TesseractException {
        File image = new File("C:\\Users\\TEST\\Downloads\\text.png");
        // Skip gracefully if the test image is not available (CI / other environments)
        assumeTrue(image.exists() && image.canRead(),
                "Test image not found at " + image.getAbsolutePath() + " — skipping OCR test");

        OcrService service = new OcrService("C:\\Program Files\\Tesseract-OCR\\tessdata");
        String resultat = service.extraireTexte(image);

        System.out.println("Texte extrait : ");
        System.out.println(resultat);

        assertTrue(resultat.length() > 0);
    }
}