package com.jindle.images;

import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ImageResizer {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 800;

    public void resize(InputStream is, File file) throws IOException {
        BufferedImage bi = Scalr.resize(ImageIO.read(is), Scalr.Method.SPEED, Scalr.Mode.AUTOMATIC, WIDTH, HEIGHT);
        ImageIO.write(bi, "jpg", file);
    }
}
