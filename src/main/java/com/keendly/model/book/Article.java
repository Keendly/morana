package com.keendly.model.book;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
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
    private Date date;
    private Map<String, String> actions = new HashMap<>();
}