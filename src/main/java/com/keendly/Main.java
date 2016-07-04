package com.keendly;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.SignalWorkflowExecutionRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keendly.model.Article;
import com.keendly.model.Book;
import com.keendly.model.Section;
import com.keendly.schema.GenerateProtos;
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final String QUEUE_URL = "https://sqs.eu-west-1.amazonaws.com/625416862388/generation-queue";
    private static final int QUEUE_POLL_INTERVAL = 30; // seconds
    private static final String BUCKET = "keendly";

    private static AmazonS3 amazonS3Client;
    private static AmazonSQS amazonSQSClient;
    private static AmazonSimpleWorkflow amazonSWFClient;

    private static String kindleGenPath;
    private static String credentialsProfile;

    public static void main(String[] args){
        kindleGenPath = args[0];
        if (args.length > 1){
            credentialsProfile = args[1];
        }
        initClients();

        while (true){
            LOG.debug("Polling for messages...");
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(QUEUE_URL).withMaxNumberOfMessages(2);
            try {
                List<Message> messages = amazonSQSClient.receiveMessage(receiveMessageRequest).getMessages();
                if (!messages.isEmpty()){
                    LOG.info("Got {} messages from queue", messages.size());
                    for (Message message : messages){
                        MDC.put("messageId", message.getMessageId());
                        try {
                            if (!message.getAttributes().containsKey("workflowId")) {
                                // old way
                                GenerateMessage generateMessage = new ObjectMapper()
                                    .readValue(message.getBody(), GenerateMessage.class);
                                try {
                                    LOG.debug("Deserialized message: {}", message.getBody());
                                    Book book = fetchBookMetadata(generateMessage);
                                    LOG.debug("Ebook metadata extracted");
                                    String ebookPath = new Generator("/tmp", kindleGenPath).generate(book);
                                    LOG.debug("Ebook generated in {}", ebookPath);
                                    String ebookKey = extractDir(generateMessage.key) + "/keendly.mobi";
                                    storeEbookToS3(generateMessage.bucket, ebookKey, ebookPath);
                                    storeGenerationSuccessResponse(generateMessage.bucket, ebookKey,
                                        extractDir(generateMessage.key) + "/generate_ebook.res");
                                    LOG.info("Processing finished");
                                } catch (Exception e) {
                                    String ebookDir = extractDir(generateMessage.key);
                                    storeGenerationFailResponse(generateMessage.bucket, ebookDir + "/generate_ebook.res", e.getMessage());
                                    throw e;
                                }
                            } else {
                                // SWF workflow
                                try {
                                    Book book = new ObjectMapper().readValue(message.getBody(), Book.class);
                                    String ebookPath = new Generator("/tmp", kindleGenPath).generate(book);
                                    String key = "ebooks/" + UUID.randomUUID().toString();

                                    storeEbookToS3(BUCKET, key, ebookPath);
                                    signalWorkflow(message.getAttributes().get("workflowId"),
                                        message.getAttributes().get("runId"), "generationFinished", key);

                                } catch (Exception e) {
                                    signalWorkflow(message.getAttributes().get("workflowId"),
                                        message.getAttributes().get("runId"), "generationFinished",
                                        "ERROR:" + e.getMessage());
                                    throw e;
                                }
                            }
                        } catch (Exception e){
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

    private static void initClients(){
        AmazonSQS sqsClient = null;
        if (credentialsProfile != null){
            LOG.info("Initiating AWS clients with profile: {}", credentialsProfile);
            ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider(credentialsProfile);
            amazonS3Client = new AmazonS3Client(credentialsProvider);
            amazonSWFClient = new AmazonSimpleWorkflowClient(credentialsProvider);
            sqsClient = new AmazonSQSClient(credentialsProvider);
        } else {
            amazonS3Client = new AmazonS3Client();
            amazonSWFClient = new AmazonSimpleWorkflowClient();
            sqsClient = new AmazonSQSClient();
        }
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
            .withLargePayloadSupportEnabled(amazonS3Client, BUCKET);
        amazonSQSClient = new AmazonSQSExtendedClient(sqsClient, extendedClientConfiguration);

        amazonSWFClient.setRegion(Region.getRegion(Regions.EU_WEST_1));

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
        builder.setSuccess(false);
        builder.setErrorDescription(error);
        File f = new File("/tmp/" + UUID.randomUUID().toString());
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        builder.build().writeTo(fos);
        PutObjectResult result = amazonS3Client.putObject(new PutObjectRequest(bucket, responseKey, f));
        LOG.debug("Response message in S3, key: {}, etag: {}", responseKey, result.getETag());
    }

    private static void signalWorkflow(String workflowId, String runId, String signal, String input){
        SignalWorkflowExecutionRequest signalRequest = new SignalWorkflowExecutionRequest();
        signalRequest.setDomain("keendly");
        signalRequest.setInput(input);
        signalRequest.setRunId(runId);
        signalRequest.setWorkflowId(workflowId);
        signalRequest.setSignalName(signal);
        amazonSWFClient.signalWorkflowExecution(signalRequest);
    }

    private static String extractDir(String key){
        return key.substring(0, key.lastIndexOf("/"));
    }

    static class GenerateMessage {
        public String bucket;
        public String key;


    }
}
