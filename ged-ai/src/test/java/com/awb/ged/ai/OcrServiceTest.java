package com.awb.ged.ai;

import net.sourceforge.tess4j.TesseractException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OcrServiceTest {

    @Test
    void extraitTexteDepuisUneImage() throws TesseractException {
        OcrService service = new OcrService("C:\\Program Files\\Tesseract-OCR\\tessdata");

        File image = new File("C:\\Users\\TEST\\Downloads\\text.png");
        String resultat = service.extraireTexte(image);

        System.out.println("Texte extrait : ");
        System.out.println(resultat);

        assertTrue(resultat.length() > 0);
    }
}