package com.keendly;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keendly.cover.CoverCreator;
import com.keendly.images.ImageExtractor;
import com.keendly.kindlegen.Executor;
import com.keendly.kindlegen.exception.KindleGenException;
import com.keendly.kindlegen.exception.TimeoutException;
import com.keendly.model.Article;
import com.keendly.model.Book;
import com.keendly.model.Section;
import com.keendly.template.Processor;
import com.keendly.utils.BookUtils;
import com.keendly.schema.GenerateProtos;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Generator {

    private static final Logger LOG = LoggerFactory.getLogger(Generator.class);
    private static final String SECTIONS_DIR = "sections";
    private static final String OPF_FILE_NAME = "jindle.opf";

    private String tempDirectory;

    private static Processor templateProcessor = new Processor();
    private static CoverCreator coverCreator = new CoverCreator();

    private ImageExtractor imageExtractor = new ImageExtractor();

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
            LOG.error(e.getOutput());
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
        FileUtils.writeStringToFile(new File(filePath), content, "UTF-8");
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

    private static String kindleGenPath;

    public static void main(String[] args){
        kindleGenPath = args[0];

        while (true){
            LOG.debug("Polling for messages...");
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(QUEUE_URL).withMaxNumberOfMessages(2);
            try {
                List<Message> messages = amazonSQSClient.receiveMessage(receiveMessageRequest).getMessages();
                if (!messages.isEmpty()){
                    LOG.info("Got {} messages from queue", messages.size());
                    for (Message message : messages){
                        GenerateMessage generateMessage = deserializeMessage(message);
                        LOG.debug("Deserialized message: {}", message.getBody());
                        try {
                            MDC.put("messageId", message.getMessageId());
                            LOG.info("Processing started");
                            processMessage(generateMessage);
                            LOG.info("Processing finished");
                        } catch (Exception e){
                            String ebookDir = extractDir(generateMessage.key);
                            storeGenerationFailResponse(generateMessage.bucket, ebookDir  + "/generate_ebook.res", e.getMessage());
                            throw e;
                        } finally {
                            amazonSQSClient.deleteMessage(QUEUE_URL, message.getReceiptHandle());
                        }
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
            } catch (Exception e) {
                LOG.error("Unknown exception", e);
            }
        }
    }

    private static void processMessage(GenerateMessage generateMessage) throws IOException, GeneratorException {
        Book book = fetchBookMetadata(generateMessage);
        LOG.debug("Ebook metadata fetched from S3");
        String ebookPath = new Generator("/tmp", kindleGenPath).generate(book);
        LOG.debug("Ebook generated in {}", ebookPath);
        String ebookKey = extractDir(generateMessage.key) + "/keendly.mobi";
        storeEbookToS3(generateMessage.bucket, ebookKey, ebookPath);
        storeGenerationSuccessResponse(generateMessage.bucket, ebookKey, extractDir(generateMessage.key) + "/generate_ebook.res");
    }

    private static GenerateMessage deserializeMessage(Message msg) throws IOException {
        return new ObjectMapper().readValue(msg.getBody(), GenerateMessage.class);
    }

    // public for test
    public static Book fetchBookMetadata(GenerateMessage message) throws IOException {
        S3Object ebookObj = amazonS3Client.getObject(new GetObjectRequest(message.bucket, message.key));
        GenerateProtos.GenerateEbookRequest req =
            GenerateProtos.GenerateEbookRequest.parseFrom(ebookObj.getObjectContent());

        return map(req);
    }

    private static Book map(GenerateProtos.GenerateEbookRequest req){
        Book book = new Book();
        book.setTitle(req.getTitle());
        book.setCreator(req.getCreator());
        book.setDate(req.getDate());
        book.setDescription(req.getDescription());
        book.setLanguage(req.getLanguage());
        book.setPublisher(req.getPublisher());
        book.setSubject(req.getSubject());
        List<Section> sections = new ArrayList<>();
        for (GenerateProtos.GenerateEbookRequest.Section section : req.getSectionsList()){
            Section s = new Section();
            s.setTitle(section.getTitle());
            List<Article> articles = new ArrayList<>();
            for (GenerateProtos.GenerateEbookRequest.Section.Article article : section.getArticlesList()){
                Article a = new Article();
                a.setAuthor(article.getAuthor());
                a.setContent(article.getContent());
                a.setDate(new Date(article.getDate()));
                a.setTitle(article.getTitle());
                a.setUrl(article.getUrl());
                articles.add(a);
            }
            s.setArticles(articles);
            sections.add(s);
        }
        book.setSections(sections);
        return book;
    }

    private static void storeEbookToS3(String bucket, String key, String filePath){
        PutObjectResult result = amazonS3Client.putObject(new PutObjectRequest(bucket, key, new File(filePath)));
        LOG.debug("Ebook stored in S3, key: {}, etag: {}", key, result.getETag());
    }

    private static void storeGenerationSuccessResponse(String bucket, String ebookKey, String responseKey) throws IOException {
        GenerateProtos.GenerateEbookResponse.Builder builder = GenerateProtos.GenerateEbookResponse.newBuilder();
        GenerateProtos.GenerateEbookResponse.File.Builder fileBuilder =
            GenerateProtos.GenerateEbookResponse.File.newBuilder();
        fileBuilder.setBucket(bucket);
        fileBuilder.setKey(ebookKey);
        builder.setPath(fileBuilder.build());
        builder.setSuccess(true);
        File f = new File("/tmp/" + UUID.randomUUID().toString());
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        builder.build().writeTo(fos);
        PutObjectResult result = amazonS3Client.putObject(new PutObjectRequest(bucket, responseKey, f));
        LOG.debug("Response message in S3, key: {}, etag: {}", responseKey, result.getETag());
    }

    private static void storeGenerationFailResponse(String bucket, String responseKey, String error) throws IOException {
        GenerateProtos.GenerateEbookResponse.Builder builder = GenerateProtos.GenerateEbookResponse.newBuilder();
        GenerateProtos.GenerateEbookResponse.File.Builder fileBuilder =
            GenerateProtos.GenerateEbookResponse.File.newBuilder();
        builder.setSuccess(false);
        builder.setErrorDescription(error);
        File f = new File("/tmp/" + UUID.randomUUID().toString());
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        builder.build().writeTo(fos);
        PutObjectResult result = amazonS3Client.putObject(new PutObjectRequest(bucket, responseKey, f));
        LOG.debug("Response message in S3, key: {}, etag: {}", responseKey, result.getETag());
    }

    private static String extractDir(String key){
        return key.substring(0, key.lastIndexOf("/"));
    }

    static class GenerateMessage {
        public String bucket;
        public String key;
    }
}

