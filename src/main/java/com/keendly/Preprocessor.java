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
        removeBrokenAttributes();
        remvoeSVGs();
    }

    private void remvoeSVGs(){
        Elements elements = document.getElementsByTag("svg");
        elements.remove();
//        for (Element element : elements){
//            Elements children = element.getAllElements();
//            for (Element child : children){
//                if (child.tagName().equals("use")){
//                    child.remove();
//                }
//
//                for (Attribute attribute : child.attributes()){
//                    if (attribute.getKey().equals("xmlns")){
//                        element.removeAttr(attribute.getKey());
//                    }
//                }
//            }
//        }
    }

    private void removeBrokenAttributes(){
        Elements elements = document.getAllElements();

        for (Element element : elements){
            if (element.tagName().equals("picture")){
                element.remove();
            }

            for (Attribute attribute : element.attributes()){
                // remove attributes that have < or > in value because they cause kindlegen crash
                if (attribute.getValue().contains(">") || attribute.getValue().contains("<")){
                    element.removeAttr(attribute.getKey());
                }
            }
        }
    }
}
