package com.keendly.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
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
