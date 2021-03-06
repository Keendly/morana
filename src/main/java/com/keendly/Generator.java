package com.keendly;

import com.keendly.cover.CoverCreator;
import com.keendly.images.ImageExtractor;
import com.keendly.model.book.Article;
import com.keendly.model.book.Book;
import com.keendly.model.book.Section;
import com.keendly.qr.QrCodeGenerator;
import com.keendly.template.Processor;
import com.keendly.utils.BookUtils;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

public class Generator {

    private static final Logger LOG = LoggerFactory.getLogger(Generator.class);
    private static final String SECTIONS_DIR = "sections";
    private static final String OPF_FILE_NAME = "keendly.opf";

    private String tempDirectory = "/tmp";

    private static Processor templateProcessor = new Processor();
    private static CoverCreator coverCreator = new CoverCreator();
    private static QrCodeGenerator qrCodeGenerator = new QrCodeGenerator();

    // instance field to keep track number of images per ebook
    private ImageExtractor imageExtractor = new ImageExtractor();

    public String generate(Book book) throws IOException, URISyntaxException {

        BookUtils.setNumbers(book);
        String bookDirectory = UUID.randomUUID().toString();

        createBookDirectory(bookDirectory);
        copyMasthead(bookDirectory);
        saveCover(book, bookDirectory);
        saveDetailsFile(book, bookDirectory);
        saveContentsHTML(book, bookDirectory);
        for (Section section : book.getSections()){
            saveSection(section, bookDirectory);
            for (Article article : section.getArticles()){
                saveArticle(section, article, bookDirectory);
            }
        }
        saveContentsNCX(book, bookDirectory);
        imageExtractor.close();

        return bookDirectory;
    }

    private void createBookDirectory(String bookDirectory) throws IOException {
        FileUtils.forceMkdir(new File(bookDirPath(bookDirectory)));
    }

    private void copyMasthead(String bookDirectory) throws URISyntaxException, IOException {
        File masthead = new File(getClass().getClassLoader().getResource("images/masthead.gif").toURI());
        FileUtils.copyFileToDirectory(masthead, new File(bookDirPath(bookDirectory)));
    }

    private void saveCover(Book book, String dir) throws IOException {
        coverCreator.create(book, bookFilePath(dir, "cover.jpg"));
    }

    private void saveDetailsFile(Book book, String dir) throws IOException {
        String content = templateProcessor.details(book);
        String filePath = bookFilePath(dir, OPF_FILE_NAME);
        saveToFile(filePath, content);
    }

    private void saveContentsHTML(Book book, String dir) throws IOException {
        String content = templateProcessor.contentsHTML(book);
        String filePath = bookFilePath(dir, "contents.html");
        saveToFile(filePath, content);
    }

    private void saveContentsNCX(Book book, String dir) throws IOException {
        String content = templateProcessor.contentsNCX(book);
        String filePath = bookFilePath(dir, "nav-contents.ncx");
        saveToFile(filePath, content);
    }

    private void saveSection(Section section, String dir) throws IOException {
        String content = templateProcessor.section(section);
        String filePath = sectionFilePath(dir, section, "section.html");
        saveToFile(filePath, content);
    }

    private void saveArticle(Section section, Article article, String dir) throws IOException {
        try {
            Document document = Jsoup.parse(article.getContent());
            Preprocessor preprocessor = new Preprocessor(document);
            preprocessor.preprocess();

            String sectionPath = bookFilePath(dir, SECTIONS_DIR + File.separator + section.getHref());
            imageExtractor.extractImages(document, article.getUrl(), sectionPath);

            article.setQrCode(qrCodeGenerator.generate(sectionPath, article.getUrl()));
            article.setContent(document.body().html());
            article.setSnippet(Article.extractSnippet(document.body().text()));
        } catch (Exception e){
            LOG.error("Error processing article", e);
        }

        String content = templateProcessor.article(article);
        String filePath = sectionFilePath(dir, section, article.getHref() + ".html");
        saveToFile(filePath, content);
    }

    private String bookDirPath(String bookDir){
        return tempDirectory + File.separator + bookDir;
    }

    private String bookFilePath(String bookDir, String fileName){
        return bookDirPath(bookDir) + File.separator + fileName;
    }

    private String sectionFilePath(String bookDir, Section section, String fileName){
        return bookFilePath(bookDir,
            SECTIONS_DIR + File.separator + section.getHref() + File.separator + fileName);
    }

    private void saveToFile(String filePath, String content) throws IOException {
        FileUtils.writeStringToFile(new File(filePath), content, "UTF-8");
    }
}

