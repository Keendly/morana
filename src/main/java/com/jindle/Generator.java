package com.jindle;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
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
            LOG.error("Error calling kindlegen, exit value: " + e.getExitValue(), e);
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

    private static final String QUEUE_URL = "https://sqs.eu-west-1.amazonaws.com/625416862388/generation-queue";
    private static final int QUEUE_POLL_INTERVAL = 30; // seconds

    private static AmazonS3Client amazonS3Client = new AmazonS3Client();
    private static AmazonSQSClient amazonSQSClient = new AmazonSQSClient();

    private static Generator generator;

    public static void main(String[] args){
        String kindleGenPath = args[0];
        generator = new Generator("/tmp", kindleGenPath);

        while (true){
            LOG.debug("Polling for messages...");
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(QUEUE_URL).withMaxNumberOfMessages(2);
            List<Message> messages = amazonSQSClient.receiveMessage(receiveMessageRequest).getMessages();
            try {
                if (!messages.isEmpty()){
                    LOG.info("Got {} messages from queue", messages.size());
                    for (Message message : messages){
                        MDC.put("messageId", message.getMessageId());
                        LOG.info("Processing started");
                        processMessage(message);
                        amazonSQSClient.deleteMessage(QUEUE_URL, message.getReceiptHandle());
                        LOG.info("Processing finished");
                    }
                    MDC.clear();
                } else {
                    LOG.debug("No messages received from queue, going to sleep for {} seconds", QUEUE_POLL_INTERVAL);
                        Thread.sleep(QUEUE_POLL_INTERVAL * 1000);
                }
            } catch (InterruptedException e){
                LOG.error("AAAAA", e);
            } catch (GeneratorException e) {
                LOG.error("Exception generating ebook", e);
            } catch (IOException e) {
                LOG.error("Exception during deserializaton", e);
            }
        }
    }

    private static void processMessage(Message msg) throws IOException, GeneratorException {
        GenerateMessage generateMessage = deserializeMessage(msg);
        LOG.debug("Deserialized message: {}", msg.getBody());
        Book book = fetchBookMetadata(generateMessage);
        LOG.debug("Ebook metadata fetched from S3");
        String ebookPath = generator.generate(book);
        LOG.debug("Ebook generated in {}", ebookPath);
        storeEbookToS3(generateMessage.bucket, extractFileName(generateMessage.key) + ".mobi", ebookPath);
    }

    private static GenerateMessage deserializeMessage(Message msg) throws IOException {
        return new ObjectMapper().readValue(msg.getBody(), GenerateMessage.class);
    }

    private static Book fetchBookMetadata(GenerateMessage message) throws IOException {
        S3Object ebookObj = amazonS3Client.getObject(new GetObjectRequest(message.bucket, message.key));
        return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(ebookObj.getObjectContent(), Book.class);
    }

    private static void storeEbookToS3(String bucket, String key, String filePath){
        PutObjectResult result = amazonS3Client.putObject(new PutObjectRequest(bucket, key, new File(filePath)));
        LOG.debug("File stored in S3, key: {}, etag: {}", key, result.getETag());
    }

    private static String extractFileName(String key){
        return key.substring(0, key.lastIndexOf("."));
    }

    static class GenerateMessage {
        public String bucket;
        public String key;
    }
}

