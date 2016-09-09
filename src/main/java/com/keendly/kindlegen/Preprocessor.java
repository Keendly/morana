package com.keendly.kindlegen;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Preprocessor {

    private Document document;

    public Preprocessor(Document document){
        this.document = document;
    }

    public void preprocess(){
        removeImgAltAttributes();
    }

    private void removeImgAltAttributes(){
        Elements elements = document.select("img");

        // remove 'alt' attributes because they sometimes cause kindlegen fail, for example:
        // alt="At a glance, <i>Pokemon Uranium</i> is pretty hard to distinguish from an official Nintendo release."
        for (Element element : elements){
            element.removeAttr("alt");
        }
    }
}
