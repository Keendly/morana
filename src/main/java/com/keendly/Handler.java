package com.keendly;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keendly.kindlegen.Executor;
import com.keendly.model.Article;
import com.keendly.model.Book;
import com.keendly.model.DeliveryArticle;
import com.keendly.model.DeliveryItem;
import com.keendly.model.DeliveryRequest;
import com.keendly.model.ExtractResult;
import com.keendly.model.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Handler implements RequestHandler<DeliveryRequest, String> {

    private static final Logger LOG = LoggerFactory.getLogger(Handler.class);


    private static final String BUCKET = "keendly";
    private static AmazonS3 s3 = new AmazonS3Client();

    @Override
    public String handleRequest(DeliveryRequest input, Context context) {
        try {
            input = deserializeDeliveryRequest(input);
            input = deserializeExtractResult(input);

            Book book = mapDeliveryRequestAndExtractResultToBook(input, input.extractResults);
            String ebookDirPath = new Generator().generate(book);
            String ebookArchivePath = new Executor().compress(ebookDirPath);

            String key = "ebooks/" + UUID.randomUUID().toString() + "/keendly.tar.gz";
            storeEbookToS3(BUCKET, key, ebookArchivePath);

            return key;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void storeEbookToS3(String bucket, String key, String filePath){
        PutObjectResult result = s3.putObject(new PutObjectRequest(bucket, key, new File(filePath)));
        LOG.debug("Ebook data stored in S3, key: {}, etag: {} ", key, result.getETag());
    }

    public static Book mapDeliveryRequestAndExtractResultToBook
        (DeliveryRequest deliveryRequest, List<ExtractResult> articles){
        Book book = new Book();
        book.title = "Keendly Feeds";
        book.creator = "Keendly";
        book.subject = "News";
        book.language = "en-GB";
        for (DeliveryItem item : deliveryRequest.items){
            if (item.articles == null || item.articles.isEmpty()){
                continue;
            }
            Section section = new Section();
            section.title = item.title;

            for (DeliveryArticle article : item.articles){
                Article bookArticle = new Article();
                bookArticle.id = article.id;
                bookArticle.title = article.title;
                bookArticle.author = article.author;
                bookArticle.date  = article.timestamp != null ? new Date(article.timestamp) : null;
                bookArticle.url = article.url;
                if (articles != null && getArticleText(article.url, articles) != null){
                    bookArticle.content = getArticleText(article.url, articles);
                } else {
                    bookArticle.content = article.content;
                }
                section.articles.add(bookArticle);
            }
            book.sections.add(section);
        }
        return book;
    }

    private static String getArticleText(String url, List<ExtractResult> articles){
        for (ExtractResult result : articles){
            if (url.equals(result.url)){
                return result.text;
            }
        }
        return null;
    }

    private static DeliveryRequest deserializeExtractResult(DeliveryRequest request) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        GetObjectRequest getObjectRequest = new GetObjectRequest(BUCKET, request.extractResult);
        com.amazonaws.services.s3.model.S3Object object = s3.getObject(getObjectRequest);

        List<ExtractResult> items = mapper
            .readValue(IOUtils.toString(object.getObjectContent()).getBytes("UTF8"),
                new TypeReference<List<ExtractResult>>(){});

        request.extractResults = items;
        return request;
    }

    private static DeliveryRequest deserializeDeliveryRequest(DeliveryRequest request) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        if (request.s3Items != null){
            GetObjectRequest getObjectRequest = new GetObjectRequest(request.s3Items.bucket, request.s3Items.key);
            com.amazonaws.services.s3.model.S3Object object = s3.getObject(getObjectRequest);

            List<DeliveryItem> items = mapper
                .readValue(IOUtils.toString(object.getObjectContent()).getBytes("UTF8"),
                    new TypeReference<List<DeliveryItem>>(){});

            request.items = items;
        }

        return request;
    }
}