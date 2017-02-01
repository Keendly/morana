package com.keendly;

import static java.util.Arrays.*;
import static org.junit.Assert.*;

import com.keendly.model.book.Article;
import com.keendly.model.book.Book;
import com.keendly.model.book.Section;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.InputStream;

public class GeneratorTest {

    private Generator generator = new Generator();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGenerate() throws Exception {
        // given
        Book book = generateBook();

        // when
        String result = generator.generate(book);

        // then
        assertNotNull(result);
    }

    @Test
    public void testGenerate_unsupportedImgAltAttribute() throws Exception {
        // given
        Book book = bookWithContent(fromResource("/test-articles/unsupported-alt-tag.html"));

        // when
        String result = generator.generate(book);

        // then
        assertNotNull(result);
    }

    @Test
    public void testGenerate_unsupportedImgTitleAttribute() throws Exception {
        // given
        Book book = bookWithContent(fromResource("/test-articles/unsupported-title-tag.html"));

        // when
        String result = generator.generate(book);

        // then
        assertNotNull(result);
    }

    private Book bookWithContent(String content){
        return Book.builder()
            .title("TEST_BOOK")
            .language("en-gb")
            .creator("jindle")
            .publisher("jindle")
            .subject("news")
            .date("2015-01-01")
            .description("TEST")
            .sections(asList(
                Section.builder()
                    .title("section1")
                    .articles(asList(
                        Article.builder()
                            .url("http://media02.hongkiat.com/translate-wordpress-themes/")
                            .title("article1")
                            .author("author1")
                            .content(content)
                            .build()
                    ))
                    .build()
            )).build();
    }

    private Book generateBook(){
        Book book = Book.builder()
            .title("TEST_BOOK")
            .language("en-gb")
            .creator("jindle")
            .publisher("jindle")
            .subject("news")
            .date("2015-01-01")
            .description("TEST")
            .sections(asList(
                Section.builder()
                    .title("section1")
                    .articles(asList(
                        Article.builder()
                            .url("http://media02.hongkiat.com/translate-wordpress-themes/")
                            .title("article1")
                            .author("author1")
                            .content("<p>text<img src=\"translate-wordpress-core.jpg\"></p>")
                            .build(),
                        Article.builder()
                            .title("article2")
                            .author("author2")
                            .content("article2 contentntntntnntnt")
                            .build()
                    ))
                    .build(),
                Section.builder()
                    .title("section2")
                    .articles(asList(
                        Article.builder()
                            .title("article3")
                            .content("contennt3")
                            .build(),
                        Article.builder()
                            .title("article4")
                            .content("contenttt4")
                            .build()
                    ))
                    .build()
            )).build();

        return book;
    }

    private String fromResource(String resource) throws IOException {
        InputStream s = this.getClass().getResourceAsStream(resource);
        return IOUtils.toString(s);
    }
}
