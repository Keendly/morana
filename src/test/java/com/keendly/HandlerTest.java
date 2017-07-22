package com.keendly;

import com.keendly.model.DeliveryRequest;
import com.keendly.model.S3Object;
import org.junit.Ignore;
import org.junit.Test;

public class HandlerTest {

    @Test
    @Ignore("for debugging")
    public void test(){
        DeliveryRequest request = new DeliveryRequest();
        request.extractResult = "messages/NDPKJTQFLSFU";
        request.actionLinks = "messages/TKFSQRAVQCXM.json";
        request.s3Items = new S3Object();
        request.s3Items.key = "messages/b39b95e9fb8542ab8964424145df8600.json";
        request.s3Items.bucket = "keendly";

        new Handler().handleRequest(request, null);
    }
}
