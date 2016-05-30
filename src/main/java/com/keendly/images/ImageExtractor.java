package com.keendly.images;

import com.keendly.model.Article;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(ImageExtractor.class);
    private static final int REQUEST_TIMEOUT = 60 * 1000;// 1 minute
    private static final int MAX_IMAGES = 300; // max number of images to extract

    private AsyncHttpClient asyncHttpClient;

    public ImageExtractor(){
        this(new AsyncHttpClient());
    }

    public ImageExtractor(AsyncHttpClient httpClient){
        this.asyncHttpClient = httpClient;
    }

    private AtomicInteger imagesCount = new AtomicInteger(0);

    public void extractImages(Article article, String directory) {
        Document document = Jsoup.parse(article.getContent());
        Elements elements = document.select("img");

        if (imagesCount.get() >= MAX_IMAGES){
            LOG.warn("Reached max number of images: {}, skipping", MAX_IMAGES);
            elements.remove();
            return;
        }

        Map<String, AsyncHttpClient.BoundRequestBuilder> urls = new HashMap<>();
        Map<AsyncHttpClient.BoundRequestBuilder, List<Element>> requests = new HashMap<>();
        for (Element element : elements){


            try {
                String url = extractImageUrl(element, article);
                if (urls.containsKey(url)){
                    LOG.debug("Request already exists for url: {}", url);
                    requests.get(url).add(element);
                } else {
                    LOG.debug("Downloading image: {}", url);
                    AsyncHttpClient.BoundRequestBuilder get = asyncHttpClient.prepareGet(url).setFollowRedirects(true).setRequestTimeout(REQUEST_TIMEOUT);
                    urls.put(url, get);
                    List<Element> list = new ArrayList<>();
                    list.add(element);
                    requests.put(get, list);
                    imagesCount.incrementAndGet();
                }
            } catch (ImageExtractionException e){
                LOG.warn("Couldn't extract url from {}, ignoring element", element.html());
                element.remove();
            } catch (Exception e){
                LOG.warn("Unexpected error trying to extract url from {}, ignoring element", element.html());
                element.remove();
            }
        }
        if (requests.isEmpty()){
            return;
        }
        runRequests(directory, requests);
        article.setContent(document.body().html());
        return;
    }

    private void runRequests(final String directory, Map<AsyncHttpClient.BoundRequestBuilder, List<Element>> requests) {
        CountDownLatch counter = new CountDownLatch(requests.size());

        for (Map.Entry<AsyncHttpClient.BoundRequestBuilder, List<Element>> request : requests.entrySet()){
            request.getKey().execute(new AsyncCompletionHandler<Response>() {

                @Override
                public Response onCompleted(Response response) throws Exception {
                    String uid = generateUUID();
                    String extension = ExtensionUtils.extractFileExtension(request.getKey().build(), response);
                    if (StringUtils.isEmpty(extension)){
                        LOG.warn("Empty extension for {}", request.getKey().build().getUrl());
                        counter.countDown();
                        return response;
                    }
                    String fileName = uid + extension;
                    String filePath = directory + File.separator + fileName;

                    saveImage(response.getResponseBodyAsStream(), filePath);
                    // point to downloaded image
                    for (Element element : request.getValue()){
                        element.attr("src", fileName);
                    }
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

    private void saveImage(InputStream is, String filePath) throws IOException {
        File f = new File(filePath);
        FileOutputStream fos = new FileOutputStream(f);

        try {
            byte[] compressed = new ImageCompressor().compress(is);
            new ImageResizer().resize(new ByteArrayInputStream(compressed), f);
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

    private String generateUUID(){
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
