package com.keendly.qr;

import org.junit.Test;

public class QrCodeGeneratorTest {

    @Test
    public void test(){
        String file = new QrCodeGenerator().generate("/tmp", "http://www.wykop.pl/");

        System.out.println(file);
    }
}
