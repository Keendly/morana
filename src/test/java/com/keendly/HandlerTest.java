package com.keendly;

import com.keendly.model.DeliveryRequest;
import com.keendly.model.S3Object;
import org.junit.Test;

public class HandlerTest {

    @Test
    public void test(){
        DeliveryRequest request = new DeliveryRequest();
        request.extractResult = "messages/485129e7-c5c9-4ec8-8be1-5ce6099c591b";
        request.actionLinks = "messages/GLQSVHUPVGLN.json";
        request.s3Items = new S3Object();
        request.s3Items.key = "messages/1c41cfa2497b427f975d85b40d26b637.json";
        request.s3Items.bucket = "keendly";

        new Handler().handleRequest(request, null);
    }
}
