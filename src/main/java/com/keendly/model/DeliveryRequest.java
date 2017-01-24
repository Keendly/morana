package com.keendly.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeliveryRequest {

    public Long id;
    public Long userId;
    public Provider provider;
    public String email;
    public Long timestamp;

    public List<DeliveryItem> items;
    public S3Object s3Items;

    public String extractResult;
    public List<ExtractResult> extractResults;
}
