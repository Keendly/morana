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
import com.keendly.lang.DateFormatter;
import com.keendly.model.DeliveryArticle;
import com.keendly.model.DeliveryItem;
import com.keendly.model.DeliveryRequest;
import com.keendly.model.ExtractResult;
import com.keendly.model.book.Article;
import com.keendly.model.book.Book;
import com.keendly.model.book.Section;
import com.keendly.readingtime.ReadingTimeCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Handler implements RequestHandler<DeliveryRequest, String> {

    private static final Logger LOG = LoggerFactory.getLogger(Handler.class);

    private static final String BUCKET = "keendly";
    private static final String KEY_PATTERN = "ebooks/%s/keendly.tar.gz";
    private static AmazonS3 s3 = new AmazonS3Client();
    private static ReadingTimeCalculator readingTimeCalculator = new ReadingTimeCalculator();
    private static DateFormatter dateFormatter = new DateFormatter();

    @Override
    public String handleRequest(DeliveryRequest input, Context context) {
        try {
            input = deserializeDeliveryRequest(input);
            input = deserializeExtractResult(input);
            input = deserializeActionLinks(input);

            Book book = mapToBook(input);
            String ebookDirPath = new Generator().generate(book);
            String ebookArchivePath = new Executor().compress(ebookDirPath);

            String key = String.format(KEY_PATTERN, UUID.randomUUID().toString());
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

    public static Book mapToBook(DeliveryRequest deliveryRequest){
        List<ExtractResult> articles = deliveryRequest.extractResults;
        Map<String, List<DeliveryRequest.ActionLink>> actionLinks = deliveryRequest.actionLinksContent;

        Book book = Book.builder()
            .title("Keendly Feeds")
            .creator("Keendly")
            .subject("News")
            .language("en-GB")
            .sections(new ArrayList<>())
            .date(dateFormatter.formatDate(deliveryRequest.timestamp, deliveryRequest.timezone))
            .build();

        for (DeliveryItem item : deliveryRequest.items){
            if (item.articles == null || item.articles.isEmpty()){
                continue;
            }

            Section section = Section.builder()
                .title(item.title)
                .articles(new ArrayList<>())
                .build();

            for (DeliveryArticle article : item.articles){
                Article.ArticleBuilder articleBuilder = Article.builder()
                    .id(article.id)
                    .title(article.title)
                    .author(article.author)
                    .url(article.url);
                String content;
                if (articles != null && getArticleText(article.url, articles) != null){
                    content = getArticleText(article.url, articles);
                } else {
                    content = article.content;
                }
                articleBuilder.content(content);
                articleBuilder.date(dateFormatter.formatDate(article.timestamp, content, deliveryRequest.timezone));

                if (actionLinks.containsKey(article.id)){
                    for (DeliveryRequest.ActionLink link : actionLinks.get(article.id)){
                        articleBuilder.action(link.action, link.link);
                    }
                }
                Integer readingTime = readingTimeCalculator.getReadingTime(content);
                articleBuilder.readingTime(readingTime);

                section.getArticles().add(articleBuilder.build());
            }
            book.getSections().add(section);
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

    private DeliveryRequest deserializeActionLinks(DeliveryRequest request) throws IOException {
        if (request.actionLinks != null){
            ObjectMapper mapper = new ObjectMapper();
            GetObjectRequest getObjectRequest = new GetObjectRequest(BUCKET, request.actionLinks);
            com.amazonaws.services.s3.model.S3Object object = s3.getObject(getObjectRequest);

            Map<String, List<DeliveryRequest.ActionLink>> items = mapper
                .readValue(IOUtils.toString(object.getObjectContent()).getBytes("UTF8"),
                    new TypeReference<Map<String, List<DeliveryRequest.ActionLink>>>(){});

            request.actionLinksContent = items;
        } else {
            request.actionLinksContent = Collections.emptyMap();
        }
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
