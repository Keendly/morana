package com.keendly.qr;

import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.UUID;

public class QrCodeGenerator {

    public String generate(String directory, String url){
        String filename = generateUUID() + ".png";
        try {
            FileOutputStream fos = new FileOutputStream(directory + File.separator + filename);
            QRCode.from(url).to(ImageType.PNG).writeTo(fos);
            return filename;
        } catch (FileNotFoundException e) {
            return null;
        }

    }

    private String generateUUID(){
        return UUID.randomUUID().toString().replace("-","");
    }
}
