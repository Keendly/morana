package com.jindle.images;

import com.sun.imageio.plugins.jpeg.JPEGImageWriter;
import com.sun.imageio.plugins.jpeg.JPEGImageWriterSpi;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageCompressor {

    private static final float COMPRESSION_QUALITY = 0.5f;

    private static ImageWriteParam imageWriteParam;

    static {
        imageWriteParam = new JPEGImageWriteParam(null);
        imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        imageWriteParam.setCompressionQuality(COMPRESSION_QUALITY);
    }

    public byte[] compress(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageWriter imageWriter = new JPEGImageWriter(new JPEGImageWriterSpi());
        imageWriter.setOutput(ImageIO.createImageOutputStream(os));
        BufferedImage bufferedImage = ImageIO.read(is);
        if (bufferedImage.getTransparency() == Transparency.TRANSLUCENT){
            // create a blank, RGB, same width and height, and a white background
            BufferedImage newBufferedImage = new BufferedImage(bufferedImage.getWidth(),
                bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            newBufferedImage.createGraphics().drawImage(bufferedImage, 0, 0, Color.WHITE, null);
            bufferedImage = newBufferedImage;
        }
        imageWriter.write(null,  new IIOImage(bufferedImage, null, null), imageWriteParam);
        return os.toByteArray();
    }
}
