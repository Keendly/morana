package com.jindle.model;

import lombok.Builder;
import lombok.Getter;

import java.util.Date;

@Builder
@Getter
public class Article extends Numbered {

    private String author;
    private String title;
    private String content;
    private Date date;
}
