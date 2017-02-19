package com.keendly;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Preprocessor {

    private Document document;

    public Preprocessor(Document document){
        this.document = document;
    }

    public void preprocess(){
        removeUnnecessaryImgAttributes();
        removeOnclick();
        removeStyle();
    }

    private void removeUnnecessaryImgAttributes(){
        Elements elements = document.select("img");

        // remove 'alt' attributes because they sometimes cause kindlegen fail, for example:
        // alt="At a glance, <i>Pokemon Uranium</i> is pretty hard to distinguish from an official Nintendo release."

        // remove 'title' attributes for the same reason
        // title="A painting of martyr Oscar Arnulfo Romero, Metropoli    tan Cathedral, 2015</b><b>."
        for (Element element : elements){
            element.removeAttr("alt");
            element.removeAttr("title");

            // remove 'data-' attributes
            for (Attribute attribute : element.attributes()){
                if (attribute.getKey().startsWith("data-")){
                    element.removeAttr(attribute.getKey());
                }
            }
        }


    }

    private void removeOnclick(){
        Elements elements = document.select("a");
        for (Element element : elements){
            element.removeAttr("onclick");
        }
    }

    private void removeStyle(){
        for (Element element : document.getAllElements()) {
            element.removeAttr("style");
        }
    }
}
