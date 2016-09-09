package com.keendly.images;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import com.ning.http.client.AsyncHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ImageExtractorTest {

    private static final String TMP = "/tmp";

    private ImageExtractor imageExtractor;
    private AsyncHttpClient httpClient;
    private String calledURL;

    @Before
    public void setUp() {
        httpClient = new AsyncHttpClient() {

            @Override
            public BoundRequestBuilder prepareGet(String url) {
                calledURL = url;
                return super.prepareGet(url);
            }
        };
        imageExtractor = new ImageExtractor(httpClient);
    }

    @Test
    public void testExtract_correctHTML_noImages() {
        String HTML = "<p>text</p>";

        // given
        Document article = Jsoup.parse(HTML);

        // when
        imageExtractor.extractImages(article, null, TMP);

        // then
        assertEquals(HTML, article.body().html());
        assertNull(calledURL);
    }

    @Test
    public void testExtract_correctHTML_wrongSrcAttribute() {
        String HTML = "<p>text<img src=\"blab\\la\"></p>";

        // given
        Document article = Jsoup.parse(HTML);

        // when
        imageExtractor.extractImages(article, null, TMP);

        // then
        assertNull(calledURL);
        assertEquals("<p>text</p>", article.body().html());
    }

    @Test
    public void testExtract_notHTML() {
        String HTML = "blabla";

        // given
        Document article = Jsoup.parse(HTML);

        // when
        imageExtractor.extractImages(article, null, TMP);

        // then
        assertEquals(HTML, article.body().html());
        assertNull(calledURL);
    }

    @Test
    public void testExtract_absoluteURL() {
        String URL = "http://media02.hongkiat.com/translate-wordpress-themes/translate-wordpress-core.jpg";
        String HTML = "<p>Allowing clients to manage their websites in their native languages is an important aspect of <a href=\"http://www.hongkiat.com/blog/accessibility-design-needs/\" target=\"_blank\">accessibility</a>. If you develop a WordPress site that may have <strong>users from non-English speaking countries</strong>, it can be necessary to translate the theme.</p><p>Localizing the theme doesn’t mean you translate the content on the frontend such as posts and pages; instead, it refers to the <strong>theme-related content</strong> in the admin area: the theme’s description, options, and the customizer.</p><img src=\"http://media02.hongkiat.com/translate-wordpress-themes/translate-wordpress-core.jpg\">";

        // given
        Document article = Jsoup.parse(HTML);

        // when
        imageExtractor.extractImages(article, null, TMP);

        // then
        assertEquals(URL, calledURL);
        assertThat(article.select("p").size(), is(2));
        assertNotEquals(article.select("img").attr("src"), URL);
    }

    @Test
    public void testExtract_relativeURL() {
        String IMAGE_URL = "translate-wordpress-core.jpg";
        String HTML = "<p>text<img src=\"" + IMAGE_URL + "\"></p>";
        String ARTICLE_URL = "http://media02.hongkiat.com/translate-wordpress-themes/";

        // given
        Document article = Jsoup.parse(HTML);

        // when
        imageExtractor.extractImages(article, ARTICLE_URL, TMP);

        // then
        assertEquals(ARTICLE_URL + IMAGE_URL, calledURL);
        assertThat(article.select("p").size(), is(1));
    }

    @Test
    public void testExtract_tooManyImages() {
        int MAX_IMAGES = 1;
        String FIRST_IMAGE = "http://blabla.jpg";
        String SECOND_IMAGE = "http://blabla1.jpg";
        String FIRST_ARTICLE = "<p>text<img src=\"" + FIRST_IMAGE + "\"></p>";
        String SECOND_ARTICLE = "<p>text<img src=\"" + SECOND_IMAGE + "\"></p>";

        // given
        ImageExtractor.MAX_IMAGES = MAX_IMAGES;
        Document firstArticle = Jsoup.parse(FIRST_ARTICLE);
        Document secondArticle = Jsoup.parse(SECOND_ARTICLE);

        // when
        imageExtractor.extractImages(firstArticle, null, TMP);
        imageExtractor.extractImages(secondArticle, null, TMP);

        // then
        assertEquals(FIRST_IMAGE, calledURL);
    }

    @Test
    @Ignore
    public void testExtract_imageNotFound(){
        // TODO, check that we remove element on 404
    }

    @Test
    @Ignore
    public void testExtract_svgImage(){
        // TODO, check that no compression is done on svg
    }
}
