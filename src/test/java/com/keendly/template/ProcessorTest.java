package com.keendly.template;

import static java.util.Arrays.*;
import static org.junit.Assert.*;
import static org.xmlunit.builder.DiffBuilder.*;

import com.keendly.model.book.Article;
import com.keendly.model.book.Book;
import com.keendly.model.book.Section;
import com.keendly.utils.BookUtils;
import org.junit.Test;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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
        .content("<div>test</div>")
        .build();

    // when
    String html = processor.article(article);

    // then
    assertTheSame(html, "/test-data/article.html");
  }

  @Test
  public void testArticle_noAuthor() {
    // given
    Article article = Article.builder()
        .title("article_title")
        .content("<div>test</div>")
        .build();

    // when
    String html = processor.article(article);

    // then
    assertTheSame(html, "/test-data/article_no_author.html");
  }

  @Test
  public void testAritlce_withActions() {
    // given
    Map<String, String> actions = new HashMap<>();
    actions.put("Keep unread", "http://blabla/unread_action");
    actions.put("Save for later", "http://blabla/save_action");

    Article article = Article.builder()
        .title("article_title")
        .author("Hakuna Matata")
        .content("<div>test</div>")
        .actions(actions)
        .build();

    // when
    String html = processor.article(article);

    // then
    assertTheSame(html, "/test-data/article_actions.html");
  }

  @Test
  public void testAritlce_withActions_noAuthor() {
    // given
    Map<String, String> actions = new HashMap<>();
    actions.put("Keep unread", "http://blabla/unread_action");
    actions.put("Save for later", "http://blabla/save_action");

    Article article = Article.builder()
        .title("article_title")
        .content("<div>test</div>")
        .actions(actions)
        .build();

    // when
    String html = processor.article(article);

    // then
    assertTheSame(html, "/test-data/article_actions_no_author.html");
  }

  @Test
  public void testArticle_readingTime() {
    // given
    Article article = Article.builder()
        .title("article_title")
        .content("<div>test</div>")
        .readingTime(5)
        .build();

    // when
    String html = processor.article(article);

    // then
    assertTheSame(html, "/test-data/article_readingtime.html");
  }

  @Test
  public void testArticle_readingTime_lessThanOneMinute() {
    // given
    Article article = Article.builder()
        .title("article_title")
        .content("<div>test</div>")
        .readingTime(0)
        .build();

    // when
    String html = processor.article(article);

    // then
    assertTheSame(html, "/test-data/article_readingtime_lessthan1min.html");
  }

  @Test
  public void testArticle_readingTime_author() {
    // given
    Article article = Article.builder()
        .title("article_title")
        .content("<div>test</div>")
        .author("Hakuna Matata")
        .readingTime(5)
        .build();

    // when
    String html = processor.article(article);

    // then
    assertTheSame(html, "/test-data/article_readingtime_author.html");
  }

  @Test
  public void testAritlce_readingTime_authorAndActions() {
    // given
    Map<String, String> actions = new HashMap<>();
    actions.put("Keep unread", "http://blabla/unread_action");
    actions.put("Save for later", "http://blabla/save_action");

    Article article = Article.builder()
        .title("article_title")
        .content("<div>test</div>")
        .author("Hakuna Matata")
        .readingTime(5)
        .actions(actions)
        .build();

    // when
    String html = processor.article(article);

    // then
    assertTheSame(html, "/test-data/article_readingtime_author_actions.html");
  }

  private Book generateBook(){
    Book book = Book.builder()
        .title("TEST_BOOK")
        .language("en-gb")
        .creator("Keendly")
        .publisher("Keendly")
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
