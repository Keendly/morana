package com.keendly.model.book;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Article extends Numbered {

    private String id;
    private String url;
    private String author;
    private String title;
    private String content;
    private String date;
    @Singular private Map<String, String> actions = new HashMap<>();
    private Integer readingTime;
    private String qrCode;
}
