package com.jindle;

import com.jindle.cover.CoverCreator;
import com.jindle.images.ImageExtractor;
import com.jindle.kindlegen.Executor;
import com.jindle.kindlegen.exception.KindleGenException;
import com.jindle.kindlegen.exception.TimeoutException;
import com.jindle.model.Article;
import com.jindle.model.Book;
import com.jindle.model.Section;
import com.jindle.template.Processor;
import com.jindle.utils.BookUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class Generator {

    private static final Logger LOG = LoggerFactory.getLogger(Generator.class);
    private static final String SECTIONS_DIR = "sections";
    private static final String OPF_FILE_NAME = "jindle.opf";

    private String tempDirectory;
    private String kindleGenPath;

    private static Processor templateProcessor = new Processor();
    private static ImageExtractor imageExtractor = new ImageExtractor();
    private static CoverCreator coverCreator = new CoverCreator();

    public Generator(String tempDirectory, String kindleGenPath){
        this.tempDirectory = tempDirectory;
        this.kindleGenPath = kindleGenPath;
    }

    public String generate(Book book) throws GeneratorException {
        BookUtils.setNumbers(book);
        String bookDirectory = UUID.randomUUID().toString();

        try {
            createBookDirectory(bookDirectory);
            saveCover(book, bookDirectory);
            saveDetailsFile(book, bookDirectory);
            saveContentsHTML(book, bookDirectory);
            saveContentsNCX(book, bookDirectory);
            for (Section section : book.getSections()){
                saveSection(section, bookDirectory);
                for (Article article : section.getArticles()){
                    saveArticle(section, article, bookDirectory);
                }
            }

            return generateMobi(bookDirectory);
        } catch (IOException | InterruptedException e) {
            LOG.error("Couldn't generate ebook", e);
            throw new GeneratorException(e);
        } catch (KindleGenException e){
            LOG.error("Error calling kindlegen, exit value: " + e.getExiteValue(), e);
            LOG.debug(e.getOutput());
            throw new GeneratorException(e);
        } catch (TimeoutException e) {
            LOG.error("Timeout calling kindlegen", e);
            throw new GeneratorException(e);
        }
    }

    private void createBookDirectory(String bookDirectory) throws IOException {
        FileUtils.forceMkdir(new File(bookDirPath(bookDirectory)));
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
        imageExtractor.extractImages(article, bookFilePath(dir,
            SECTIONS_DIR + File.separator + section.getHref()));
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
        FileUtils.writeStringToFile(new File(filePath), content);
    }

    private String generateMobi(String bookDirectory)
        throws InterruptedException, IOException, KindleGenException, TimeoutException {
        String workingDirectory = tempDirectory + File.separator + bookDirectory;
        Executor executor = new Executor(kindleGenPath, workingDirectory, OPF_FILE_NAME);
        return executor.run();
    }
}

