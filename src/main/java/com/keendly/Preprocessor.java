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

        for (Element element : elements){

            for (Attribute attribute : element.attributes()){
                // remove attributes that have < or > in value because they cause kindlegen crash
                if (attribute.getValue().contains(">") || attribute.getValue().contains("<")){
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
