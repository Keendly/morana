package com.keendly.model;

import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
public class Article extends Numbered {

    public String id;
    public String url;
    public String author;
    public String title;
    public String content;
    public Date date;
    public Map<String, String> actions = new HashMap<>();
}
