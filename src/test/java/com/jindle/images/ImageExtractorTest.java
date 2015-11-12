package com.jindle.images;

import static org.junit.Assert.*;

import com.jindle.model.Article;
import com.ning.http.client.AsyncHttpClient;
import org.junit.Before;
import org.junit.Test;

public class ImageExtractorTest {

    private static final String TMP = "/tmp";

    private ImageExtractor imageExtractor;
    private AsyncHttpClient httpClient;
    private String calledURL;

    @Before
    public void setUp(){
        httpClient = new AsyncHttpClient(){

            @Override
            public BoundRequestBuilder prepareGet(String url){
                calledURL = url;
                return super.prepareGet(url);
            }
        };
        imageExtractor = new ImageExtractor(httpClient);
    }

    @Test
    public void testExtract_correctHTML_noImages(){
        String HTML = "<p>text</p>";

        // given
        Article article = Article.builder().content(HTML).build();

        // when
        imageExtractor.extractImages(article, TMP);

        // then
        assertEquals(HTML, article.getContent());
        assertNull(calledURL);
    }

    @Test
    public void testExtract_correctHTML_wrongSrcAttribute(){
        String HTML = "<p>text<img src=\"blab\\la\"></p>";

        // given
        Article article = Article.builder().content(HTML).build();

        // when
        imageExtractor.extractImages(article, TMP);

        // then
        assertEquals(HTML, article.getContent());
        assertNull(calledURL);
    }

    @Test
    public void testExtract_notHTML(){
        String HTML = "blabla";

        // given
        Article article = Article.builder().content(HTML).build();

        // when
        imageExtractor.extractImages(article, TMP);

        // then
        assertEquals(HTML, article.getContent());
        assertNull(calledURL);
    }

    @Test
    public void testExtract_absoluteURL(){
        String URL = "http://media02.hongkiat.com/translate-wordpress-themes/translate-wordpress-core.jpg";
        String HTML = "<p>text<img src=\"" + URL + "\"></p>";

        // given
        Article article = Article.builder().content(HTML).build();

        // when
        imageExtractor.extractImages(article, TMP);

        // then
        assertEquals(URL, calledURL);
        assertFalse(HTML.equals(article.getContent()));
    }

    @Test
    public void testExtract_relativeURL(){
        String IMAGE_URL = "translate-wordpress-core.jpg";
        String HTML = "<p>text<img src=\"" + IMAGE_URL + "\"></p>";
        String ARTICLE_URL = "http://media02.hongkiat.com/translate-wordpress-themes/";

        // given
        Article article = Article.builder().content(HTML).url(ARTICLE_URL).build();

        // when
        imageExtractor.extractImages(article, TMP);

        // then
        assertEquals(ARTICLE_URL + IMAGE_URL, calledURL);
        assertFalse(HTML.equals(article.getContent()));
    }
}
