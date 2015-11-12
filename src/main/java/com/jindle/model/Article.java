package com.jindle.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Builder
@Getter
public class Article extends Numbered {

    private String url;
    private String author;
    private String title;
    @Setter
    private String content;
    private Date date;
}
