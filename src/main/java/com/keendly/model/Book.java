package com.keendly.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Book {

    public String title;
    public String language;
    public String creator;
    public String publisher;
    public String subject;
    public String date;
    public String description;
    public List<Section> sections = new ArrayList<>();
}
