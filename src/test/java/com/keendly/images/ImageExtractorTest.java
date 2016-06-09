package com.keendly.images;

import static org.junit.Assert.*;

import com.keendly.model.Article;
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
        String HTML = "<p>Allowing clients to manage their websites in their native languages is an important aspect of <a href=\"http://www.hongkiat.com/blog/accessibility-design-needs/\" target=\"_blank\">accessibility</a>. If you develop a WordPress site that may have <strong>users from non-English speaking countries</strong>, it can be necessary to translate the theme.</p><p>Localizing the theme doesn’t mean you translate the content on the frontend such as posts and pages; instead, it refers to the <strong>theme-related content</strong> in the admin area: the theme’s description, options, and the customizer.</p><img src=\"http://media02.hongkiat.com/translate-wordpress-themes/translate-wordpress-core.jpg\">";
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

    @Test
    public void testExtract_tooManyImages(){
        int MAX_IMAGES = 1;
        String FIRST_IMAGE = "http://blabla.jpg";
        String SECOND_IMAGE = "http://blabla1.jpg";
        String FIRST_ARTICLE = "<p>text<img src=\"" + FIRST_IMAGE + "\"></p>";
        String SECOND_ARTICLE = "<p>text<img src=\"" + SECOND_IMAGE + "\"></p>";

        // given
        ImageExtractor.MAX_IMAGES = MAX_IMAGES;
        Article firstArticle = Article.builder().content(FIRST_ARTICLE).build();
        Article secondArticle = Article.builder().content(SECOND_ARTICLE).build();

        // when
        imageExtractor.extractImages(firstArticle, TMP);
        imageExtractor.extractImages(secondArticle, TMP);

        // then
        assertEquals(FIRST_IMAGE, calledURL);
    }
}
