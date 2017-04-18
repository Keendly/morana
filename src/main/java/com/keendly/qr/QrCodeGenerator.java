package com.keendly.qr;

import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;

import java.io.File;
import java.util.UUID;

public class QrCodeGenerator {

    public String generate(String directory, String url){
        String filename = generateUUID() + ".png";
        QRCode.from(url).to(ImageType.PNG).file(directory + File.separator + filename);
        return filename;
    }

    private String generateUUID(){
        return UUID.randomUUID().toString().replace("-","");
    }
}
