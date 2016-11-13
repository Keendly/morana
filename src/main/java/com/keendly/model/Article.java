package com.keendly.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Article extends Numbered {

    private String id;
    private String url;
    private String author;
    private String title;
    @Setter
    private String content;
    private Date date;
}
