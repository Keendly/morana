package com.keendly.model.book;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Section extends Numbered {

    public String title;
    public List<Article> articles = new ArrayList<>();
}
