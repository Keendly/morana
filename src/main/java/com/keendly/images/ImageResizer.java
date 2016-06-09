package com.keendly.images;

import org.imgscalr.Scalr;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class ImageResizer {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 800;

    public BufferedImage resize(BufferedImage input) throws IOException {
        return Scalr.resize(input, Scalr.Method.SPEED, Scalr.Mode.AUTOMATIC, WIDTH, HEIGHT);
    }
}
