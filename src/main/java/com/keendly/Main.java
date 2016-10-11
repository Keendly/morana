package com.keendly;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.util.json.Jackson;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keendly.model.Book;
import com.keendly.model.GenerateFinished;
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final int QUEUE_POLL_INTERVAL = 30; // seconds
    private static final String BUCKET = "keendly";

    private static AmazonS3 amazonS3Client;
    private static AmazonSQS amazonSQSClient;
    private static AmazonSNS amazonSNSClient;

    private static String kindlegenPath;

    public static void main(String[] args){
        Arguments arguments = new Arguments();
        JCommander jc = new JCommander(arguments);

        try {
            jc.parse(args);
        } catch (Exception e){
            jc.usage();
            System.exit(1);
        }

        kindlegenPath = arguments.kindlegenPath;
        initClients(arguments.profile);

        if (arguments.onlyGenerate != null){
            try {
                S3Object s3Object = amazonS3Client.getObject(BUCKET, arguments.onlyGenerate);
                Book book = new ObjectMapper().readValue(s3Object.getObjectContent(), Book.class);
                String ebookPath = new Generator("/tmp", kindlegenPath).generate(book);

                LOG.info("Generated: {}", ebookPath);
            } catch (IOException e){
                e.printStackTrace();
            } catch (GeneratorException e) {
                e.printStackTrace();
            }

            return;
        }

        while (true){
            LOG.debug("Polling for messages...");
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(arguments.queue.trim())
                .withMessageAttributeNames("All")
                .withMaxNumberOfMessages(2);
            try {
                List<Message> messages = amazonSQSClient.receiveMessage(receiveMessageRequest).getMessages();
                if (!messages.isEmpty()){
                    LOG.info("Got {} messages from queue", messages.size());
                    for (Message message : messages){
                        MDC.put("messageId", message.getMessageId());
                        try {
                            try {
                                Book book = new ObjectMapper().readValue(message.getBody(), Book.class);
                                String ebookPath = new Generator("/tmp", kindlegenPath).generate(book);
                                String key = "ebooks/" + UUID.randomUUID().toString() + "/keendly.mobi";
                                storeEbookToS3(BUCKET, key, ebookPath);
                                publishSuccess(key, arguments.topic, message);
                            } catch (Exception e) {
                                LOG.error("Error during SWF execution", e);
                                publishError(e, arguments.topic, message);
                                throw e;
                            }
                        } catch (Exception e){
                            throw e;
                        } finally {
                            amazonSQSClient.deleteMessage(arguments.queue, message.getReceiptHandle());
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

    private static void initClients(String credentialsProfile){
        AmazonSQS sqsClient = null;
        if (credentialsProfile != null){
            LOG.info("Initiating AWS clients with profile: {}", credentialsProfile);
            ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider(credentialsProfile);
            amazonS3Client = new AmazonS3Client(credentialsProvider);
            amazonSNSClient = new AmazonSNSClient(credentialsProvider);
            sqsClient = new AmazonSQSClient(credentialsProvider);
        } else {
            amazonS3Client = new AmazonS3Client();
            amazonSNSClient = new AmazonSNSClient();
            sqsClient = new AmazonSQSClient();
        }
        amazonSNSClient.setRegion(Region.getRegion(Regions.EU_WEST_1));
        ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
            .withLargePayloadSupportEnabled(amazonS3Client, BUCKET);
        amazonSQSClient = new AmazonSQSExtendedClient(sqsClient, extendedClientConfiguration);
    }

    private static void storeEbookToS3(String bucket, String key, String filePath){
        PutObjectResult result = amazonS3Client.putObject(new PutObjectRequest(bucket, key, new File(filePath)));
        LOG.debug("Ebook stored in S3, key: {}, etag: {} ", key, result.getETag());
    }

    private static void publishSuccess(String key, String topic, Message message){
        GenerateFinished msg = new GenerateFinished();
        msg.key = key;
        msg.success = true;

        PublishRequest publishRequest = new PublishRequest();
        publishRequest.setMessageAttributes(copyAttributes(message.getMessageAttributes()));
        publishRequest.setMessage(Jackson.toJsonString(msg));
        publishRequest.setTopicArn(topic.trim());
        LOG.info("Publishing to {}, message {}", topic.trim(), Jackson.toJsonString(msg));
        amazonSNSClient.publish(publishRequest);
    }

    private static void publishError(Exception e, String topic, Message message){
        GenerateFinished msg = new GenerateFinished();
        msg.success = false;
        msg.error = e.getMessage();

        PublishRequest publishRequest = new PublishRequest();
        publishRequest.setMessageAttributes(copyAttributes(message.getMessageAttributes()));
        publishRequest.setMessage(Jackson.toJsonString(msg));
        publishRequest.setTopicArn(topic.trim());
        amazonSNSClient.publish(publishRequest);
    }

    private static Map<String, MessageAttributeValue> copyAttributes(Map<String, com.amazonaws.services.sqs.model.MessageAttributeValue> input){
        Map<String, MessageAttributeValue> res = new HashMap<>();
        for (Map.Entry<String, com.amazonaws.services.sqs.model.MessageAttributeValue> entry : input.entrySet()){
            MessageAttributeValue r = new MessageAttributeValue();
            r.setBinaryValue(entry.getValue().getBinaryValue());
            r.setStringValue(entry.getValue().getStringValue());
            r.setDataType(entry.getValue().getDataType());
            res.put(entry.getKey(), r);
        }
        return res;
    }

    private static String extractDir(String key){
        return key.substring(0, key.lastIndexOf("/"));
    }

    private static class GenerateMessage {
        public String bucket;
        public String key;
    }

    private static class Arguments {

        @Parameter(names = "--profile", description = "AWS Credentials profile")
        String profile;

        @Parameter(names = "--kindlegen", description = "Kindlegen path", required = true)
        String kindlegenPath;

        @Parameter(names = "--inboundQueue", description = "SQS queue to poll for messages")
        String queue;

        @Parameter(names = "--outboundTopic", description = "SNS topic to publish result to")
        String topic;

        @Parameter(names = "--onlyGenerate", description = "Only generate ebook, for debugging")
        String onlyGenerate;
    }
}
