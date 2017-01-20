package com.keendly.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Section extends Numbered {

    public String title;
    public List<Article> articles = new ArrayList<>();
}
