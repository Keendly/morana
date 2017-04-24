package com.keendly;

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
            if (!isAbsolute(element.attr("href"))){
                element.removeAttr("href");
            }
        }
    }

    private boolean isAbsolute(String url) {
        URI u;
        try {
            u = new URI(url);
        } catch (URISyntaxException e) {
            return false;
        }
        return u.isAbsolute();
    }
}
