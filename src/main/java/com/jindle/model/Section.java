package com.jindle.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class Section extends Numbered {

    private String title;
    private List<Article> articles;
}
