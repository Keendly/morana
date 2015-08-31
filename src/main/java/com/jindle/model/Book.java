package com.jindle.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class Book {

    private String title;
    private String language;
    private String creator;
    private String publisher;
    private String subject;
    private String date;
    private String description;
    private List<Section> sections;
}
