package com.jindle.images;

import com.jindle.model.Article;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class ImageExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(ImageExtractor.class);
    private static final int REQUEST_TIMEOUT = 5 * 60 * 1000;// 5 minutes

    private AsyncHttpClient asyncHttpClient;

    public ImageExtractor(){
        this(new AsyncHttpClient());
    }

    public ImageExtractor(AsyncHttpClient httpClient){
        this.asyncHttpClient = httpClient;
    }

    public void extractImages(Article article, String directory) {
        Document document = Jsoup.parse(article.getContent());
        Elements elements = document.select("img");
        Map<AsyncHttpClient.BoundRequestBuilder, Element> requests = new HashMap<>();
        for (Element element : elements){
            try {
                String url = extractImageUrl(element, article);
                AsyncHttpClient.BoundRequestBuilder get = asyncHttpClient.prepareGet(url).setFollowRedirects(true).setRequestTimeout(REQUEST_TIMEOUT);
                requests.put(get, element);
            } catch (ImageExtractionException e){
                continue;
            }
        }
        if (requests.isEmpty()){
            return;
        }
        runRequests(directory, requests);
        article.setContent(document.body().html());
        return;
    }

    private void runRequests(final String directory, Map<AsyncHttpClient.BoundRequestBuilder, Element> requests) {
        CountDownLatch counter = new CountDownLatch(requests.size());

        for (Map.Entry<AsyncHttpClient.BoundRequestBuilder, Element> request : requests.entrySet()){
            request.getKey().execute(new AsyncCompletionHandler<Response>() {

                @Override
                public Response onCompleted(Response response) throws Exception {
                    saveImage(response, directory, request.getValue());
                    counter.countDown();
                    return response;
                }

                @Override
                public void onThrowable(Throwable t) {
                    LOG.error("Couldn't download image " + request.getKey().build().getUrl(), t);
                    counter.countDown();
                }
            });
        }
        try {
            counter.await();
        } catch (InterruptedException e) {
            LOG.error("Error waiting for requests to finish", e);
        }
    }

    private void saveImage(Response response, String directory, Element element) throws IOException {
        InputStream is = response.getResponseBodyAsStream();
        String uid = generateFileName();
        String filePath = directory + File.separator + uid;
        File f = new File(filePath);
        FileOutputStream fos = new FileOutputStream(f);
        element.attr("src", uid);
        try {
            IOUtils.copy(is, fos);
        } finally {
            is.close();
            fos.close();
        }
    }

    private String extractImageUrl(Element element, Article article) throws ImageExtractionException {
        String src = getSource(element);
        try {
            String url;
            if (isAbsolute(src)){
                url = src;
            } else {
                url = buildAbsolutePath(src, article.getUrl());
            }
            return url;
        } catch (URISyntaxException e){
            LOG.warn("Couldn't parse URL " + src, e);
            throw new ImageExtractionException();
        }
    }

    private String getSource(Element img) throws ImageExtractionException {
        String src = img.attr("src");
        if (StringUtils.isEmpty(src)){
            LOG.warn("Empty src found, nothing to do");
            throw new ImageExtractionException();
        }
        return src;
    }

    private String buildAbsolutePath(String src, String articleUrl) throws ImageExtractionException {
        String url;
        if (StringUtils.isEmpty(articleUrl)){
            LOG.warn("Couldn't build absolute URL for {}, beacuse there is no article URL provided", src);
            throw new ImageExtractionException();
        }
        try {
            url = buildAbsoluteURL(articleUrl, src);
        } catch (MalformedURLException e) {
            LOG.warn("Couldn't build absolute URL using base " + articleUrl + " and relative " + src, e);
            throw new ImageExtractionException();
        }
        return url;
    }

    private String generateFileName(){
        return UUID.randomUUID().toString().replace("-","");
    }

    private String buildAbsoluteURL(String articleUrl, String imageRelativeUrl) throws MalformedURLException {
        URL base = new URL(articleUrl);
        URL absolute = new URL(base, imageRelativeUrl);
        return absolute.toString();
    }

    private boolean isAbsolute(String url) throws URISyntaxException {
        URI u = new URI(url);
        return u.isAbsolute();
    }

    private class ImageExtractionException extends Exception {

    }
}
