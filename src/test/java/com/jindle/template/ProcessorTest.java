package com.jindle.template;

import static java.util.Arrays.*;
import static org.junit.Assert.*;
import static org.xmlunit.builder.DiffBuilder.*;

import com.jindle.utils.BookUtils;
import com.jindle.model.Article;
import com.jindle.model.Book;
import com.jindle.model.Section;
import org.junit.Test;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import java.io.InputStream;

public class ProcessorTest {

  private Processor processor = new Processor();

  @Test
  public void testDetails() {
    // given
    Book book = generateBook();

    // when
    String details = processor.details(book);

    // then
    assertTheSame(details, "/test-data/details.opf");
  }

  @Test
  public void testContentsHTML() {
    // given
    Book book = generateBook();

    // when
    String contents = processor.contentsHTML(book);

    // then
    assertTheSame(contents, "/test-data/contents.html");
  }

  @Test
  public void testContentsNCX() {
    // given
    Book book = generateBook();

    // when
    String contents = processor.contentsNCX(book);
    System.out.println(contents);

    // then
    assertTheSame(contents, "/test-data/nav-contents.ncx");
  }

  @Test
  public void testSection() {
    // given
    Section section = Section.builder()
        .title("section_title")
        .build();

    // when
    String html = processor.section(section);

    // then
    assertTheSame(html, "/test-data/section.html");
  }

  @Test
  public void testArticle() {
    // given
    Article article = Article.builder()
        .title("article_title")
        .author("Hakuna Matata")
        .content("bumbum")
        .build();

    // when
    String html = processor.article(article);

    // then
    assertTheSame(html, "/test-data/article.html");
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
                        .title("article1")
                        .author("author1")
                        .build(),
                    Article.builder()
                        .title("article2")
                        .author("author2")
                        .build()
                ))
                .build(),
            Section.builder()
                .title("section2")
                .articles(asList(
                    Article.builder()
                        .title("article3")
                        .build(),
                    Article.builder()
                        .title("article4")
                        .build()
                ))
                .build()
        )).build();

    BookUtils.setNumbers(book);

    return book;
  }

  private Input.Builder fromResource(String resource){
    InputStream s = this.getClass().getResourceAsStream(resource);
    return Input.fromStream(s);
  }

  private void assertTheSame(String actual, String expectedFile){
    Diff d = compare(fromResource(expectedFile)).
        withTest(actual).ignoreWhitespace().build();
    assertFalse(d.hasDifferences());
  }
}
