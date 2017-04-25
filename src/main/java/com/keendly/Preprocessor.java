package com.keendly;

import com.amazonaws.util.StringUtils;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URISyntaxException;

public class Preprocessor {

    private Document document;

    public Preprocessor(Document document){
        this.document = document;
    }

    public void preprocess(){
        removeBrokenAttributes();
        tryToAvoidSegFault();
        removeRelativeLinks();
        removeVideo();
    }

    private void tryToAvoidSegFault(){
        document.getElementsByTag("svg").remove();
        document.getElementsByTag("picture").remove();
    }

    private void removeBrokenAttributes(){
        Elements elements = document.getAllElements();

        for (Element element : elements){
            for (Attribute attribute : element.attributes()){
                // remove attributes that have < or > in value because they cause kindlegen crash
                if (attribute.getValue().contains(">") || attribute.getValue().contains("<")){
                    element.removeAttr(attribute.getKey());
                }
            }
        }
    }

    private void removeRelativeLinks(){
        for (Element element : document.getElementsByTag("a")){
            // not absolute links wont work anyway, and they may cause kindlegen failures
            if (!isAbsolute(element.attr("href"))){
                if (!StringUtils.isNullOrEmpty(element.text())){
                    // add only text
                    element.after(element.text());
                }
                element.remove();
            }
        }
    }

    private void removeVideo(){
        document.getElementsByTag("video").remove();
    }

    private boolean isAbsolute(String url) {
        URI u;
        try {
            u = new URI(url);
        } catch (URISyntaxException e) {
            return false;
        }
        return u.isAbsolute() && (u.getScheme().equals("http") || u.getScheme().equals("https"));
    }
}
