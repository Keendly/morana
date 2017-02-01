package com.keendly.model.book;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
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
