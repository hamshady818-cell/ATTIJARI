package com.awb.ged.ai;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class OcrService {

    private final Tesseract tesseract;

    public OcrService(@Value("${tesseract.datapath}") String datapath) {
        this.tesseract = new Tesseract();
        this.tesseract.setDatapath(datapath);
    }

    public String extraireTexte(File image) throws TesseractException {
        return tesseract.doOCR(image);
    }
}