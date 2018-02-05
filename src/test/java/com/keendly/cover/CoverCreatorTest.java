package com.keendly.cover;

import static java.util.Arrays.*;

import com.keendly.model.book.Article;
import com.keendly.model.book.Book;
import com.keendly.model.book.Section;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test cases need to be verified manually.
 */
public class CoverCreatorTest {

    @Test
    public void testCreate() throws IOException {
        // given
        Book book = generateBook();

        // when
        new CoverCreator().create(book, "/tmp/cover1.jpg");

        // then go to the file and verify if is correct :>
    }

    @Test
    public void testCreate_tooLongSectionTitle() throws IOException {
        // given
        Book book = Book.builder()
            .title("Keendly")
            .sections(asList(
                Section.builder()
                    .title("Giant Robots Smashing Into Other Giant Robots")
                    .articles(asList(
                        Article.builder()
                            .build()
                    ))
                    .build()
            )).build();

        // when
        new CoverCreator().create(book, "/tmp/cover2.jpg");

        // then go and verify whether the section title got trimmed
    }

    @Test
    public void testCreate_maxNumberOfSections() throws IOException {
        int sectionsToCreate = CoverCreator.MAX_SECTIONS;

        // given
        Book book = Book.builder()
            .title("Keendly")
            .sections(produceSections(sectionsToCreate)).build();

        // when
        new CoverCreator().create(book, "/tmp/cover3.jpg");

        // then go and verify if all sections fit and there is no ellipis
    }

    private List<Section> produceSections(int number){
        List<Section> sections = new ArrayList<>();
        for (int i = 0; i < number; i++){
            sections.add(Section.builder().title("section " + Integer.toString(i)).build());
        }
        return sections;
    }

    @Test
    public void testCreate_tooManySections() throws IOException {
        int sectionsToCreate = CoverCreator.MAX_SECTIONS + 1;

        // given
        Book book = Book.builder()
            .title("Keendly")
            .sections(produceSections(sectionsToCreate)).build();

        // when
        new CoverCreator().create(book, "/tmp/cover4.jpg");

        // then go and verify if there is ellipsis
    }

    private Book generateBook(){
        Book book = Book.builder()
            .title("Keendly")
            .date("2017-05-10")
            .sections(asList(
                Section.builder()
                    .title("¡Olé! Magazyn")
                    .articles(asList(
                        Article.builder()
                            .build(),
                        Article.builder()
                            .build()
                    ))
                    .build(),
                Section.builder()
                    .title("Giant Robots Smashing Into Other Giant Robots")
                    .articles(asList(
                        Article.builder()
                            .build()
                    ))
                    .build(),
                Section.builder()
                    .title("warszawskibiegacz.pl")
                    .articles(asList(
                        Article.builder()
                            .build(),
                        Article.builder()
                            .build(),
                        Article.builder()
                            .build(),
                        Article.builder()
                            .build()
                    ))
                    .build(),
                Section.builder()
                    .title("FCBarca")
                    .build(),
                Section.builder()
                    .title("Fluent in 3 months")
                    .build(),
                Section.builder()
                    .title("The Clean Code Blog")
                    .build(),
                Section.builder()
                    .title("trybunazet")
                    .articles(asList(
                        Article.builder()
                            .build(),
                        Article.builder()
                            .build(),
                        Article.builder()
                            .build(),
                        Article.builder()
                            .build()
                    ))
                    .build(),
                Section.builder()
                    .title("rudzki")
                    .build(),
                Section.builder()
                    .title("michalpol")
                    .build(),
                Section.builder()
                    .title("CANAL+")
                    .build(),
                Section.builder()
                    .title("Redbooth Engineering")
                    .build(),
                Section.builder()
                    .title("AntyWeb")
                    .build(),
                Section.builder()
                    .title("Sport")
                    .build(),
                Section.builder()
                    .title("Hongkiat.com")
                    .build(),
                Section.builder()
                    .title("Świat Czytników")
                    .build(),
                Section.builder()
                    .title("jestem.mobi")
                    .build()
            )).build();
        return book;
    }
}
