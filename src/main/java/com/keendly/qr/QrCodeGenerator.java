package com.keendly.qr;

import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.UUID;

public class QrCodeGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(QrCodeGenerator.class);


    public String generate(String directory, String url){
        String filename = generateUUID() + ".png";
        try {
            FileOutputStream fos = new FileOutputStream(directory + File.separator + filename);
            QRCode.from(url).to(ImageType.PNG).writeTo(fos);
            return filename;
        } catch (FileNotFoundException e) {
            LOG.error("Error creating qrcode for: " + url, e);
            return null;
        }
    }

    private String generateUUID(){
        return UUID.randomUUID().toString().replace("-","");
    }
}
