package com.keendly.model.book;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Article extends Numbered {

    private String id;
    private String url;
    private String author;
    private String title;
    private String content;
    private String date;
    @Singular private Map<String, String> actions = new HashMap<>();
    private Integer readingTime;
    private String qrCode;
    private String snippet;



    public static String extractSnippet(String content) {
        if (content == null) {
            return null;
        }
        // this text from add will sometimes make it through content extraction but we don't want it in snippet
        content = content.replace("Ads from Inoreader • Remove", "").trim();
        if (content.isEmpty()) {
            return null;
        }
        String snippet;
        if (content.length() >= 400) {
            snippet = content.substring(0, 400) + "...";
        } else {
            snippet = content;
        }
        return snippet.trim();
    }
}
