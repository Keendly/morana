package com.keendly;

import static java.util.Arrays.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import com.keendly.model.Article;
import com.keendly.model.Book;
import com.keendly.model.Section;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

public class GeneratorTest {

    private Generator generator = new Generator("/tmp", System.getenv().get("KINDLEGEN_PATH"));

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
    public void testGenerate_wrongKindleGenPath() throws Exception {
        thrown.expect(GeneratorException.class);
        thrown.expectCause(is(IOException.class));

        // given
        Book book = generateBook();

        // when
        new Generator("/tmp", "wrong").generate(book);
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

    @Ignore
    @Test
    public void test() throws IOException, GeneratorException {
        Generator.GenerateMessage msg = new Generator.GenerateMessage();
        msg.bucket = "keendly";
        msg.key = "deliveries/20160319000247370_bHYba/generate_ebook.req";
        Book b = Generator.fetchBookMetadata(msg);

        String a = generator.generate(b);
        System.out.println(a);
    }
}